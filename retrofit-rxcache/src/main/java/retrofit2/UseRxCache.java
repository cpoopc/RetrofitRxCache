package retrofit2;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author cpoopc
 * @date 2016/1/16
 * @time 20:54
 * @description
 */

@Target(METHOD)
@Retention(RUNTIME)
public @interface UseRxCache {
}
