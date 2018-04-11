package top.webdevelop.gull.apidoc;

import com.alibaba.fastjson.annotation.JSONType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by xumingming on 2018/3/24.
 */
@Data
@AllArgsConstructor
@JSONType(orders = {"url", "action", "request", "response"})
public class APIDoc implements Serializable {
    private String url;
    private String action;
    private List<Field> request;
    private List<Field> response;

    @Data
    @JSONType(orders = {"name", "type", "required", "desc"})
    static class Field implements Serializable {
        private String name;
        private APIDocFieldType type;
        private boolean required;
        private String desc = "";

        private List<Field> childs;

        public Field(String name, APIDocFieldType type) {
            this.name = name;
            this.type = type;
            this.desc = "";
        }

        public Field(String name, APIDocFieldType type, boolean required) {
            this(name, type);
            this.required = required;
        }
    }
}
