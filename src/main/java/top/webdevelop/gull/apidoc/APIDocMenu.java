package top.webdevelop.gull.apidoc;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xumingming on 2018/4/9.
 */
@Data
public class APIDocMenu implements Serializable {
    private Set<Tab> tabs;

    public APIDocMenu() {
        this.tabs = new HashSet<>();
    }

    @Data
    @JSONType(orders = {"title", "pages"})
    public static class Tab implements Serializable {
        private String title;
        private Set<Page> pages;

        public Tab() {
            this.pages = new HashSet<>();
        }

        public Tab(String title) {
            this();
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Tab tab = (Tab) o;

            return title.equals(tab.title);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + title.hashCode();
            return result;
        }
    }

    @Data
    @JSONType(orders = {"title", "menus"})
    public static class Page implements Serializable {
        private String title;
        private Set<Menu> menus;

        public Page() {
            this.menus = new HashSet<>();
        }

        public Page(String title) {
            this();
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Page page = (Page) o;

            return title.equals(page.title);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + title.hashCode();
            return result;
        }
    }

    @Data
    @AllArgsConstructor
    public static class Menu implements Serializable {
        private String title;
        private String url;
    }
}
