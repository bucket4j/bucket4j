package io.github.bucket4j.redis.jedis;

public interface RedisApi {

    Object eval(final byte[] script, final int keyCount, final byte[]... params);

    byte[] get(byte[] key);

    void delete(byte[] key);

}
