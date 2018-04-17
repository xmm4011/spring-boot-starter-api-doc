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
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;
import sun.reflect.generics.reflectiveObjects.WildcardTypeImpl;
import top.webdevelop.gull.utils.ClassUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * //TODO 暂未处理多个泛型参数
 * //TODO 暂未处理嵌套List
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
                .map(parameter -> this.parseAPIDocFieldByClass(parameter.getGenericParameterType(), parameter, parameter.getParameterType(), parameter.getGenericParameterType()))
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
        if (ClassUtils.isSingleFieldType(returnType)) {
            return Collections.singletonList(new APIDoc.Field("direct return", APIDocFieldType.parse(returnType), false));
        }

        List<Class<?>> parents = new ArrayList<>();
        parents.add(returnType.getSuperclass());
        return parseAPIDocFieldByClass(this.method.getGenericReturnType(), parents, returnType, this.method.getGenericReturnType());
    }

    private List<APIDoc.Field> parseAPIDocFieldByClass(Type rootType, MethodParameter parameter, Class<?> clazz, Type type) {
        List<APIDoc.Field> fields = new ArrayList<>();

        if (ClassUtils.isSingleFieldType(clazz)) {
            RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
            APIDoc.Field field = new APIDoc.Field((requestParam == null || StringUtils.isEmpty(requestParam.name())) ?
                    parameter.getParameterName() : requestParam.name(), APIDocFieldType.parse(clazz), requestParam != null && requestParam.required());

            fields.add(field);
        } else {
            List<Class<?>> parents = new ArrayList<>();
            parents.add(clazz.getSuperclass());
            fields.addAll(parseAPIDocFieldByClass(rootType, parents, clazz, type));
        }

        return fields;
    }

    private List<APIDoc.Field> parseAPIDocFieldByClass(Type rootType, List<Class<?>> parents, Class<?> clazz, Type type) {
        if (ClassUtils.isListType(clazz)) {
            return parseAPIDocFieldByList(rootType, parents, clazz, type);
        }
        if (ClassUtils.isMappingType(clazz)) {
            return parseAPIDocFieldByBean(rootType, parents, clazz, type);
        }
        return new ArrayList<>();
    }

    private List<APIDoc.Field> parseAPIDocFieldByList(Type rootType, List<Class<?>> parents, Class<?> clazz, Type type) {
        if (clazz.isArray()) {
            return parseAPIDocFieldByClass(rootType, parents, clazz.getComponentType(), type);
        } else {
            Type actualTypeArgument = getActualTypeArgument(type, 0);
            if (actualTypeArgument == null) {
                return new ArrayList<>();
            }

            return parseAPIDocFieldByClass(rootType, parents, getRawType(actualTypeArgument), actualTypeArgument);
        }
    }

    private List<APIDoc.Field> parseAPIDocFieldByBean(Type rootType, List<Class<?>> parents, Class<?> clazz, Type type) {
        List<APIDoc.Field> fields = new ArrayList<>();

        if (parents.contains(clazz)) {
            return fields;
        }

        if (ClassUtils.isObjectType(clazz)) {
            int typeVariableIndex = getTypeVariableIndex(parents.get(parents.size() - 1), type);
            Type actualTypeArgument = getActualTypeArgument(rootType, typeVariableIndex);
            if (actualTypeArgument == null) {
                return fields;
            }

            return parseAPIDocFieldByClass(actualTypeArgument, parents, getRawType(actualTypeArgument), getActualTypeArgument(type, typeVariableIndex));
        }

        if (ClassUtils.isMapType(clazz)) {
            Type actualTypeArgument = getActualTypeArgument(type, 1);
            if (actualTypeArgument == null) {
                return fields;
            }

            Class<?> rawType = getRawType(actualTypeArgument);
            APIDoc.Field field = new APIDoc.Field("dynamic key", APIDocFieldType.parse(rawType), false);
            List<APIDoc.Field> childs = parseAPIDocFieldByClass(rootType, parents, rawType, actualTypeArgument);
            if (!CollectionUtils.isEmpty(childs)) {
                field.setChilds(childs);
            }

            fields.add(field);
            return fields;
        }

        try {
            return Arrays.stream(Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors())
                    .filter(i -> i.getReadMethod() != null)
                    .map(i -> this.parseAPIDocFieldByPropertyDescriptor(rootType, parents, clazz, i))
                    .collect(Collectors.toList());
        } catch (IntrospectionException e) {
            logger.warn("parseAPIDocFieldByBean fail", e);
        }

        return fields;
    }

    private APIDoc.Field parseAPIDocFieldByPropertyDescriptor(Type rootType, List<Class<?>> parents, Class<?> clazz, PropertyDescriptor propertyDescriptor) {
        APIDoc.Field field = new APIDoc.Field(propertyDescriptor.getName(),
                APIDocFieldType.parse(propertyDescriptor.getPropertyType()),
                hasRequiredAnnotation(clazz, propertyDescriptor));

        if (ClassUtils.isObjectType(propertyDescriptor.getPropertyType())) {
            Optional.ofNullable(getActualTypeArgument(rootType, 0)).ifPresent(t -> field.setType(APIDocFieldType.parse(getRawType(t))));
        }

        parents.add(clazz);
        List<APIDoc.Field> childs = parseAPIDocFieldByClass(rootType, parents, propertyDescriptor.getPropertyType(), propertyDescriptor.getReadMethod().getGenericReturnType());
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

    private boolean hasRequiredAnnotation(Class<?> clazz, PropertyDescriptor propertyDescriptor) {
        Method readMethod = propertyDescriptor.getReadMethod();
        Field field = null;
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(propertyDescriptor.getName());
                break;
            } catch (NoSuchFieldException ignored) {
            }
        }

        return readMethod.getAnnotation(NotNull.class) != null
                || readMethod.getAnnotation(NotBlank.class) != null
                || readMethod.getAnnotation(NotEmpty.class) != null
                || (field != null && (field.getAnnotation(NotNull.class) != null
                || field.getAnnotation(NotBlank.class) != null
                || field.getAnnotation(NotEmpty.class) != null
        ));
    }

    private String removeBrackets(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        return str.substring(1, str.length() - 1);
    }
}
