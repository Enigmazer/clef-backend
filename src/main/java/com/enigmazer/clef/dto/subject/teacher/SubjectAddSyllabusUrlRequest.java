package com.enigmazer.clef.dto.subject.teacher;

import org.hibernate.validator.constraints.URL;

public record SubjectAddSyllabusUrlRequest(
        @URL
        String syllabusUrl
) {}
