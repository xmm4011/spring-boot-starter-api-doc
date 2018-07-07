package top.webdevelop.gull.apidoc;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;
import sun.reflect.generics.reflectiveObjects.WildcardTypeImpl;
import top.webdevelop.gull.annotation.APIDocIgnore;
import top.webdevelop.gull.utils.ClassUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * //TODO 相同深度支持合并
 * //TODO API删除的情况
 * Created by xumingming on 2018/3/24.
 */
public class APIDocBuilder {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private ParameterNameDiscoverer parameterNameDiscoverer;
    private RequestMappingInfo requestMappingInfo;
    private Method method;

    public static APIDocBuilder newInstance() {
        return new APIDocBuilder();
    }

    public APIDocBuilder setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
        return this;
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

    String parseAPIDocUrl() {
        return removeBrackets(this.requestMappingInfo.getPatternsCondition().toString());
    }

    private String parseAPIDocAction() {
        return removeBrackets(this.requestMappingInfo.getMethodsCondition().toString());
    }

    private List<APIDoc.Field> parseAPIDocRequest() {
        List<MethodParameter> parameters = new ArrayList<>();
        for (int i = 0; i < this.method.getParameterTypes().length; i++) {
            SynthesizingMethodParameter parameter = new SynthesizingMethodParameter(this.method, i);
            parameter.initParameterNameDiscovery(parameterNameDiscoverer);
            parameters.add(parameter);
        }

        return parameters.stream()
                .filter(this::nonIgnoreRequestParam)
                .map(parameter -> this.parseAPIDocFieldByParameter(parameter.getGenericParameterType(), parameter, parameter.getParameterType()))
                .reduce(new ArrayList<>(), (left, right) -> {
                    left.addAll(right);
                    return left;
                });
    }

    private boolean nonIgnoreRequestParam(MethodParameter parameter) {
        return parameter.getParameterAnnotation(PathVariable.class) == null && !parameter.getParameterType().equals(HttpServletRequest.class) && !parameter.getParameterType().equals(HttpServletResponse.class);
    }

    private List<APIDoc.Field> parseAPIDocResponse() {
        Class<?> returnType = this.method.getReturnType();
        if (returnType.equals(ModelAndView.class)) {
            return Collections.emptyList();
        }
        if (ClassUtils.isSingleFieldType(returnType)) {
            return Collections.singletonList(new APIDoc.Field("direct return", APIDocFieldType.parse(returnType), false));
        }

        List<Class<?>> beanClasses = new ArrayList<>();
        beanClasses.add(returnType.getSuperclass());
        return parseAPIDocFieldByClass(this.method.getGenericReturnType(), beanClasses, returnType.getSuperclass(), returnType, this.method.getGenericReturnType());
    }

    private List<APIDoc.Field> parseAPIDocFieldByParameter(Type parentType, MethodParameter parameter, Class<?> clazz) {
        if (ClassUtils.isSingleFieldType(clazz)) {
            RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
            APIDoc.Field field = new APIDoc.Field((requestParam == null || StringUtils.isEmpty(requestParam.name())) ? parameter.getParameterName() : requestParam.name(),
                    APIDocFieldType.parse(clazz),
                    requestParam != null && requestParam.required());
            return Collections.singletonList(field);
        }

        List<Class<?>> beanClasses = new ArrayList<>();
        beanClasses.add(clazz.getSuperclass());
        List<APIDoc.Field> fields = parseAPIDocFieldByClass(parentType, beanClasses, clazz.getSuperclass(), clazz, parentType);
        if (ClassUtils.isListType(clazz)) {
            return Collections.singletonList(new APIDoc.Field("direct parameter", APIDocFieldType.parse(clazz), false, fields));
        }
        return fields;
    }

    private List<APIDoc.Field> parseAPIDocFieldByClass(Type parentType, List<Class<?>> beanClasses, Class<?> parent, Class<?> clazz, Type type) {
        if (ClassUtils.isListType(clazz)) {
            return parseAPIDocFieldByList(parentType, beanClasses, parent, clazz, type);
        }
        if (ClassUtils.isBeanType(clazz)) {
            return parseAPIDocFieldByBean(parentType, beanClasses, parent, clazz, type);
        }

        return new ArrayList<>();
    }

    private List<APIDoc.Field> parseAPIDocFieldByList(Type parentType, List<Class<?>> beanClasses, Class<?> parent, Class<?> clazz, Type type) {
        if (clazz.isArray()) {
            return parseAPIDocFieldByClass(parentType, beanClasses, parent, clazz.getComponentType(), type);
        } else {
            Type actualTypeArgument = getActualTypeArgument(type, 0);
            if (actualTypeArgument == null) {
                return new ArrayList<>();
            }

            Class<?> rawType = getRawType(actualTypeArgument);
            if (ClassUtils.isSingleFieldType(rawType)) {
                return Collections.singletonList(new APIDoc.Field("direct parameter", APIDocFieldType.parse(rawType), false));
            }

            return parseAPIDocFieldByClass(parentType, beanClasses, parent, rawType, actualTypeArgument);
        }
    }

    private List<APIDoc.Field> parseAPIDocFieldByBean(Type parentType, List<Class<?>> beanClasses, Class<?> parent, Class<?> clazz, Type type) {
        List<APIDoc.Field> fields = new ArrayList<>();

        if (beanClasses.contains(clazz)) {
            return fields;
        }

        if (ClassUtils.isObjectType(clazz)) {
            Type actualTypeArgument = getActualTypeArgument(parentType, parent, type);
            if (actualTypeArgument == null) {
                return fields;
            }

            return parseAPIDocFieldByClass(actualTypeArgument, beanClasses, parent, getRawType(actualTypeArgument), actualTypeArgument);
        }

        if (ClassUtils.isMapType(clazz)) {
            Type actualTypeArgument = getActualTypeArgument(type, 1);
            if (actualTypeArgument == null) {
                return fields;
            }

            Class<?> rawType = getRawType(actualTypeArgument);
            APIDoc.Field field = new APIDoc.Field("dynamic key", APIDocFieldType.parse(rawType), false);
            List<APIDoc.Field> childs = parseAPIDocFieldByClass(parentType, beanClasses, parent, rawType, actualTypeArgument);
            if (!CollectionUtils.isEmpty(childs)) {
                field.setChilds(childs);
            }

            fields.add(field);
            return fields;
        }

        beanClasses.add(clazz);
        try {
            return Arrays.stream(Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors())
                    .filter(i -> i.getReadMethod() != null)
                    .map(i -> this.parseAPIDocFieldByPropertyDescriptor(type, beanClasses, clazz, i))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IntrospectionException e) {
            logger.warn("parseAPIDocFieldByBean fail", e);
        }

        return fields;
    }

    private APIDoc.Field parseAPIDocFieldByPropertyDescriptor(Type parentType, List<Class<?>> beanClasses, Class<?> clazz, PropertyDescriptor propertyDescriptor) {
        Field propertyField = getField(clazz, propertyDescriptor.getName());
        if (propertyField != null && Optional.ofNullable(propertyField.getAnnotation(APIDocIgnore.class)).map(APIDocIgnore::value).orElse(false)) {
            return null;
        }

        APIDoc.Field field = new APIDoc.Field(propertyDescriptor.getName(),
                APIDocFieldType.parse(propertyDescriptor.getPropertyType()),
                hasRequiredAnnotation(clazz, propertyDescriptor));

        if (ClassUtils.isObjectType(propertyDescriptor.getPropertyType())) {
            Optional.ofNullable(getActualTypeArgument(parentType, clazz, propertyDescriptor.getReadMethod().getGenericReturnType()))
                    .ifPresent(t -> field.setType(APIDocFieldType.parse(getRawType(t))));
        }

        List<APIDoc.Field> childs = parseAPIDocFieldByClass(parentType, beanClasses, clazz, propertyDescriptor.getPropertyType(), propertyDescriptor.getReadMethod().getGenericReturnType());
        if (!CollectionUtils.isEmpty(childs)) {
            field.setChilds(childs);
        }

        return field;
    }

    private int getTypeVariableIndex(Class<?> clazz, Type type) {
        TypeVariable<? extends Class<?>>[] typeParameters = clazz.getTypeParameters();
        for (int i = 0; i < typeParameters.length; i++) {
            if (typeParameters[i].getTypeName().equals(type.getTypeName())) {
                return i;
            }
        }

        return 0;
    }

    private Type getActualTypeArgument(Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            if (actualTypeArguments.length > index) {
                return actualTypeArguments[index];
            }
        }

        return null;
    }

    private Type getActualTypeArgument(Type parentType, Class<?> clazz, Type type) {
        return getActualTypeArgument(parentType, getTypeVariableIndex(clazz, type));
    }

    private Class<?> getRawType(Type type) {
        //TODO GenericArrayType
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof WildcardTypeImpl) {
            return Object.class;//TODO
        } else if (type instanceof TypeVariableImpl) {
            return Object.class;//TODO
        } else {
            return ((ParameterizedTypeImpl) type).getRawType();
        }
    }

    private Field getField(Class<?> clazz, String name) {
        Field field = null;
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException ignored) {
            }
        }

        return field;
    }

    private boolean hasRequiredAnnotation(Class<?> clazz, PropertyDescriptor propertyDescriptor) {
        Method readMethod = propertyDescriptor.getReadMethod();
        Field field = getField(clazz, propertyDescriptor.getName());

        return hasAnnotation(readMethod, NotNull.class)
                || hasAnnotation(readMethod, NotBlank.class)
                || hasAnnotation(readMethod, NotEmpty.class)
                || hasAnnotation(field, NotNull.class)
                || hasAnnotation(field, NotBlank.class)
                || hasAnnotation(field, NotEmpty.class);
    }

    private <T extends Annotation> boolean hasAnnotation(Method method, Class<T> annotationClass) {
        if (method == null) {
            return false;
        }

        return method.getAnnotation(annotationClass) != null;
    }

    private <T extends Annotation> boolean hasAnnotation(Field field, Class<T> annotationClass) {
        if (field == null) {
            return false;
        }

        return field.getAnnotation(annotationClass) != null;
    }

    private String removeBrackets(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        return str.substring(1, str.length() - 1);
    }
}
