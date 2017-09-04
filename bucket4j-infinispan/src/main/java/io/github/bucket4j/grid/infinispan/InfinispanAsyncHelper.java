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

package io.github.bucket4j.grid.infinispan;

import org.infinispan.functional.EntryView.ReadWriteEntryView;
import org.infinispan.functional.FunctionalMap;
import org.infinispan.util.function.SerializableFunction;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.util.concurrent.CompletableFuture;


public class InfinispanAsyncHelper {

    public static <K, V, R> CompletableFuture<R> invokeAsync(K key, FunctionalMap.ReadWriteMap<K, V> readWriteMap, EntryProcessor<K, V, R> entryProcessor) {
        return readWriteMap.eval(key, new SerializableFunction<ReadWriteEntryView<K, V>, R>() {
            @Override
            public R apply(ReadWriteEntryView<K, V> entryView) {
                return entryProcessor.process(new MutableEntryAdapter<>(entryView));
            }
        });
    }

    private static class MutableEntryAdapter<K, V> implements MutableEntry<K, V> {

        private final ReadWriteEntryView<K, V> entryView;

        public MutableEntryAdapter(ReadWriteEntryView<K, V> entryView) {
            this.entryView = entryView;
        }

        @Override
        public K getKey() {
            return entryView.key();
        }

        @Override
        public V getValue() {
            // TODO make a copy
            return entryView.get();
        }

        @Override
        public boolean exists() {
            return entryView.find().isPresent();
        }

        @Override
        public void remove() {
            entryView.remove();
        }

        @Override
        public void setValue(V value) {
            entryView.set(value);
        }

        @Override
        public <T> T unwrap(Class<T> clazz) {
            throw new UnsupportedOperationException();
        }

    }

}
