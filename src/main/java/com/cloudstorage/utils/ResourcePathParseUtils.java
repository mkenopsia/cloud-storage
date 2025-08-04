package com.cloudstorage.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ResourcePathParseUtils {

    public static String getFileName(String fullFilePath) {
        if(fullFilePath.endsWith("/")) return "";
        if(!fullFilePath.contains("/")) return fullFilePath;

        String[] parts = fullFilePath.split("/");

        return parts[parts.length - 1];
    }

    public static String getFilePath(String fullFilePath) {
        String[] parts = fullFilePath.split("/");

        if(parts.length == 1) return "/";

        return Arrays.stream(parts)
                .limit(parts.length - 1)
                .collect(Collectors.joining("/")) + "/";
    }

    public static String getPathWithoutUserPrefix(String fullFilePath) {
        String[] parts = fullFilePath.split("/");

        if(parts.length <= 1) return "/";

        return Arrays.stream(parts)
                .skip(1)
                .limit(parts.length - 2)
                .collect(Collectors.joining("/", "", "/"));
    }

    public static String getDirectoryPathWithoutUserPrefix(String fullDirectoryPath) {
        String[] parts = fullDirectoryPath.split("/");

        if(parts.length <= 2) return "/";

        return Arrays.stream(parts)
                .skip(1)
                .limit(parts.length - 2)
                .collect(Collectors.joining("/", "", "/"));
    }
}
