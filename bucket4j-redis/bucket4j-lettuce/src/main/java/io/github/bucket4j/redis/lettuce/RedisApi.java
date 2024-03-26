package io.github.bucket4j.redis.lettuce;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;

public interface RedisApi<K> {

    <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params);

    RedisFuture<byte[]> get(K key);

    RedisFuture<?> delete(K key);

}
