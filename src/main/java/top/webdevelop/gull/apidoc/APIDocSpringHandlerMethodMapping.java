package top.webdevelop.gull.apidoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import top.webdevelop.gull.autoconfigure.APIDocProperties;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Created by xumingming on 2018/3/23.
 */

public class APIDocSpringHandlerMethodMapping implements EmbeddedValueResolverAware {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

    private APIDocProperties apiDocProperties;
    private ApplicationContext applicationContext;
    private StringValueResolver embeddedValueResolver;
    private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
    private DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private APIDocMenu menu = null;

    public APIDocSpringHandlerMethodMapping(APIDocProperties apiDocProperties, ApplicationContext applicationContext) {
        this.apiDocProperties = apiDocProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    public void handler() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);

        for (String beanName : beanNames) {

            if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
                Class<?> beanType = null;
                try {
                    beanType = applicationContext.getType(beanName);
                } catch (Throwable ex) {
                    // An unresolvable bean type, probably from a lazy bean - let's ignore it.
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
                    }
                }
                if (beanType != null && isHandler(beanType)) {
                    detectHandlerMethods(beanName);
                }
            }
        }

        APIDocMetadataGenerator.newInstance().generateMenu(apiDocProperties.getOutputRootPath(), this.menu);
    }

    protected void detectHandlerMethods(final Object handler) {
        Class<?> handlerType = (handler instanceof String ?
                applicationContext.getType((String) handler) : handler.getClass());

        if (handlerType != null) {
            final Class<?> userType = ClassUtils.getUserClass(handlerType);
            Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(userType,
                    (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method -> {
                        try {
                            return getMappingForMethod(method, userType);
                        } catch (Throwable ex) {
                            throw new IllegalStateException("Invalid mapping on handler class [" +
                                    userType.getName() + "]: " + method, ex);
                        }
                    });

            logger.debug("{} request handler methods found on {}: {}", methods.size(), userType, methods);


            for (Map.Entry<Method, RequestMappingInfo> entry : methods.entrySet()) {
                Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
                RequestMappingInfo mapping = entry.getValue();
                RequestMappingInfo typeInfo = createRequestMappingInfo(userType);

                if (handler.equals("basicErrorController")
                        || !apiDocProperties.hasIncludeBean(handler.toString())
                        || !apiDocProperties.hasIncludeMethod(invocableMethod.getName())
                        || apiDocProperties.hasExcludeBean(handler.toString())
                        || apiDocProperties.hasExcludeMethod(invocableMethod.getName())) {
                    continue;
                }

                logger.info("APIDoc detect RequestMappingInfo: {}", mapping);
                logger.info("APIDoc detect Method {}", invocableMethod);

                APIDocMetadataPath path = APIDocMetadataPath.newBuilder().setApiDocProperties(apiDocProperties).setMethod(invocableMethod).build();
                APIDoc doc = APIDocBuilder.newInstance().setParameterNameDiscoverer(parameterNameDiscoverer).setRequestMappingInfo(mapping).setMethod(invocableMethod).build();
                APIDocMetadataGenerator.newInstance().generate(path, doc);

                this.menu = buildAPIDocMenu(typeInfo, this.menu, path, doc);
            }
        }
    }

    private APIDocMenu buildAPIDocMenu(RequestMappingInfo typeInfo, APIDocMenu menu, APIDocMetadataPath path, APIDoc doc) {
        if (!apiDocProperties.isMenu()) {
            return menu;
        }
        if (menu == null) {
            menu = new APIDocMenu();
        }
        String[] splitPackage = path.getRelativePackage().split("\\.");
        String tabTitle = splitPackage.length > 1 ? splitPackage[1] : "default";
        String pageTitle = Optional.ofNullable(removeBrackets(typeInfo.getPatternsCondition().toString())).filter(x -> x.length() > 0).orElse("default");
        APIDocMenu.Tab tab = menu.getTabs().stream().filter(i -> i.getTitle().equals(tabTitle)).findFirst().orElse(new APIDocMenu.Tab(tabTitle));
        APIDocMenu.Page page = tab.getPages().stream().filter(i -> i.getTitle().equals(pageTitle)).findFirst().orElse(new APIDocMenu.Page(pageTitle));

        page.getMenus().add(new APIDocMenu.Menu(doc.getUrl(), "." + path.getRelativeFileName()));
        tab.getPages().add(page);
        menu.getTabs().add(tab);

        return menu;
    }


    private RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = createRequestMappingInfo(method);
        if (info != null) {
            RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
            if (typeInfo != null) {
                info = typeInfo.combine(info);
            }
        }
        return info;
    }

    private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        RequestCondition<?> condition = (element instanceof Class ?
                getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
        return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
    }

    protected RequestMappingInfo createRequestMappingInfo(
            RequestMapping requestMapping, RequestCondition<?> customCondition) {

        RequestMappingInfo.Builder builder = RequestMappingInfo
                .paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
                .methods(requestMapping.method())
                .params(requestMapping.params())
                .headers(requestMapping.headers())
                .consumes(requestMapping.consumes())
                .produces(requestMapping.produces())
                .mappingName(requestMapping.name());
        if (customCondition != null) {
            builder.customCondition(customCondition);
        }
        return builder.options(this.config).build();
    }

    protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
        if (this.embeddedValueResolver == null) {
            return patterns;
        } else {
            String[] resolvedPatterns = new String[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
            }
            return resolvedPatterns;
        }
    }

    private RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        return null;
    }

    private RequestCondition<?> getCustomMethodCondition(Method method) {
        return null;
    }

    private boolean isHandler(Class<?> beanType) {
        return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
    }

    private String removeBrackets(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        return str.substring(1, str.length() - 1);
    }
}
