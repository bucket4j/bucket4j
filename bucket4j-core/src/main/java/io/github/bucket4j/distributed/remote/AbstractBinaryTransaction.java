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
package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.versioning.*;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.*;

public abstract class AbstractBinaryTransaction {

    private final byte[] requestBytes;
    private Version backwardCompatibilityVersion;

    protected AbstractBinaryTransaction(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    public byte[] execute() {
        Request<?> request;
        try {
            request = InternalSerializationHelper.deserializeRequest(requestBytes);
        } catch (UnsupportedTypeException e) {
            return serializeResult(CommandResult.unsupportedType(e.getTypeId()), Versions.getOldest());
        } catch (UsageOfUnsupportedApiException e) {
            return serializeResult(CommandResult.usageOfUnsupportedApiException(e.getRequestedFormatNumber(), e.getMaxSupportedFormatNumber()), Versions.getOldest());
        } catch (UsageOfObsoleteApiException e) {
            return serializeResult(CommandResult.usageOfObsoleteApiException(e.getRequestedFormatNumber(), e.getMinSupportedFormatNumber()), Versions.getOldest());
        }

        backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();

        try {
            RemoteBucketState currentState = null;
            if (exists()) {
                byte[] stateBytes = getRawState();
                currentState = deserializeState(stateBytes);
            }
            MutableBucketEntry entryWrapper = new MutableBucketEntry(currentState);

            long time = request.getClientSideTime() != null? request.getClientSideTime(): System.currentTimeMillis() * 1_000_000;
            RemoteCommand<?> command = request.getCommand();
            CommandResult<?> result = command.execute(entryWrapper, time);

            if (entryWrapper.isStateModified()) {
                RemoteBucketState newState = entryWrapper.get();
                setRawState(serializeState(newState, backwardCompatibilityVersion), newState);
            }

            return serializeResult(result, request.getBackwardCompatibilityVersion());
        } catch (UnsupportedTypeException e) {
            return serializeResult(CommandResult.unsupportedType(e.getTypeId()), backwardCompatibilityVersion);
        } catch (UsageOfUnsupportedApiException e) {
            return serializeResult(CommandResult.usageOfUnsupportedApiException(e.getRequestedFormatNumber(), e.getMaxSupportedFormatNumber()), backwardCompatibilityVersion);
        } catch (UsageOfObsoleteApiException e) {
            return serializeResult(CommandResult.usageOfObsoleteApiException(e.getRequestedFormatNumber(), e.getMinSupportedFormatNumber()), backwardCompatibilityVersion);
        }
    }

    protected abstract byte[] getRawState();

    protected abstract void setRawState(byte[] newStateBytes, RemoteBucketState newState);

    public abstract boolean exists();


}
