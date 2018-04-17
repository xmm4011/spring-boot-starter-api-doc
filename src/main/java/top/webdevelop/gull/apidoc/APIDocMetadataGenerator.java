package top.webdevelop.gull.apidoc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
        try {
            if (file.exists()) {
                logger.info("generate api doc file exists, merge begin");
                merge(apiDoc, JSON.parseObject(FileUtils.readFileToString(file), APIDoc.class));
            }
        } catch (IOException e) {
            logger.error("merge fail", e);
        }

        logger.info("generate api doc: {}", JSON.toJSONString(apiDoc));
        try {
            FileUtils.writeStringToFile(file, JSON.toJSONString(apiDoc, true));
        } catch (IOException e) {
            logger.warn("write fail", e);
        }
    }

    private void merge(APIDoc apiDoc, APIDoc oldApiDoc) {
        merge(apiDoc.getRequest(), oldApiDoc.getRequest());
        merge(apiDoc.getResponse(), oldApiDoc.getResponse());
    }

    private void merge(List<APIDoc.Field> fields, List<APIDoc.Field> oldFields) {
        if (CollectionUtils.isEmpty(fields) || CollectionUtils.isEmpty(oldFields)) {
            return;
        }

        fields.forEach(field -> {
            Optional<APIDoc.Field> oldField = oldFields.stream().filter(of -> field.getName().equals(of.getName())).findFirst();
            if (!oldField.isPresent()) {
                return;
            }
            field.setDesc(oldField.get().getDesc());

            merge(field.getChilds(), oldField.get().getChilds());
        });
    }

    public void generateMenu(String outputPath, APIDocMenu apiDocMenu) {
        if (apiDocMenu == null) {
            return;
        }

        logger.info("generate api doc menu path: {}", outputPath);
        File file = new File(FilenameUtils.concat(outputPath, "menu.json"));
        try {
            if (file.exists()) {
                logger.info("generate api doc menu file exists, merge begin");
                mergeMenu(apiDocMenu, JSON.parseObject(FileUtils.readFileToString(file), APIDocMenu.class));
            }
        } catch (IOException e) {
            logger.warn("merge menu fail", e);
        }

        logger.info("generate api doc menu: {}", JSON.toJSONString(apiDocMenu));
        try {
            FileUtils.writeStringToFile(file, JSON.toJSONString(apiDocMenu, true));
        } catch (IOException e) {
            logger.warn("write menu fail", e);
        }
    }

    private void mergeMenu(APIDocMenu apiDocMenu, APIDocMenu oldApiDocMenu) {
        apiDocMenu.getTabs().forEach(tab -> {
            Optional<APIDocMenu.Tab> oldTab = oldApiDocMenu.getTabs().stream().filter(ot -> tab.getMapping().equals(ot.getMapping())).findFirst();
            if (!oldTab.isPresent()) {
                return;
            }
            tab.setTitle(oldTab.get().getTitle());

            tab.getPages().forEach(page -> {
                Optional<APIDocMenu.Page> oldPage = oldTab.get().getPages().stream().filter(op -> page.getMapping().equals(op.getMapping())).findFirst();
                if (!oldPage.isPresent()) {
                    return;
                }
                page.setTitle(oldPage.get().getTitle());

                page.getMenus().forEach(menu -> {
                    Optional<APIDocMenu.Menu> oldMenu = oldPage.get().getMenus().stream().filter(om -> menu.getUrl().equals(om.getUrl())).findFirst();
                    if (!oldMenu.isPresent()) {
                        return;
                    }

                    menu.setTitle(oldMenu.get().getTitle());
                });
            });
        });
    }
}
