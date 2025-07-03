package com.cloudstorage.controller.payload;

public record DirectoryPayload(
    String path,
    String name,
    String type
){}
