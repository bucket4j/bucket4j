/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite.thin.compute;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.resources.IgniteInstanceResource;

public class Bucket4jComputeTask <K> extends ComputeTaskAdapter<Bucket4jComputeTaskParams<K>, byte[]> {

    public static final String JOB_NAME = Bucket4jComputeTask.class.getName();

    @IgniteInstanceResource
    private Ignite ignite;

    @Override
    public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, Bucket4jComputeTaskParams<K> params) throws IgniteException {
        Bucket4jComputeJob<K> job = new Bucket4jComputeJob<>(params);

        ClusterNode primaryNodeForKey = ignite.affinity(params.getCacheName()).mapKeyToNode(params.getKey());
        for (ClusterNode clusterNode : subgrid) {
            if (clusterNode == primaryNodeForKey) {
                return Collections.singletonMap(job, clusterNode);
            }
        }

        // should never come here, but if it happens let's execute Job on random node
        int randomNode = ThreadLocalRandom.current().nextInt(subgrid.size());
        return Collections.singletonMap(job, subgrid.get(randomNode));
    }

    @Override
    public byte[] reduce(List<ComputeJobResult> results) throws IgniteException {
        return results.get(0).getData();
    }

}
