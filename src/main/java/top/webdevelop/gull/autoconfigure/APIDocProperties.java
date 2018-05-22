package top.webdevelop.gull.autoconfigure;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
    private String outputRootPath;
    private String outputCurrentPath;

    public String getOutputRootPath() {
        if (StringUtils.hasText(outputRootPath)) {
            return outputRootPath.replace("~", FileUtils.getUserDirectoryPath());
        } else {
            return FileUtils.getUserDirectoryPath() + "/apidoc";
        }
    }

    public boolean hasIncludeBean(String beanName) {
        return StringUtils.isEmpty(includeMethodNames) || !contains(includeMethodNames, beanName);
    }

    public boolean hasIncludeMethod(String methodName) {
        return StringUtils.isEmpty(includeMethodNames) || !contains(includeMethodNames, methodName);
    }

    public boolean hasExcludeBean(String beanName) {
        return StringUtils.hasText(excludeBeanNames) && contains(excludeBeanNames, beanName);
    }

    public boolean hasExcludeMethod(String methodName) {
        return StringUtils.hasText(excludeMethodNames) && contains(excludeMethodNames, methodName);
    }

    private boolean contains(String names, String name) {
        List<String> strings = Arrays.asList(names.split("[,;]"));
        return strings.contains(name) || strings.stream().anyMatch(pattern -> Pattern.matches(pattern, name));
    }
}
