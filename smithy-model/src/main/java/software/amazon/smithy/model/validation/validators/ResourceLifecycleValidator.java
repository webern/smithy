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

package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that resource are applied appropriately to resources.
 */
public final class ResourceLifecycleValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        return model.getShapeIndex().shapes(ResourceShape.class)
                .flatMap(shape -> validateResource(index, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateResource(ShapeIndex index, ResourceShape resource) {
        List<ValidationEvent> events = new ArrayList<>();

        // Note: Whether or not these use a valid bindings is validated in ResourceIdentifierBindingValidator.
        resource.getPut().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "put", false).ifPresent(events::add);
            validateIdempotent(resource, operation, "put", "").ifPresent(events::add);
        });

        resource.getCreate().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "create", false).ifPresent(events::add);
        });

        resource.getRead().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "read", true).ifPresent(events::add);
        });

        resource.getUpdate().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "update", false).ifPresent(events::add);
        });

        resource.getDelete().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "delete", false).ifPresent(events::add);
            validateIdempotent(resource, operation, "delete", "").ifPresent(events::add);
        });

        resource.getList().flatMap(index::getShape).flatMap(Shape::asOperationShape).ifPresent(operation -> {
            validateReadonly(resource, operation, "list", true).ifPresent(events::add);
        });

        return events;
    }

    private Optional<ValidationEvent> validateReadonly(
            ResourceShape resource,
            OperationShape operation,
            String lifecycle,
            boolean requireReadOnly
    ) {
        if (requireReadOnly == operation.hasTrait(ReadonlyTrait.class)) {
            return Optional.empty();
        }

        return Optional.of(error(resource, format(
                "The `%s` lifecycle operation of this resource targets an invalid operation, `%s`. The targeted "
                + "operation %s be marked with the readonly trait.",
                lifecycle, operation.getId(), requireReadOnly ? "must" : "must not")));
    }

    private Optional<ValidationEvent> validateIdempotent(
            ResourceShape resource,
            OperationShape operation,
            String lifecycle,
            String additionalMessage
    ) {
        if (operation.hasTrait(IdempotentTrait.class)) {
            return Optional.empty();
        }

        return Optional.of(error(resource, format(
                "The `%s` lifecycle operation of this resource targets an invalid operation, `%s`. The targeted "
                + "operation must be marked as idempotent.%s",
                lifecycle, operation.getId(), additionalMessage)));
    }
}
