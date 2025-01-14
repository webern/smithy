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

package software.amazon.smithy.model.validation;

import static software.amazon.smithy.model.validation.Severity.ERROR;
import static software.amazon.smithy.model.validation.Validator.MODEL_ERROR;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A validation event created when validating a model.
 *
 * <p>Validation events are collection while assembling and validating a model.
 * Events with a severity less than ERROR can be suppressed. All events contain
 * a message, severity, and eventId.
 */
public final class ValidationEvent implements ToNode, ToSmithyBuilder<ValidationEvent> {
    private final SourceLocation sourceLocation;
    private final String message;
    private final String eventId;
    private final Severity severity;
    private final ShapeId shapeId;
    private final String suppressionReason;
    private final String asString;

    private ValidationEvent(Builder builder) {
        if (builder.suppressionReason != null && builder.severity != Severity.SUPPRESSED) {
            throw new IllegalStateException("A suppression reason must only be provided for SUPPRESSED events");
        }

        this.sourceLocation = SmithyBuilder.requiredState("sourceLocation", builder.sourceLocation);
        this.message = SmithyBuilder.requiredState("message", builder.message);
        this.severity = SmithyBuilder.requiredState("severity", builder.severity);
        this.eventId = SmithyBuilder.requiredState("eventId", builder.eventId);
        this.shapeId = builder.shapeId;
        this.suppressionReason = builder.suppressionReason;
        this.asString = String.format("[%s] %s: %s | %s %s:%s:%s",
                severity, shapeId != null ? shapeId : "-", message, eventId,
                sourceLocation.getFilename(), sourceLocation.getLine(), sourceLocation.getColumn());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new ValidationEvent from a {@link SourceException}.
     *
     * @param exception Exception to use to create the event.
     * @return Returns a created validation event with an ID of Model.
     */
    public static ValidationEvent fromSourceException(SourceException exception) {
        return fromSourceException(exception, "");
    }

    /**
     * Creates a new ValidationEvent from a {@link SourceException}.
     *
     * @param exception Exception to use to create the event.
     * @param prefix Prefix string to add to the message.
     * @return Returns a created validation event with an ID of Model.
     */
    public static ValidationEvent fromSourceException(SourceException exception, String prefix) {
        // Get the message without source location since it's in the event.
        return ValidationEvent.builder()
                .eventId(MODEL_ERROR)
                .severity(ERROR)
                .message(prefix + exception.getMessageWithoutLocation())
                .sourceLocation(exception.getSourceLocation())
                .build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.sourceLocation = sourceLocation;
        builder.message = message;
        builder.severity = severity;
        builder.eventId = eventId;
        builder.shapeId = shapeId;
        builder.suppressionReason = suppressionReason;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ValidationEvent)) {
            return false;
        }

        ValidationEvent other = (ValidationEvent) o;
        return sourceLocation.equals(other.sourceLocation)
                && message.equals(other.message)
                && severity.equals(other.severity)
                && eventId.equals(other.eventId)
                && getShapeId().equals(other.getShapeId())
                && getSuppressionReason().equals(other.getSuppressionReason());
    }

    @Override
    public int hashCode() {
        return asString.hashCode() + getSuppressionReason().hashCode();
    }

    @Override
    public String toString() {
        return asString;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("id", Node.from(getEventId()))
                .withMember("severity", Node.from(getSeverity().toString()))
                .withOptionalMember("shapeId", getShapeId().map(Object::toString).map(Node::from))
                .withMember("message", Node.from(getMessage()))
                .withOptionalMember("suppressionReason", getSuppressionReason().map(Node::from))
                .withMember("filename", Node.from(getSourceLocation().getFilename()))
                .withMember("line", Node.from(getSourceLocation().getLine()))
                .withMember("column", Node.from(getSourceLocation().getColumn()))
                .build();
    }

    /**
     * @return The location at which the event occurred.
     */
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * @return The human-readable event message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The severity level of the event.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the identifier of the validation event.
     *
     * <p>The validation event identifier can be used to suppress events.
     *
     * @return Returns the event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return The shape ID that is associated with the event.
     */
    public Optional<ShapeId> getShapeId() {
        return Optional.ofNullable(shapeId);
    }

    /**
     * Get the reason that the event was suppressed.
     *
     * @return Returns the suppression reason if available.
     */
    public Optional<String> getSuppressionReason() {
        return Optional.ofNullable(suppressionReason);
    }

    /**
     * Builds ValidationEvent values.
     */
    public static final class Builder implements SmithyBuilder<ValidationEvent> {

        private SourceLocation sourceLocation = SourceLocation.none();
        private String message;
        private Severity severity;
        private String eventId;
        private ShapeId shapeId;
        private String suppressionReason;

        private Builder() {}

        /**
         * Sets the required message of the event.
         *
         * @param eventMessage Message to set.
         * @return Returns the builder.
         */
        public Builder message(String eventMessage) {
            message = Objects.requireNonNull(eventMessage);
            return this;
        }

        public Builder message(String eventMessage, Object... placeholders) {
            return message(String.format(eventMessage, placeholders));
        }

        /**
         * Sets the required severity of the event.
         * @param severity Event severity.
         * @return Returns the builder.
         */
        public Builder severity(Severity severity) {
            this.severity = Objects.requireNonNull(severity);
            return this;
        }

        /**
         * Sets the required event ID of the event.
         *
         * @param eventId Event ID.
         * @return Returns the builder.
         */
        public Builder eventId(final String eventId) {
            this.eventId = Objects.requireNonNull(eventId);
            return this;
        }

        /**
         * Sets the source location of where the event occurred.
         *
         * @param sourceLocation Event source location.
         * @return Returns the builder.
         */
        public Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = Objects.requireNonNull(sourceLocation.getSourceLocation());
            return this;
        }

        /**
         * Sets the shape ID related to the event.
         *
         * @param toShapeId Shape ID.
         * @param <T> Value to convert to a shape ID.
         * @return Returns the builder.
         */
        public <T extends ToShapeId> Builder shapeId(T toShapeId) {
            this.shapeId = toShapeId == null ? null : toShapeId.toShapeId();
            return this;
        }

        /**
         * Sets the shape ID and source location based on a shape.
         *
         * @param encounteredShape Shape.
         * @return Returns the builder.
         */
        public Builder shape(Shape encounteredShape) {
            return sourceLocation(Objects.requireNonNull(encounteredShape).getSourceLocation())
                    .shapeId(encounteredShape.getId());
        }

        /**
         * Sets a reason for suppressing the event.
         *
         * <p>This is only relevant if the severity is SUPPRESSED.
         *
         * @param eventSuppressionReason Event suppression reason.
         * @return Returns the builder.
         */
        public Builder suppressionReason(String eventSuppressionReason) {
            suppressionReason = eventSuppressionReason;
            return this;
        }

        @Override
        public ValidationEvent build() {
            return new ValidationEvent(this);
        }
    }
}
