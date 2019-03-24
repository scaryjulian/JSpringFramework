package com.jspringframework.mvc.utils;

public class StringUtils {

    public static String lowerFirstChar(String str) {
        if (str == null) {
            return "";
        }
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public static String urlPattern(String url) {
        if (url == null) {
            return "/";
        }
        return ("/" +url.trim()).replaceAll("//+", "/");

    }
}
