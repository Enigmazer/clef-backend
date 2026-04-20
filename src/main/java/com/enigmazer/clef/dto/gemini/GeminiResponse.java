package com.enigmazer.clef.dto.gemini;

import java.util.List;

public record GeminiResponse (
        List<Candidate> candidates
){}
