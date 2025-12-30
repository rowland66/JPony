package org.rowland.jpony.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Documented
@Target(TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface JPActor {
    /**
     * {@return the interfaces implement by this actor that should be exposed as the actors behavior}
     */
    Class<?>[] behaviors();

    Class<?> factory();
}
