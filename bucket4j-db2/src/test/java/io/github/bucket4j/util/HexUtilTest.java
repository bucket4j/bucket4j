package io.github.bucket4j.util;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexUtilTest {

    @Test
    public void testConverter() {
        String randomString = UUID.randomUUID().toString();
        byte[] randomBytes = randomString.getBytes();
        String hexString = HexUtil.hexFromBinary(randomBytes);
        byte[] decodedBytes = HexUtil.hexToBinary(hexString);
        assertEquals(randomString, new String(decodedBytes));
    }

}
