package com.group1.banking.dto.chat;

import java.util.List;

public record DeveloperChatResponse(
        String sessionId,
        String answer,
        List<DeveloperChatSource> sources,
        String sourceStatus) {
}
