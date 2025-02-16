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

import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.utils.MapUtils;

class TypedPropertiesBag {

    private final Map<String, Object> properties;

    TypedPropertiesBag(Map<String, Object> properties) {
        this.properties = MapUtils.copyOf(properties);
    }

    /**
     * Gets the additional properties of the object.
     *
     * @return Returns a map of additional property strings.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Gets a specific property if present.
     *
     * @param name Property to retrieve.
     * @return Returns the optionally found property.
     */
    public Optional<Object> getProperty(String name) {
        return Optional.ofNullable(properties.get(name));
    }

    /**
     * Gets an additional property of a specific type.
     *
     * @param name Name of the property to get.
     * @param type Type of value to expect.
     * @param <T> Type of value to expect.
     * @return Returns a map of additional property strings.
     * @throws IllegalArgumentException if the value is not of the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String name, Class<T> type) {
        return getProperty(name)
                .map(value -> {
                    if (!type.isInstance(value)) {
                        throw new IllegalArgumentException(String.format(
                                "%s property `%s` of `%s` is not an instance of `%s`. Found `%s`",
                                getClass().getSimpleName(), name, this, type.getName(), value.getClass().getName()));
                    }
                    return (T) value;
                });
    }

    /**
     * Gets a specific additional property or throws if missing.
     *
     * @param name Property to retrieve.
     * @return Returns the found property.
     * @throws IllegalArgumentException if the property is not present.
     */
    public Object expectProperty(String name) {
        return getProperty(name).orElseThrow(() -> new IllegalArgumentException(String.format(
                "Property `%s` is not part of %s, `%s`", name, getClass().getSimpleName(), this)));
    }

    /**
     * Gets a specific additional property or throws if missing or if the
     * property is not an instance of the given type.
     *
     * @param name Property to retrieve.
     * @param type Type of value to expect.
     * @param <T> Type of value to expect.
     * @return Returns the found property.
     * @throws IllegalArgumentException if the property is not present.
     * @throws IllegalArgumentException if the value is not of the given type.
     */
    public <T> T expectProperty(String name, Class<T> type) {
        return getProperty(name, type).orElseThrow(() -> new IllegalArgumentException(String.format(
                "Property `%s` is not part of %s, `%s`", name, getClass().getSimpleName(), this)));
    }
}
