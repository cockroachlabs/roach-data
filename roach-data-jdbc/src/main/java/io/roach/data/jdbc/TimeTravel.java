package io.roach.data.jdbc;

import java.lang.annotation.*;

/**
 * Annotation marking a transaction boundary to use follower reads (time travel).
 * See https://www.cockroachlabs.com/docs/stable/follower-reads.html
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TimeTravel {
    int value() default -1; // Non-zero denotes follower read
}
