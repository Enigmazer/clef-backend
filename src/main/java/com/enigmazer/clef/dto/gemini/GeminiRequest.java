package com.enigmazer.clef.dto.gemini;

import java.util.List;

public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig
) {}

