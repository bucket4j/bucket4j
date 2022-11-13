package test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LogbackTest {

    @Test
    public void loggerClassCheck() {
        Logger logger = LoggerFactory.getLogger(LogbackTest.class);
        logger.debug("fhdkjhfkdjhfkdjfhkdjfhkdjfhkdju");
        logger.info("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        logger.error("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        assertEquals(
                ch.qos.logback.classic.Logger.class.toString(),
                logger.getClass().toString());
        assertFalse(logger.isDebugEnabled());
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(LogbackTest.class);
        logger.debug("fhdkjhfkdjhfkdjfhkdjfhkdjfhkdju");
        logger.info("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        logger.error("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        System.out.println(logger.getClass());
    }

}
