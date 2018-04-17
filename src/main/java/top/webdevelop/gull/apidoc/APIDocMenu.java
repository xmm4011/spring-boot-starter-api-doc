package top.webdevelop.gull.apidoc;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by xumingming on 2018/4/9.
 */
@Data
public class APIDocMenu implements Serializable {
    private Set<Tab> tabs;

    public APIDocMenu() {
        this.tabs = new TreeSet<>();
    }

    @Data
    @JSONType(orders = {"title", "mapping", "pages"})
    public static class Tab implements Serializable, Comparable<Tab> {
        private String title;
        private String mapping;
        private Set<Page> pages;

        public Tab() {
            this.pages = new TreeSet<>();
        }

        public Tab(String mapping) {
            this();
            this.title = mapping;
            this.mapping = mapping;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Tab tab = (Tab) o;

            return mapping.equals(tab.mapping);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + mapping.hashCode();
            return result;
        }


        @Override
        public int compareTo(Tab o) {
            return this.mapping.compareTo(o.mapping);
        }
    }

    @Data
    @JSONType(orders = {"title", "mapping", "menus"})
    public static class Page implements Serializable, Comparable<Page> {
        private String title;
        private String mapping;
        private Set<Menu> menus;

        public Page() {
            this.menus = new TreeSet<>();
        }

        public Page(String mapping) {
            this();
            this.title = mapping;
            this.mapping = mapping;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Page page = (Page) o;

            return mapping.equals(page.mapping);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + mapping.hashCode();
            return result;
        }

        @Override
        public int compareTo(Page o) {
            return this.mapping.compareTo(o.mapping);
        }
    }

    @Data
    @NoArgsConstructor
    @JSONType(orders = {"title", "mapping", "action", "url"})
    public static class Menu implements Serializable, Comparable<Menu> {
        private String title;
        private String mapping;
        private String action;
        private String url;

        public Menu(String title, String mapping, String action, String url) {
            this.title = title;
            this.mapping = mapping;
            this.action = action;
            this.url = url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Menu menu = (Menu) o;

            return url.equals(menu.url);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + url.hashCode();
            return result;
        }

        @Override
        public int compareTo(Menu o) {
            return Optional.of(this.mapping.compareTo(o.mapping)).filter(x -> x != 0).orElseGet(() -> this.action.compareTo(o.action));
        }
    }
}
