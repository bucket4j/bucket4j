/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j

import io.github.bucket4j.grid.GridBucket
import io.github.bucket4j.mock.GridProxyMock
import io.github.bucket4j.mock.SchedulerMock
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.grid.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class AsyncBlockingBucketSpecification extends Specification {

	def "should complete future exceptionally if scheduler failed to schedule the task"() {
		setup:
			BucketConfiguration configuration = Bucket4j.configurationBuilder()
					.addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
					.build()
			GridProxyMock mockProxy = new GridProxyMock(SYSTEM_MILLISECONDS)
			SchedulerMock schedulerMock = new SchedulerMock()
			Bucket bucket = GridBucket.createInitializedBucket(BucketListener.NOPE, "66", configuration, mockProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION)
		when:
			schedulerMock.setException(new RuntimeException())
			CompletableFuture<Boolean> future = bucket.asAsyncScheduler().tryConsume(10, Duration.ofNanos(100000), schedulerMock)
		then:
			future.isCompletedExceptionally()
	}

}
