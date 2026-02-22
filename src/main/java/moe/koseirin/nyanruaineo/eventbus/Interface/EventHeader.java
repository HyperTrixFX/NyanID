package moe.koseirin.nyanruaineo.eventbus.Interface;

/*
 * @author KoseiRin_
 * awa
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHeader {
    Priority priority() default Priority.NORMAL;
}
