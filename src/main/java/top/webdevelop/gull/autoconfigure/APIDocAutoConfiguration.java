package top.webdevelop.gull.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.webdevelop.gull.apidoc.APIDocProperties;
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
    public APIDocSpringHandlerMethodMapping apiDocSpringHandlerMethodMapping(APIDocProperties apiDocProperties, ConfigurableApplicationContext applicationContext) {
        return new APIDocSpringHandlerMethodMapping(apiDocProperties, applicationContext);
    }
}
