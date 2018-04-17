package top.webdevelop.gull.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Created by xumingming on 2018/3/24.
 */
@Configuration
@ConfigurationProperties("api.doc")
@Data
public class APIDocProperties {
    private boolean auto;
    private String webRootPackage = "";
    private String includeBeanNames;
    private String includeMethodNames;
    private String excludeBeanNames;
    private String excludeMethodNames;
    private String outputRootPath = "/";
    private String outputCurrentPath;

    public boolean hasIncludeBean(String beanName) {
        return StringUtils.isEmpty(includeMethodNames) || !Arrays.asList(includeMethodNames.split(";")).contains(beanName);
    }

    public boolean hasIncludeMethod(String methodName) {
        return StringUtils.isEmpty(includeMethodNames) || !Arrays.asList(includeMethodNames.split(";")).contains(methodName);
    }

    public boolean hasExcludeBean(String beanName) {
        return StringUtils.hasText(excludeBeanNames) && Arrays.asList(excludeBeanNames.split(";")).contains(beanName);
    }

    public boolean hasExcludeMethod(String methodName) {
        return StringUtils.hasText(excludeMethodNames) && Arrays.asList(excludeMethodNames.split(";")).contains(methodName);
    }
}
