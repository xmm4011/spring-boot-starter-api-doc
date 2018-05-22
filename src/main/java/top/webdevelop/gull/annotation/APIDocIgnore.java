package top.webdevelop.gull.annotation;

import java.lang.annotation.*;

/**
 * Created by xumingming on 2018/5/10.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface APIDocIgnore {
    boolean value() default true;
}
