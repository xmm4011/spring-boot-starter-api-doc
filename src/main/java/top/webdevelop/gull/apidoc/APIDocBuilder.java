package top.webdevelop.gull.apidoc;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import top.webdevelop.gull.utils.ClassUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xumingming on 2018/3/24.
 */
public class APIDocBuilder {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private RequestMappingInfo requestMappingInfo;
    private Method method;

    public static APIDocBuilder newInstance() {
        return new APIDocBuilder();
    }

    public APIDocBuilder setRequestMappingInfo(RequestMappingInfo requestMappingInfo) {
        this.requestMappingInfo = requestMappingInfo;
        return this;
    }

    public APIDocBuilder setMethod(Method method) {
        this.method = method;
        return this;
    }

    public APIDoc build() {
        return new APIDoc(parseAPIDocUrl(), parseAPIDocAction(), parseAPIDocRequest(), parseAPIDocResponse());
    }

    private String parseAPIDocUrl() {
        return removeBrackets(this.requestMappingInfo.getPatternsCondition().toString());
    }

    private String parseAPIDocAction() {
        return removeBrackets(this.requestMappingInfo.getMethodsCondition().toString());
    }

    private List<APIDoc.Field> parseAPIDocRequest() {
        List<MethodParameter> parameters = new ArrayList<>();
        for (int i = 0; i < this.method.getParameterAnnotations().length; i++) {
            parameters.add(new MethodParameter(this.method, i));
        }

        return parameters.stream()
                .filter(this::nonIgnoreRequestParam)
                .map(parameter -> this.parseAPIDocFieldByClass(parameter.getParameterType(), parameter))
                .reduce(new ArrayList<>(), (left, right) -> {
                    left.addAll(right);
                    return left;
                });
    }

    private boolean nonIgnoreRequestParam(MethodParameter parameter) {
        return parameter.getParameterAnnotation(PathVariable.class) == null && !parameter.getParameterType().equals(HttpServletRequest.class) && !parameter.getParameterType().equals(HttpServletResponse.class);
    }

    private List<APIDoc.Field> parseAPIDocResponse() {
        return parseAPIDocFieldByClass(this.method.getReturnType());
    }

    private List<APIDoc.Field> parseAPIDocFieldByClass(Class<?> classz) {
        return parseAPIDocFieldByClass(classz, null);
    }

    private List<APIDoc.Field> parseAPIDocFieldByClass(Class<?> classz, MethodParameter parameter) {
        List<APIDoc.Field> fields = new ArrayList<>();
        if (ClassUtils.isSingleFieldType(classz)) {
            if (parameter != null) {
                RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
                if (requestParam != null) {
                    fields.add(new APIDoc.Field(requestParam.value(), APIDocFieldType.parse(classz), requestParam.required()));
                } else {
                    logger.warn("ignore parameter: {}", parameter);
                }
            } else {
                logger.warn("response primitive type, for: {}", classz);
                fields.add(new APIDoc.Field(null, APIDocFieldType.parse(classz)));
            }
        } else if (ClassUtils.isListType(classz)) {
            APIDoc.Field field = new APIDoc.Field(null, APIDocFieldType.parse(classz));
            try {
                Class<?> aClass;
                if (classz.isArray()) {
                    aClass = classz.getComponentType();
                } else {
                    aClass = Class.forName(((ParameterizedType) classz.getGenericSuperclass()).getActualTypeArguments()[0].getTypeName());
                }
                field.setChilds(parseAPIDocFieldByBean(aClass));
            } catch (ClassNotFoundException ignored) {
            }
            fields.add(field);
        } else {
            fields.addAll(parseAPIDocFieldByBean(classz));
        }

        return fields;
    }

    private List<APIDoc.Field> parseAPIDocFieldByBean(Class<?> classz) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(classz, Object.class).getPropertyDescriptors())
                    .filter(i -> i.getReadMethod() != null)
                    .map(i -> this.parseAPIDocFieldByPropertyDescriptor(classz, i))
                    .collect(Collectors.toList());
        } catch (IntrospectionException e) {
            return new ArrayList<>();
        }
    }

    private APIDoc.Field parseAPIDocFieldByPropertyDescriptor(Class<?> classz, PropertyDescriptor propertyDescriptor) {
        APIDoc.Field field = new APIDoc.Field(propertyDescriptor.getName(),
                APIDocFieldType.parse(propertyDescriptor.getPropertyType()),
                hasRequiredAnnotation(classz, propertyDescriptor));

        if (field.getType().isObject()) {
            field.setChilds(this.parseAPIDocFieldByBean(propertyDescriptor.getPropertyType()));
        }
        if (field.getType().isList()) {
            try {
                Class<?> aClass;
                if (propertyDescriptor.getPropertyType().isArray()) {
                    aClass = propertyDescriptor.getPropertyType().getComponentType();
                } else {
                    aClass = Class.forName(((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0].getTypeName());
                }
                field.setChilds(this.parseAPIDocFieldByBean(aClass));
            } catch (ClassNotFoundException ignored) {
            }
        }

        return field;

    }

    private boolean hasRequiredAnnotation(Class<?> classz, PropertyDescriptor propertyDescriptor) {
        try {
            Method readMethod = propertyDescriptor.getReadMethod();
            Field field = classz.getDeclaredField(propertyDescriptor.getName());

            if (readMethod.getAnnotation(NotNull.class) != null || readMethod.getAnnotation(NotBlank.class) != null || readMethod.getAnnotation(NotEmpty.class) != null
                    || field.getAnnotation(NotNull.class) != null || field.getAnnotation(NotBlank.class) != null || field.getAnnotation(NotEmpty.class) != null) {
                return true;
            }
        } catch (NoSuchFieldException ignored) {
        }

        return false;
    }

    private String removeBrackets(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        return str.substring(1, str.length() - 1);
    }
}
