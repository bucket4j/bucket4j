
package io.github.bucket4j.util;

import java.math.BigDecimal;

public class Formatter {

    public static BigDecimal format(double value) {
        return new BigDecimal(value).setScale(3, BigDecimal.ROUND_CEILING);
    }

}
