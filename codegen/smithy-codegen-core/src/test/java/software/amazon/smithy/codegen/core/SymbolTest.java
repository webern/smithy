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

package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SymbolTest {
    @Test
    public void relativizesSymbol() {
        String ns = "com.foo";
        Symbol symbol = Symbol.builder()
                .name("Baz")
                .namespace(ns, "::")
                .build();

        assertThat(symbol.relativize(ns), equalTo("Baz"));
        assertThat(symbol.relativize("com.bam"), equalTo("com.foo::Baz"));
    }

    @Test
    public void getsTypedProperties() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .putProperty("baz", "bar")
                .putProperty("bam", 100)
                .build();

        assertThat(symbol.expectProperty("baz", String.class), equalTo("bar"));
        assertThat(symbol.expectProperty("bam", Integer.class), equalTo(100));
    }

    @Test
    public void throwsIfExpectedPropertyIsNotPresent() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Symbol symbol = Symbol.builder().name("foo").build();

            symbol.expectProperty("baz");
        });
    }

    @Test
    public void throwsIfExpectedPropertyIsNotOfSameType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Symbol symbol = Symbol.builder()
                    .name("foo")
                    .putProperty("bam", 100)
                    .build();

            symbol.expectProperty("bam", String.class);
        });
    }

    @Test
    public void returnsDefinitionIfDeclarationPresent() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .declarationFile("/foo/bar.baz")
                .build();

        assertThat(symbol.getDefinitionFile(), equalTo("/foo/bar.baz"));
    }

    @Test
    public void returnsDeclarationIfDefinitionPresent() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .declarationFile("/foo/bar.baz")
                .build();

        assertThat(symbol.getDeclarationFile(), equalTo("/foo/bar.baz"));
    }

    @Test
    public void returnsAppropriateDefinitionAndDeclarationFiles() {
        Symbol symbol = Symbol.builder()
                .name("foo")
                .definitionFile("/foo/bar.baz")
                .declarationFile("/foo/bar.h")
                .build();

        assertThat(symbol.getDefinitionFile(), equalTo("/foo/bar.baz"));
        assertThat(symbol.getDeclarationFile(), equalTo("/foo/bar.h"));
    }
}
