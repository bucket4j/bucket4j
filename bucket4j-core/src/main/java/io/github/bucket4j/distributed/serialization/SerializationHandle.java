/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.distributed.versioning.Version;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SerializationHandle<T> {

    <I> T deserialize(DeserializationAdapter<I> adapter, I input, Version backwardCompatibilityVersion) throws IOException;

    <O> void serialize(SerializationAdapter<O> adapter, O output, T serializableObject, Version backwardCompatibilityVersion) throws IOException;

    /**
     * @return the type identifier that is unique across all Bucket4j classes
     */
    int getTypeId();

    Class<T> getSerializedType();

    Version getEffectiveVersion(T serializableObject);

    T fromJsonCompatibleSnapshot(Map<String, Object> snapshot, Version backwardCompatibilityVersion) throws IOException;

    Map<String, Object> toJsonCompatibleSnapshot(T serializableObject, Version backwardCompatibilityVersion) throws IOException;

    /**
     * @return the type identifier that is unique across all Bucket4j classes
     */
    String getTypeName();


    default double[] readDoubleArray(Map<String, Object> snapshot, String fieldName) {
        Object object = snapshot.get(fieldName);
        if (object instanceof double[]) {
            return (double[]) object;
        } else if (object instanceof Number[]) {
            Number[] array = (Number[]) object;
            double[] result = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i].doubleValue();
            }
            return result;
        } else {
            List<Number> list = (List<Number>) object;
            double[] result = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i).doubleValue();
            }
            return result;
        }
    }

    default long[] readLongArray(Map<String, Object> snapshot, String fieldName) {
        Object object = snapshot.get(fieldName);
        if (object instanceof long[]) {
            return (long[]) object;
        } else if (object instanceof Number[]) {
            Number[] array = (Number[]) object;
            long[] result = new long[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i].longValue();
            }
            return result;
        } else {
            List<Number> list = (List<Number>) object;
            long[] result = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i).longValue();
            }
            return result;
        }
    }

    default Long readLongValue(Map<String, Object> snapshot, String fieldName) {
        Object object = snapshot.get(fieldName);
        if (object instanceof Long) {
            return (Long) object;
        } else {
            Number number = (Number) object;
            return number.longValue();
        }
    }

    default Long readOptionalLongValue(Map<String, Object> snapshot, String fieldName) {
        Object object = snapshot.get(fieldName);
        if (object == null) {
            return null;
        }
        if (object instanceof Long) {
            return (Long) object;
        } else {
            Number number = (Number) object;
            return number.longValue();
        }
    }

    default Integer readIntegerValue(Map<String, Object> snapshot, String fieldName) {
        Object object = snapshot.get(fieldName);
        if (object instanceof Integer) {
            return (Integer) object;
        } else {
            Number number = (Number) object;
            return number.intValue();
        }
    }

    default int readIntValue(Map<String, Object> snapshot, String fieldName) {
        Object object = snapshot.get(fieldName);
        if (object instanceof Integer) {
            return (Integer) object;
        } else {
            Number number = (Number) object;
            return number.intValue();
        }
    }

}
