package com.wingman.sammy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Set;

@ConfigurationProperties(prefix = "sammy.media")
public record MediaProperties(
        int maxSizeBytes,
        Set<String> allowedMimeTypes,
        Path storageDirectory
) {}
