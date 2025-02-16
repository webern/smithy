/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.validation.testrunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.IoUtils;

/**
 * Runs a single test case by loading a model and ensuring the resulting
 * events match the validation events stored in a newline separated file.
 */
public final class SmithyTestCase {
    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "^\\[(?<severity>SUPPRESSED|NOTE|WARNING|DANGER|ERROR)] (?<shape>[^ ]+): ?(?<message>.*) \\| (?<id>[^)]+)");

    private List<ValidationEvent> expectedEvents;
    private String modelLocation;

    /**
     * @param modelLocation Location of where the model is stored.
     * @param expectedEvents The expected validation events to encounter.
     */
    public SmithyTestCase(String modelLocation, List<ValidationEvent> expectedEvents) {
        this.modelLocation = Objects.requireNonNull(modelLocation);
        this.expectedEvents = Collections.unmodifiableList(expectedEvents);
    }

    /**
     * Creates a test case from a model file.
     *
     * <p>The error file is expected to be stored in the same directory
     * as the model file and is assumed to be named the same as the
     * file with the file extension replaced with ".errors".
     *
     * <p>The accompanying error file is a newline separated list of error
     * strings, where each error is defined in the following format:
     * {@code [SEVERITY] shapeId message | EventId filename:line:column}.
     * A shapeId of "-" means that a specific shape is not targeted.
     *
     * @param modelLocation File location of the model.
     * @return Returns the created test case.
     * @throws IllegalArgumentException if the file does not contain an extension.
     */
    public static SmithyTestCase fromModelFile(String modelLocation) {
        String errorFileLocation = inferErrorFileLocation(modelLocation);
        List<ValidationEvent> expectedEvents = loadExpectedEvents(errorFileLocation);
        return new SmithyTestCase(modelLocation, expectedEvents);
    }

    /**
     * Gets the expected validation events.
     *
     * @return Expected validation events.
     */
    public List<ValidationEvent> getExpectedEvents() {
        return expectedEvents;
    }

    /**
     * Gets the location of the model file.
     *
     * @return Model location.
     */
    public String getModelLocation() {
        return modelLocation;
    }

    /**
     * Creates a test case result from a test case and validated model.
     *
     * <p>The validation events encountered while validating a model are
     * compared against the expected validation events. An actual event (A) is
     * considered a match with an expected event (E) if A and E target the
     * same shape, use the same validation event ID, have the same severity,
     * and the message of E starts with the message of A.
     *
     * @param validatedResult Result of creating and validating the model.
     * @return Returns the created test case result.
     */
    public Result createResult(ValidatedResult<Model> validatedResult) {
        List<ValidationEvent> actualEvents = validatedResult.getValidationEvents();
        List<ValidationEvent> unmatchedEvents = getExpectedEvents().stream()
                .filter(expectedEvent -> actualEvents.stream()
                        .noneMatch(actualEvent -> compareEvents(expectedEvent, actualEvent)))
                .collect(Collectors.toList());

        List<ValidationEvent> extraEvents = actualEvents.stream()
                .filter(actualEvent -> getExpectedEvents().stream()
                        .noneMatch(expectedEvent -> compareEvents(expectedEvent, actualEvent)))
                .collect(Collectors.toList());

        return new SmithyTestCase.Result(getModelLocation(), unmatchedEvents, extraEvents);
    }

    private static boolean compareEvents(ValidationEvent expected, ValidationEvent actual) {
        return expected.getSeverity() == actual.getSeverity()
               && expected.getEventId().equals(actual.getEventId())
               && expected.getShapeId().equals(actual.getShapeId())
               // Normalize new lines.
               && actual.getMessage().replace("\n", "\\n").startsWith(expected.getMessage().replace("\n", "\\n"));
    }

    private static String inferErrorFileLocation(String modelLocation) {
        int extensionPosition = modelLocation.lastIndexOf(".");
        if (extensionPosition == -1) {
            throw new IllegalArgumentException("Invalid Smithy model file: " + modelLocation);
        }
        return modelLocation.substring(0, extensionPosition) + ".errors";
    }

    private static List<ValidationEvent> loadExpectedEvents(String errorsFileLocation) {
        String contents = IoUtils.readUtf8File(errorsFileLocation);
        return Arrays.stream(contents.split("\n"))
                .filter(line -> !line.trim().isEmpty())
                .map(SmithyTestCase::parseValidationEvent)
                .collect(Collectors.toList());
    }

    static ValidationEvent parseValidationEvent(String event) {
        Matcher matcher = EVENT_PATTERN.matcher(event);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid validation event: " + event);
        }

        // Construct a dummy source location since we don't validate it.
        SourceLocation location = new SourceLocation("/", 0, 0);

        ValidationEvent.Builder builder = ValidationEvent.builder()
                .severity(Severity.fromString(matcher.group("severity")).get())
                .sourceLocation(location)
                .eventId(matcher.group("id"))
                .message(matcher.group("message"));

        // A shape ID of "-" means no shape.
        if (!matcher.group("shape").equals("-")) {
            builder.shapeId(ShapeId.from(matcher.group("shape")));
        }

        return builder.build();
    }

    /**
     * Output of validating a model against a test case.
     */
    public static final class Result {
        private final String modelLocation;
        private final Collection<ValidationEvent> unmatchedEvents;
        private final Collection<ValidationEvent> extraEvents;

        Result(
                String modelLocation,
                Collection<ValidationEvent> unmatchedEvents,
                Collection<ValidationEvent> extraEvents
        ) {
            this.modelLocation = modelLocation;
            this.unmatchedEvents = Collections.unmodifiableCollection(unmatchedEvents);
            this.extraEvents = Collections.unmodifiableCollection(extraEvents);
        }

        /**
         * Checks if the result does not match expected results.
         *
         * @return True if there are extra or unmatched events.
         */
        public boolean isInvalid() {
            return !unmatchedEvents.isEmpty() || !extraEvents.isEmpty();
        }

        /**
         * @return Returns a description of where the model was stored.
         */
        public String getModelLocation() {
            return modelLocation;
        }

        /**
         * @return Returns the events that were expected but not encountered.
         */
        public Collection<ValidationEvent> getUnmatchedEvents() {
            return unmatchedEvents;
        }

        /**
         * @return Returns the events that were encountered but not expected.
         */
        public Collection<ValidationEvent> getExtraEvents() {
            return extraEvents;
        }
    }
}
