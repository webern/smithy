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

package software.amazon.smithy.model.transform;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Removes all trait definitions from a model and all shapes that are only
 * connected to the graph by a removed trait definition shape.
 *
 * <p>All shapes in the prelude marked as private are automatically removed,
 * and all prelude trait definitions are removed. However, no public prelude
 * shapes are ever removed.
 *
 * <p>This can be useful when serializing a Smithy model to a format that
 * does not include trait definitions and the shapes used by trait definitions
 * would have no meaning (e.g., Swagger).
 *
 * <p>TODO: Should there be an option to only remove private traits?
 *
 * @see ModelTransformer#scrubTraitDefinitions
 */
final class ScrubTraitDefinitions {
    Model transform(ModelTransformer transformer, Model model) {
        // Find all trait definition shapes and private shapes in the prelude.
        Set<Shape> toMark = Stream.concat(
                model.getShapeIndex().shapes().filter(shape -> shape.hasTrait(TraitDefinition.class)),
                model.getShapeIndex().shapes().filter(shape -> Prelude.isPreludeShape(shape)
                                                               && shape.hasTrait(PrivateTrait.class))
        ).collect(Collectors.toSet());

        MarkAndSweep markAndSweep = new MarkAndSweep(
                // Mark shapes for removal that are private or trait definitions.
                context -> {
                    toMark.forEach(context::markShape);
                    toMark.clear();
                },
                // Don't remove public prelude shapes.
                ScrubTraitDefinitions::notPublicPreludeShape);

        // Removing shapes that are traits automatically removes applications of that trait from other shapes.
        return transformer.removeShapes(model, markAndSweep.markAndSweep(model));
    }

    private static boolean notPublicPreludeShape(Shape shape) {
        return !(Prelude.isPublicPreludeShape(shape.getId()) && !shape.hasTrait(TraitDefinition.class));
    }
}
