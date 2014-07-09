package services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(
        {
                ElementType.METHOD, ElementType.TYPE
        })
public @interface RetryOnFailure
{
    int attempts() default 3;

    int delay() default 1000; //default 1 second

    Class exception() default Exception.class;
}
