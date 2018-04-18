package top.webdevelop.gull.apidoc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import top.webdevelop.gull.utils.ClassUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by xumingming on 2018/3/24.
 */
public enum APIDocFieldType {
    string, file, bool, integer, decimal, object, list;

    private static Logger logger = LoggerFactory.getLogger(APIDocFieldType.class);

    public static APIDocFieldType parse(Class classz) {
        if (ClassUtils.isSingleFieldType(classz)) {
            if (classz.equals(String.class) || classz.equals(Character.class) || classz.equals(char.class)
                    || classz.equals(LocalDate.class) || classz.equals(LocalDateTime.class) || classz.equals(Date.class)
                    || classz.equals(Enum.class) || Enum.class.equals(classz.getSuperclass())) {
                return APIDocFieldType.string;
            }
            if (classz.equals(MultipartFile.class)) {
                return APIDocFieldType.file;
            }
            if (classz.equals(Boolean.class) || classz.equals(boolean.class)) {
                return APIDocFieldType.bool;
            }
            if (classz.equals(Short.class) || classz.equals(short.class)
                    || classz.equals(Integer.class) || classz.equals(int.class)
                    || classz.equals(Long.class) || classz.equals(long.class)
                    || classz.equals(Byte.class) || classz.equals(byte.class)) {
                return APIDocFieldType.integer;
            }
            if (classz.equals(BigDecimal.class)
                    || classz.equals(Float.class) || classz.equals(float.class)
                    || classz.equals(Double.class) || classz.equals(double.class)) {
                return APIDocFieldType.decimal;
            }
        } else if (ClassUtils.isBeanType(classz)) {
            return APIDocFieldType.object;
        } else if (ClassUtils.isListType(classz)) {
            return APIDocFieldType.list;
        }

        logger.warn("not supported type, class: {}", classz);
        return null;
    }
}
