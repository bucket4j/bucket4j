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

package io.github.bucket4j.distributed.proxy.generic;

import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

public class GenericEntry implements MutableBucketEntry {

    private RemoteBucketState originalState;
    private RemoteBucketState modifiedState;

    public GenericEntry(byte[] originalStateBytes) {
        this.originalState = originalStateBytes == null? null : deserialize(originalStateBytes);
    }

    @Override
    public boolean exists() {
        return originalState != null;
    }

    @Override
    public void set(RemoteBucketState state) {
        modifiedState = state;
    }

    @Override
    public RemoteBucketState get() {
        return Objects.requireNonNull(originalState);
    }

    public RemoteBucketState getModifiedState() {
        return modifiedState;
    }

    public byte[] getModifiedStateBytes() {
        return serialize(modifiedState);
    }

    public boolean isModified() {
        return modifiedState != null;
    }

    private static byte[] serialize(RemoteBucketState object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RemoteBucketState deserialize(byte[] data) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (RemoteBucketState) ois.readObject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
