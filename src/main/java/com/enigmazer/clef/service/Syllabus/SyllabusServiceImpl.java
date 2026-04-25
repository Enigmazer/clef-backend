package com.enigmazer.clef.service.Syllabus;

import com.enigmazer.clef.dto.subject.common.SubjectSyllabusUrlResponse;
import com.enigmazer.clef.dto.subject.teacher.SubjectUpdateResponse;
import com.enigmazer.clef.dto.unit.UnitParseResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.Unit;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.SubjectMapper;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.UnitRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.gemini.GeminiService;
import com.enigmazer.clef.service.storage.StorageService;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyllabusServiceImpl implements SyllabusService{

    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;

    private final StorageService storageService;
    private final GeminiService geminiService;

    private final SubjectMapper subjectMapper;

    private final ObjectMapper objectMapper;

    private final SubjectHelper subjectHelper;

    @Override
    public SubjectSyllabusUrlResponse getSyllabusUrl(Long subjectId, Long userId) {
        Subject subject = subjectRepository.findSubjectByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        String syllabusKey = subject.getSyllabusKey();
        if(syllabusKey == null){
            throw new ResourceNotFoundException("No syllabus found for the subject");
        }

        String url = storageService.generateSyllabusUrl(syllabusKey);

        log.info("Returned syllabus url [subjectId={}, userId={}]", subjectId, userId);
        return new SubjectSyllabusUrlResponse(url);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public SubjectUpdateResponse uploadSyllabus(Long subjectId, MultipartFile file, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        if(subject.getSyllabusKey() != null){
            throw new InvalidRequestException("Delete old syllabus pdf before uploading new one.");
        }

        String syllabusKey = storageService.generateSyllabusKey(file);
        subject.setSyllabusKey(syllabusKey);

        storageService.uploadSyllabus(file, syllabusKey);

        log.info("Successfully uploaded syllabus pdf for subject " +
                "[subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public SubjectUpdateResponse deleteSyllabus(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        if (subject.getSyllabusKey() == null) {
            throw new BusinessException("Subject has no syllabus to delete");
        }

        storageService.deleteSyllabus(subject.getSyllabusKey());
        subject.setSyllabusKey(null);

        log.info("Successfully deleted the syllabus [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    public List<UnitParseResponse> parseSyllabus(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        String syllabusKey = subject.getSyllabusKey();
        if (syllabusKey == null) throw new ResourceNotFoundException("No syllabus found to parse");

        byte[] bytes = storageService.getBytes(syllabusKey);

        List<Unit> units = unitRepository.findWithTopicsBySubjectId(subjectId);

        String prompt = getPromptWithContext(units);

        String rawResponse = geminiService.parse(bytes,prompt);
        String cleanedResponse = getCleanedResponse(rawResponse);

        List<UnitParseResponse> unitParseResponses;
        try {
            unitParseResponses = objectMapper
                    .readValue(cleanedResponse, new TypeReference<List<UnitParseResponse>>() {});
        }catch (JacksonException e){
            log.warn("Failed to parse Gemini response as DTO [raw={}]", rawResponse);
            throw new BusinessException("Unable to parse syllabus");
        }

        log.info("Parsed Gemini response as DTO successfully");
        return unitParseResponses;
    }

    // --- Helper Methods ---
    private static String getPromptWithContext(List<Unit> units) {
        StringBuilder context = new StringBuilder("Existing units (do not duplicate these): ");
        for (Unit unit: units){
            context.append("\n").append(unit.getTitle());

            Set<Topic> topics = unit.getTopics();
            for (Topic topic: topics){
                context.append("\n - ").append(topic.getTitle());
            }
        }

        String existingUnitsBlock = units.isEmpty()
                ? "  (none)"
                : context.toString();

        return """
        You are a syllabus parser. Your only job is to extract unit and topic structure from academic course syllabus PDFs.
        
        Return ONLY a valid JSON array. No markdown, no explanation, no preamble, no code fences.
        If the response is not parseable JSON, it is wrong.
        
        Output schema (use real values, not placeholders):
        [
          {
            "title": "Unit 3: Database Design",
            "topics": [
              { "title": "Entity Relationship Diagrams" },
              { "title": "Normalisation and its Forms" }
            ]
          }
        ]
        
        ═══ STEP 1 - CLASSIFY FIRST ═══
        
        Before extracting anything, determine if this document is a course syllabus.
        
        A course syllabus MUST have ALL of the following:
        - Formal unit or module headings (e.g. "Unit I", "Module 2", "Chapter 3")
        - A list of topics or concepts under each heading
        - It describes what will be TAUGHT in a course
        
        These are NOT syllabi — return [] immediately without extracting anything:
        - Assignment sheets or lab manuals
        - Question papers or solved exercises
        - Source code or code walkthroughes
        - Lecture notes or handouts
        - Project reports or documentation
        - Any file without clear unit/module headings
        
        If the document is not a course syllabus → return [] and stop. Do not proceed to Step 2.
        
        ═══ STEP 2 - BEFORE YOU START EXTRACTION (only if Step 1 passed) ═══
        
        These units ALREADY EXIST. Do NOT include them in output under any circumstances (match by title, case-insensitive):
        %s
        
        If the PDF contains these units, skip them entirely.
         Only extract units that are NOT in the list above.
        
        ═══ STEP 3 - EXTRACT (only if Step 1 and 2 passed) ═══
        
        1. Unit title format must be exactly: "Unit N: Title" — N is a number, no letters, no brackets.
        2. Existing unit count: %d. Start numbering from Unit %d.
        3. If no existing units are listed, extract everything from the PDF.
        4. Extract topic names exactly as they appear in the source text. Do not summarize, rephrase, or add filler words.
        5. Treat every distinct concept as a separate topic. Topics may be separated by commas, semicolons, bullet points, or line breaks in the original — split them into individual topic entries.
        6. Remove trailing punctuation from topic titles (commas, semicolons, bullets) but preserve original terminology.
        7. Topics may repeat across units if the PDF genuinely lists them in multiple units — do not de-duplicate across units.
        8. Do not invent or infer anything not present in the PDF.
        """.formatted(existingUnitsBlock, units.size(), units.size() + 1);
    }

    private String getCleanedResponse(String rawResponse){
        return rawResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
    }
}
