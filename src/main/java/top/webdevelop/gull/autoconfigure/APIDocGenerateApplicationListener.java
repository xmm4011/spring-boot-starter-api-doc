package top.webdevelop.gull.autoconfigure;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import top.webdevelop.gull.apidoc.APIDocSpringHandlerMethodMapping;

/**
 * Created by xumingming on 2018/4/9.
 */
public class APIDocGenerateApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (event.getApplicationContext().containsBean("apiDocSpringHandlerMethodMapping")) {
            event.getApplicationContext().getBean(APIDocSpringHandlerMethodMapping.class).handler();
        }
    }
}
