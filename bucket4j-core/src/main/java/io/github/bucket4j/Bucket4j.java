
/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import io.github.bucket4j.local.LocalConfigurationBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * This is entry point for functionality provided bucket4j library.
 *
 * It is always better to initialize the buckets through this class.
 */
public class Bucket4j {

    private static final Map<Class, Extension> extensions;
    static {
        extensions = new HashMap<>();
        for (Extension extension : ServiceLoader.load(Extension.class)) {
            extensions.put(extension.getClass(), extension);
        }
    }

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalConfigurationBuilder}
     */
    public static LocalConfigurationBuilder builder() {
        return new LocalConfigurationBuilder();
    }

    /**
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder();
    }

    public static <T extends ConfigurationBuilder<T>, E extends Extension<T>> E extension(Class<E> extensionClass) {
        if (!Extension.class.isAssignableFrom(extensionClass)) {
            throw new IllegalArgumentException("extensionClass must inherit from io.github.bucket4j.Extension");
        }
        E extension = (E) extensions.get(extensionClass);
        if (extension == null) {
            throw new IllegalArgumentException("extension with class [" + extensionClass + "] is not registered");
        }
        return extension;
    }

}
