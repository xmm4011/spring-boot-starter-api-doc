package top.webdevelop.gull.apidoc;

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
    private boolean menu;
    private boolean override;
    private String webRootPackage = "";
    private String includeBeanNames;
    private String includeMethodNames;
    private String outputRootPath = "/";
    private String outputCurrentPath;

    public boolean hasBean(String beanName) {
        return StringUtils.isEmpty(includeMethodNames) || !Arrays.asList(includeMethodNames.split(";")).contains(beanName);
    }

    public boolean hasMethod(String methodName) {
        return StringUtils.isEmpty(includeMethodNames) || !Arrays.asList(includeMethodNames.split(";")).contains(methodName);
    }
}
