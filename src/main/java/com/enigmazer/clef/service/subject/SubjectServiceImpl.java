package com.enigmazer.clef.service.subject;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.dto.topic.TopicCreationUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.dto.unit.UnitParseResponse;
import com.enigmazer.clef.dto.user.TeacherProfileResponse;
import com.enigmazer.clef.entity.*;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.EnrollmentMapper;
import com.enigmazer.clef.mapper.SubjectMapper;
import com.enigmazer.clef.mapper.TopicMapper;
import com.enigmazer.clef.mapper.UserMapper;
import com.enigmazer.clef.repository.*;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.gemini.GeminiService;
import com.enigmazer.clef.service.storage.StorageService;
import com.enigmazer.clef.util.JoinCodeGenerator;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    private final StorageService storageService;
    private final GeminiService geminiService;

    private final SubjectMapper subjectMapper;
    private final TopicMapper topicMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;

    private final ObjectMapper objectMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public SubjectDetailsTeacherResponse createSubject(SubjectCreationRequest request, Long teacherId) {
        User teacher = userRepository.getReferenceById(teacherId);

        Subject newSubject = subjectMapper.toEntity(request);
        newSubject.setTeacher(teacher);
        newSubject.setJoinCode(getJoinCode());

        try {
            Subject savedSubject = subjectRepository.save(newSubject);
            log.info("Subject created [name={}, id={}, teacherId={}]",
                    savedSubject.getName(), savedSubject.getId(), teacherId);
            return subjectMapper.toTeacherResponse(savedSubject);
        }catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("Subject " + request.name() + " already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListTeacherSubjectsResponse> listTeacherSubjects(Long teacherId) {
        log.debug("Returned all subjects [teacherId={}]", teacherId);
        return subjectRepository.findAllByTeacherIdAndIsArchivedFalse(teacherId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ListTeacherSubjectsResponse> listArchivedTeacherSubjects(Long teacherId) {
        log.debug("Returned all archived subjects [teacherId={}]", teacherId);
        return subjectRepository.findAllByTeacherIdAndIsArchivedTrue(teacherId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListStudentSubjectsResponse> listStudentSubjects(Long studentId) {
        log.debug("Returned all subjects [studentId={}]", studentId);
        return enrollmentRepository.findAllStudentEnrollments(studentId)
                .stream()
                .map(enrollmentMapper::toStudentSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledStudentResponse> listEnrolledStudents(Long subjectId, Long teacherId) {
        if (!subjectRepository.existsByIdAndTeacherId(subjectId, teacherId)) {
            throw new ResourceNotFoundException("Subject not found");
        }

        log.debug("Returned all students enrolled in the subject " +
                "[subjectId={}, teacherId={}]", subjectId, teacherId);
        return enrollmentRepository.findEnrollmentsBySubjectId(subjectId, teacherId)
                .stream()
                .map(enrollmentMapper::toResponse)
                .toList();
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
                    .readValue(cleanedResponse, new TypeReference<List<UnitParseResponse>>() {
                    });
        }catch (JacksonException e){
            log.warn("Failed to parse Gemini response as DTO [raw={}]", rawResponse);
            throw new BusinessException("Unable to parse syllabus");
        }

        return unitParseResponses;
    }



    @Override
    @Transactional(readOnly = true)
    public SubjectDetailsTeacherResponse getTeacherSubjectDetails(Long subjectId, Long teacherId) {
        Subject subject = subjectRepository.findSubjectDetailByIdAndTeacherId(subjectId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        log.debug("Returned subject details [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toTeacherResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectDetailsStudentResponse getStudentSubjectDetails(Long subjectId, Long studentId) {

        Subject subject = subjectRepository.findSubjectDetailByIdAndStudentId(subjectId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        log.debug("Returned subject details [subjectId={}, studentId={}]", subjectId, studentId);
        return subjectMapper.toStudentResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherProfileResponse getTeacherProfile(Long subjectId, Long studentId) {
        User teacher = subjectRepository.findTeacherByIdAndStudentId(subjectId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        log.debug("Returned teacher profile [subjectId={}, studentId={}]", subjectId, studentId);
        return userMapper.toTeacherProfileResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public String getSyllabusUrl(Long subjectId, Long userId) {
        Subject subject = subjectRepository.findSubjectByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        if(subject.getSyllabusKey() == null){
            throw new ResourceNotFoundException("No syllabus found for the subject");
        }

        log.debug("Returned syllabus url [subjectId={}, userId={}]", subjectId, userId);
        return storageService.generateSyllabusUrl(subject.getSyllabusKey());
    }

    @Override
    @Transactional
    public SubjectUpdateReqRes updateSubject(Long subjectId, SubjectUpdateReqRes request, Long teacherId){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        try {
            if (request.name() != null && !request.name().isBlank()) {
                subject.setName(request.name().trim());
            }
            if (request.description() != null && !request.description().isBlank()) {
                subject.setDescription(request.description().trim());
            }
            subject = subjectRepository.save(subject);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("Subject " + request.name() + " already exists.");
        }

        log.info("Successfully updated subject [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    public boolean toggleSubjectLock(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        boolean lock = !subject.isLocked();
        subject.setLocked(lock);

        subjectRepository.save(subject);
        log.info("Toggled lock state [oldLock={}, newLock={}, " +
                "subjectId={}, teacherId={}]", !lock, lock, subject.getId(), teacherId);
        return lock;
    }

    @Override
    @Transactional
    public boolean toggleSubjectArchive(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        boolean archive = !subject.isArchived();
        subject.setArchived(archive);

        subjectRepository.save(subject);
        log.info("Toggled archive state [oldArchive={}, newArchive={}, " +
                "subjectId={}, teacherId={}]", !archive, archive, subject.getId(), teacherId);
        return archive;
    }

    @Override
    @Transactional
    public String uploadSyllabus(Long subjectId, MultipartFile file, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        if(subject.getSyllabusKey() != null){
            throw new InvalidRequestException("Delete old syllabus pdf before uploading new one.");
        }

        String syllabusKey = storageService.generateSyllabusKey(file);
        subject.setSyllabusKey(syllabusKey);
        subjectRepository.save(subject);

        storageService.uploadSyllabus(file, syllabusKey);

        log.info("Successfully uploaded syllabus pdf for subject " +
                "[subjectId={}, teacherId={}]", subjectId, teacherId);
        return storageService.generateSyllabusUrl(syllabusKey);
    }

    @Override
    @Transactional
    public SubjectCurrentNextTopicResponse setCurrentTopic(
            Long subjectId,
            SubjectCurrentNextTopicRequest request,
            Long teacherId
    ){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(
                        request.id(), request.unitId(), subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (topic.getCompletedAt() != null) {
            throw new InvalidRequestException("A completed topic cannot be set as current topic");
        }

        if (subject.getNextTopic() != null && subject.getNextTopic().getId().equals(request.id())) {
            throw new InvalidRequestException("Current and next topic cannot be the same");
        }

        subject.setCurrentTopic(topic);

        subjectRepository.save(subject);
        log.info("Changed current topic for subject [newTopicId={} " +
                "subjectId={}, teacherId={}]", topic.getId(), subjectId, teacherId);
        return topicMapper.toResponse(topic);
    }

    @Override
    @Transactional
    public SubjectCurrentNextTopicResponse setNextTopic(
            Long subjectId,
            SubjectCurrentNextTopicRequest request,
            Long teacherId
    ){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(
                        request.id(), request.unitId(), subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (topic.getCompletedAt() != null) {
            throw new InvalidRequestException("A completed topic cannot be set as current topic");
        }

        if (subject.getCurrentTopic() != null && subject.getCurrentTopic().getId().equals(request.id())) {
            throw new InvalidRequestException("Next and current topic cannot be the same");
        }

        subject.setNextTopic(topic);

        subjectRepository.save(subject);
        log.info("Changed next topic for subject [newTopicId={} " +
                "subjectId={}, teacherId={}]", topic.getId(), subjectId, teacherId);
        return topicMapper.toResponse(topic);
    }

    @Override
    @Transactional
    public void deleteSubject(Long subjectId, Long teacherId) {
       Subject subject = subjectRepository.findWithTopicMaterialsByIdAndTeacherId(subjectId, teacherId)
               .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        // Clearing Hibernate session references before delete
        subject.setCurrentTopic(null);
        subject.setNextTopic(null);

        if(subject.getSyllabusKey() != null) {
            storageService.deleteSyllabus(subject.getSyllabusKey());
        }

        List<String> topicMaterialKeys = subject.getUnits().stream()
                .flatMap(unit -> unit.getTopics().stream())
                .flatMap(topic -> topic.getTopicMaterials().stream())
                .map(TopicMaterial::getTopicMaterialKey)
                .filter(Objects::nonNull)
                .toList();

        if(!topicMaterialKeys.isEmpty()){
            storageService.deleteTopicMaterials(topicMaterialKeys);
        }

        subjectRepository.delete(subject);

        log.info("Successfully deleted the subject [subjectId={}, teacherId={}]", subjectId, teacherId);
    }

    @Override
    @Transactional
    public void deleteSyllabus(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        if (subject.getSyllabusKey() == null) {
            throw new BusinessException("Subject has no syllabus to delete");
        }

        storageService.deleteSyllabus(subject.getSyllabusKey());
        subject.setSyllabusKey(null);

        subjectRepository.save(subject);
        log.info("Successfully deleted the syllabus [subjectId={}, teacherId={}]", subjectId, teacherId);
    }

    @Override
    @Transactional
    public SubjectDetailsStudentResponse joinSubject(String joinCode, Long studentId) {
        Subject subject = subjectRepository.findByJoinCode(joinCode).orElseThrow(
                () -> new ResourceNotFoundException("Subject not found with Join Code: " + joinCode)
        );

        if (subject.isLocked() || subject.isArchived()) {
            throw new InvalidRequestException("This subject is not accepting new enrollments");
        }

        if (subject.getTeacher().getId().equals(studentId)){
            throw new InvalidRequestException("You cannot join a subject you own");
        }

        User student = userRepository.getReferenceById(studentId);

        try {
            enrollmentRepository.save(Enrollment.builder()
                    .student(student)
                    .subject(subject)
                    .build()
            );
        }catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("You are already enrolled in this subject");
        }

        Subject fullSubject = subjectRepository.findSubjectDetailByIdAndStudentId(subject.getId(), studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        if(!subject.getTeacher().isShowPhoneToStudents()){
            subject.getTeacher().setPhoneNumbers(null);
        }

        log.info("Student successfully joined subject [studentId={}, " +
                "subjectId={}, teacherId={}]", studentId, subject.getId(), subject.getTeacher().getId());
        return subjectMapper.toStudentResponse(fullSubject);
    }


    @Override
    @Transactional
    public SubjectDetailsTeacherResponse bulkAddUnits(Long subjectId, List<UnitCreationRequest> request, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        int unitOrder = unitRepository.findMaxOrderIndex(subjectId)+1;

        for (UnitCreationRequest unitCreationRequest: request){
            Unit unit;
            try {
                unit = unitRepository.save(
                        Unit.builder()
                                .title(unitCreationRequest.title())
                                .subject(subject)
                                .orderIndex(unitOrder++)
                                .build()
                );
            }catch (DataIntegrityViolationException ex){
                throw new ResourceAlreadyExistsException(
                        "Unit " + unitCreationRequest.title() + " already exists in this subject");
            }

            int topicOrder = 1;
            for (TopicCreationUpdateRequest topicCreationUpdateRequest : unitCreationRequest.topics()){
                try {
                    topicRepository.save(
                            Topic.builder()
                                    .title(topicCreationUpdateRequest.title())
                                    .unit(unit)
                                    .orderIndex(topicOrder++)
                                    .build()
                    );
                }catch (DataIntegrityViolationException ex){
                    throw new ResourceAlreadyExistsException(
                            "Topic " + topicCreationUpdateRequest.title() + " already exists in this unit"
                    );
                }
            }
        }
        log.info("Bulk units created [subjectId={}, teacherId={}]", subjectId, teacherId);

        return getTeacherSubjectDetails(subjectId, teacherId);
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

    // generates a unique 6-character join code — throws BusinessException after 10 failed attempts
    private String getJoinCode() {
        int attempts = 0;
        String code;
        do {
            if (attempts++ > 10) {
                throw new BusinessException("Failed to generate unique join code");
            }
            code = JoinCodeGenerator.generate();
        } while (subjectRepository.existsByJoinCode(code));
        return code;
    }
}