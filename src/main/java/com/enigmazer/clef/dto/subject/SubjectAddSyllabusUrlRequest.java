package com.enigmazer.clef.dto.subject;

import org.hibernate.validator.constraints.URL;

public record SubjectAddSyllabusUrlRequest(
        @URL
        String syllabusUrl
) {}
