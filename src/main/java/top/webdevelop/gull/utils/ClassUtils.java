package top.webdevelop.gull.utils;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by xumingming on 2018/3/25.
 */
public class ClassUtils {
    public static boolean isBeanType(Class classz) {
        return !isSingleFieldType(classz) && !isListType(classz);
    }

    public static boolean isMapType(Class classz) {
        return classz.equals(Map.class);
    }

    public static boolean isObjectType(Class classz) {
        return classz.equals(Object.class);
    }

    public static boolean isListType(Class classz) {
        return Collection.class.isAssignableFrom(classz) || classz.isArray();
    }

    public static boolean isSingleFieldType(Class classz) {
        return isPrimitive(classz) || classz.equals(MultipartFile.class);
    }

    public static boolean isPrimitive(Class classz) {
        return classz.isPrimitive() || isPrimitiveWrapClass(classz) || classz.equals(String.class)
                || classz.equals(BigDecimal.class) || classz.equals(Enum.class) || Enum.class.equals(classz.getSuperclass())
                || classz.equals(LocalDate.class) || classz.equals(LocalDateTime.class) || classz.equals(Date.class);
    }

    public static boolean isPrimitiveWrapClass(Class classz) {
        try {
            return ((Class) classz.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }


}
