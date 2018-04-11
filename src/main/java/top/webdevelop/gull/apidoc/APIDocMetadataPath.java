package top.webdevelop.gull.apidoc;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import top.webdevelop.gull.autoconfigure.APIDocProperties;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by xumingming on 2018/3/24.
 */
@Getter
public class APIDocMetadataPath {
    private String relativePackage;
    private String absolutePath;
    private String relativePath;
    private String fileName;
    private String absoluteFileName;
    private String relativeFileName;
    private boolean override;

    private APIDocMetadataPath(Builder builder) {
        this.relativePackage = builder.method.getDeclaringClass().getPackage().getName()
                .replace(builder.apiDocProperties.getWebRootPackage(), "");
        this.relativePath = FilenameUtils.normalizeNoEndSeparator("/data/" + Optional.ofNullable(builder.apiDocProperties.getOutputCurrentPath())
                .filter(c -> c.length() > 0)
                .orElseGet(() -> this.relativePackage.replace(".", "/")));
        this.absolutePath = FilenameUtils.normalizeNoEndSeparator(builder.apiDocProperties.getOutputRootPath() + this.relativePath);

        this.fileName = builder.method.getName() + ".json";
        this.absoluteFileName = FilenameUtils.concat(this.absolutePath, this.fileName);
        this.relativeFileName = FilenameUtils.concat(this.relativePath, this.fileName);
        this.override = builder.apiDocProperties.isOverride();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private APIDocProperties apiDocProperties;
        private Method method;

        public Builder setApiDocProperties(APIDocProperties apiDocProperties) {
            this.apiDocProperties = apiDocProperties;
            return this;
        }

        public Builder setMethod(Method method) {
            this.method = method;
            return this;
        }


        public APIDocMetadataPath build() {
            return new APIDocMetadataPath(this);
        }
    }
}
