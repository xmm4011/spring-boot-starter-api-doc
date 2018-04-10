package top.webdevelop.gull.apidoc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by xumingming on 2018/3/24.
 */
public class APIDocMetadataGenerator {
    private Logger logger = LoggerFactory.getLogger(getClass());

    static {
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
    }

    public static APIDocMetadataGenerator newInstance() {
        return new APIDocMetadataGenerator();
    }

    public void generate(APIDocMetadataPath path, APIDoc apiDoc) {
        File file = new File(path.getAbsoluteFileName());

        logger.info("generate api doc path: {}", JSON.toJSONString(path));
        logger.info("generate api doc override: {}", path.isOverride());
        if (file.exists()) {
            logger.info("generate api doc file exists, fileName: {}", path.getAbsoluteFileName());
        }
        logger.info("generate api doc: {}", JSON.toJSONString(apiDoc));

        if (path.isOverride() || !file.exists()) {
            try {
                FileUtils.writeStringToFile(file, JSON.toJSONString(apiDoc, true));
            } catch (IOException ignored) {
            }
        }
    }

    public void generateMenu(String outputPath, APIDocMenu apiDocMenu) {
        if (apiDocMenu == null) {
            return;
        }

        logger.info("generate api doc menu: {}", JSON.toJSONString(apiDocMenu));
        try {
            FileUtils.writeStringToFile(new File(FilenameUtils.concat(outputPath, "menu.json")), JSON.toJSONString(apiDocMenu, true));
        } catch (IOException ignored) {
        }
    }
}
