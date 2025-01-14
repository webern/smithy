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

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;

public class TopDownIndexTest {
    @Test
    public void findDirectChildren() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Service")
                .version("1")
                .addResource("ns.foo#Resource")
                .build();
        ResourceShape resource = ResourceShape.builder().id("ns.foo#Resource").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(service, resource).build();
        Model model = Model.builder().shapeIndex(index).build();
        TopDownIndex childIndex = model.getKnowledge(TopDownIndex.class);

        assertThat(childIndex.getContainedOperations(service), empty());
        assertThat(childIndex.getContainedResources(service), contains(resource));
        assertThat(childIndex.getContainedOperations(resource), empty());
        assertThat(childIndex.getContainedResources(resource), empty());
    }

    @Test
    public void findsAllChildren() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Service")
                .version("1")
                .addResource("ns.foo#A")
                .build();
        ResourceShape resourceA = ResourceShape.builder()
                .id("ns.foo#A")
                .list(ShapeId.from("ns.foo#List"))
                .addResource("ns.foo#B")
                .build();

        ResourceShape resourceB = ResourceShape.builder().id("ns.foo#B").addOperation("ns.foo#Operation").build();
        OperationShape operation = OperationShape.builder().id("ns.foo#Operation").build();
        OperationShape list = OperationShape.builder().id("ns.foo#List").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(service, resourceA, resourceB, operation, list).build();
        Model model = Model.builder().shapeIndex(index).build();
        TopDownIndex childIndex = model.getKnowledge(TopDownIndex.class);

        assertThat(childIndex.getContainedResources(service.getId()), containsInAnyOrder(resourceA, resourceB));
        assertThat(childIndex.getContainedOperations(service.getId()), containsInAnyOrder(operation, list));

        assertThat(childIndex.getContainedResources(resourceA.getId()), contains(resourceB));
        assertThat(childIndex.getContainedOperations(resourceA.getId()), containsInAnyOrder(operation, list));

        assertThat(childIndex.getContainedResources(resourceB.getId()), empty());
        assertThat(childIndex.getContainedOperations(resourceB.getId()), contains(operation));

        assertThat(childIndex.getContainedOperations(operation.getId()), empty());
        assertThat(childIndex.getContainedResources(operation.getId()), empty());

        assertThat(childIndex.getContainedResources(ShapeId.from("ns.foo#NotThere")), empty());
    }
}
