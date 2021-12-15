package io.github.bucket4j;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ConsumptionProbeTest {

    @Test
    public void getRemainingTokens() throws Exception {
        assertEquals(0, ConsumptionProbe.rejected(-1, 10, 100).getRemainingTokens());
        assertEquals(0, ConsumptionProbe.rejected(0, 10, 100).getRemainingTokens());
    }

    @Test
    public void testToString() throws Exception {
        System.out.println(ConsumptionProbe.consumed(1, 100));
        System.out.println(ConsumptionProbe.rejected(-1, 10, 100));
        System.out.println(ConsumptionProbe.rejected(0, 10, 100));
    }

}