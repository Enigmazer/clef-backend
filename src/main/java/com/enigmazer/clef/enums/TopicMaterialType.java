package com.enigmazer.clef.enums;

import com.enigmazer.clef.exception.InvalidRequestException;

public enum TopicMaterialType {
    AUDIO,
    VIDEO,
    PDF,
    IMAGE,
    DOCUMENT;

    public static TopicMaterialType fromMimeType(String mimeType) {
        if (mimeType == null) throw new InvalidRequestException("Unsupported file type");
        return switch (mimeType) {
            case "audio/mpeg", "audio/mp4" -> AUDIO;
            case "video/mp4", "video/x-matroska" -> VIDEO;
            case "application/pdf" -> PDF;
            case "image/jpeg", "image/png" -> IMAGE;
            case "application/msword",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DOCUMENT;
            default -> throw new InvalidRequestException("Unsupported file type");
        };
    }
}
