/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.proxy;

/**
 * Interface used by particular {@link ProxyManager} implementations to indicate
 * that they support the manual(triggered by user) removing of expired bucket entries.
 *
 * <p>
 * Typically, this interface is implemented by RDBMS backed proxy-managers where underlying DBMS is not support automatically expiration.
 * And wise versa, this interface never implemented by Redis/JCache integrations,
 * because for such technologies expiration is supported out-of-the box by underlying technology.
 */
public interface ExpiredEntriesCleaner {

    /**
     * Tries to remove expired bucket entries and put all removed keys to the returned collection.
     * It is guaranteed that on call of {@code removeExpired} does not remove more than {@code batchSize} buckets at once,
     * it also means that returned result always less than or equal to {@code batchSize},
     * so caller needs to analyze the size of returned collection and call {@code removeExpired} again if needed.
     *
     * <p>Example of usage:
     * <pre>
     * {@code
     *    private static final int MAX_TO_REMOVE_IN_ONE_TRANSACTION = 1_000;
     *    private static final int THRESHOLD_TO_CONTINUE_REMOVING = 50;
     *
     *    // once per day at 4:30 morning
     *    @Scheduled(cron = "0 30 4 * * *")
     *    public void scheduleFixedDelayTask() {
     *       int removedKeysCount;
     *       do {
     *            removedCount = proxyManager.removeExpired(MAX_TO_REMOVE_IN_ONE_TRANSACTION);
     *            if (removedKeysCount > 0) {
     *                logger.info("Removed {} expired buckets", removedCount);
     *            } else {
     *                logger.info("There are no expired buckets to remove");
     *            }
     *       } while (removedCount > THRESHOLD_TO_CONTINUE_REMOVING)
     *    }
     * }
     * </pre>
     *
     * @param batchSize specifies how many expired buckets can be deleted in single transaction.
     *
     * @return count of removed keys
     */
    int removeExpired(int batchSize);

}
