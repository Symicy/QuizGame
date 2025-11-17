package com.example.demo.enums;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Objects;

import org.springframework.web.util.HtmlUtils;

public enum OpenTriviaEncoding {

    DEFAULT(null),
    URL_LEGACY("urlLegacy"),
    URL_3986("url3986"),
    BASE64("base64");

    private final String queryValue;

    OpenTriviaEncoding(String queryValue) {
        this.queryValue = queryValue;
    }

    public String getQueryValue() {
        return queryValue;
    }

    @SuppressWarnings("null")
    public String decode(String value) {
        if (value == null) {
            return "";
        }
        String decoded = switch (this) {
            case BASE64 -> new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            case URL_LEGACY, URL_3986 -> urlDecode(value);
            default -> value;
        };
        String safeValue = Objects.requireNonNullElse(decoded, "");
        String unescaped = HtmlUtils.htmlUnescape(safeValue);
        return unescaped == null ? "" : unescaped.trim();
    }

    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }
}
