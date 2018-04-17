# API Document Generate Spring Boot Project

## 如何使用

### 1. 编译安装

```sh
mvn clean install
```

### 2.添加依赖至WEB项目

```xml
<dependency>
    <groupId>top.webdevelop.gull</groupId>
    <artifactId>spring-boot-starter-api-doc</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 3. 添加配置

* 在`application.properties`添加，如下：

```properties
api.doc.auto=true
api.doc.web-root-package=top.webdevelop.gull.web
#api.doc.include-bean-names=
#api.doc.include-method-names=
#api.doc.exclude-bean-names=
#api.doc.exclude-method-names=
#api.doc.output-current-path=
api.doc.output-root-path=/Users/xxx/Downloads/test/doc
```

### 4. 发布output静态资源

* 可以建一个repo、指定目录输出接口元数据
* 需要中文的情况下、修改desc属性、加上中文
* 提交元数据文件、持续集成自动部署web容器
 
