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

package software.amazon.smithy.aws.traits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class DataTraitTest {

    private Model getModel() {
        return Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("data-classification-model.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void loadsWithString() {
        Model model = getModel();
        assertTrue(model.getShapeIndex().getShape(ShapeId.from("ns.foo#A"))
                .flatMap(shape -> shape.getTrait(DataTrait.class))
                .filter(trait -> trait.getValue().equals("account"))
                .isPresent());

        assertTrue(model.getShapeIndex().getShape(ShapeId.from("ns.foo#B"))
                .flatMap(shape -> shape.getTrait(DataTrait.class))
                .filter(trait -> trait.getValue().equals("tagging"))
                .isPresent());
    }
}
