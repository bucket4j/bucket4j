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

import io.github.bucket4j.local.LocalBucketBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * This is entry point for functionality provided bucket4j library.
 */
public class Bucket4j {

    private static final Map<Class, Extension> extensions;
    static {
        extensions = new HashMap<>();
        for (Extension extension : ServiceLoader.load(Extension.class)) {
            extensions.put(extension.getClass(), extension);
        }
    }

    private Bucket4j() {
        // to avoid initialization of utility class
    }

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    /**
     * Creates new instance of {@link ConfigurationBuilder}
     *
     * @return instance of {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder configurationBuilder() {
        return new ConfigurationBuilder();
    }

    /**
     * Locates Bucket4j extension by class {@code extensionClass}.
     *
     * @param extensionClass must be registered in "/META-INF/services/io.github.bucket4j.Extension" according to java SPI rules.
     * @param <T>
     * @param <E>
     *
     * @return library extension
     */
    public static <T extends AbstractBucketBuilder<T>, E extends Extension<T>> E extension(Class<E> extensionClass) {
        E extension = (E) extensions.get(extensionClass);
        if (extension == null) {
            String msg = "extension with class [" + extensionClass + "] is not registered";
            throw new IllegalArgumentException(msg);
        }
        return extension;
    }

}
