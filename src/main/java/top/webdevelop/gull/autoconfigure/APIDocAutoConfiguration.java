package top.webdevelop.gull.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import top.webdevelop.gull.apidoc.APIDocSpringHandlerMethodMapping;

/**
 * Created by xumingming on 2018/4/8.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "api.doc",
        name = {"auto"},
        havingValue = "true"
)
@EnableConfigurationProperties({APIDocProperties.class})
public class APIDocAutoConfiguration {
    @Bean
    @Lazy(false)
    public APIDocSpringHandlerMethodMapping apiDocSpringHandlerMethodMapping(APIDocProperties apiDocProperties, ApplicationContext applicationContext) {
        return new APIDocSpringHandlerMethodMapping(apiDocProperties, applicationContext);
    }
}
