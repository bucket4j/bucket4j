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

package regression;

import com.github.bucket4j.AbstractBucketBuilder;
import com.github.bucket4j.Bandwidth;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.Bucket4j;
import com.github.bucket4j.mock.BucketType;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.fail;

/**
 * https://github.com/vladimir-bukhtoyarov/bucket4j/issues/20
 */
public class ConsumeShouldDetectThatTokensGreaterThanCapacityTest {

    @Test
    public void testThatConsumeDetectThatTokensGreaterThanCapacity() throws InterruptedException {
        AbstractBucketBuilder builder = Bucket4j.builder().addLimit(Bandwidth.simple(1, Duration.ofSeconds(1)));
        for (BucketType type : BucketType.values()) {
            Bucket bucket = type.createBucket(builder);

            try {
                bucket.consume(5);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }

            try {
                bucket.consume(5, 10000);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

}
