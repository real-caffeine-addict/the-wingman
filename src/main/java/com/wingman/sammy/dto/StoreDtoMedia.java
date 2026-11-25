package com.wingman.sammy.dto;

import java.time.LocalDateTime;

public record StoreDtoMedia (
        String fileName,
        String mimeType,
        int byteSize,
        LocalDateTime received,
        String path,
        String messageId,
        String senderJid,
        String hash
) {}
