
package io.github.bucket4j.util;


public interface ComparableByContent<T extends ComparableByContent> {

    boolean equalsByContent(T other);

    public static <T> boolean equals(T object1, T object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null && object2 != null) || (object1 != null && object2 == null)) {
            return false;
        }

        if (object1 instanceof ComparableByContent) {
            return ((ComparableByContent) object1).equalsByContent((ComparableByContent) object2);
        } else {
            return object1.equals(object2);
        }
    }

}
