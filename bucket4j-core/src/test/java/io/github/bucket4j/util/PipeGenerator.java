package io.github.bucket4j.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PipeGenerator {

    public static List<List<?>> сartesianProduct(List<?> firstList, List<?> secondList) {
        List<List<?>> result = new ArrayList<>();
        for (Object first : firstList) {
            for (Object second : secondList) {
                List<?> row = Arrays.asList(first, second);
                result.add(row);
            }
        }
        return result;
    }

    public static List<List<?>> сartesianProduct(List<?> firstList, List<?> secondList, List<?> thirdList) {
        List<List<?>> result = new ArrayList<>();
        for (Object first : firstList) {
            for (Object second : secondList) {
                for (Object third : thirdList) {
                    List<?> row = Arrays.asList(first, second, third);
                    result.add(row);
                }
            }
        }
        return result;
    }

}
