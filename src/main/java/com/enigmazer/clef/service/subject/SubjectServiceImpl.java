package com.enigmazer.clef.service.subject;

import com.enigmazer.clef.dto.subject.common.SubjectUpdatedAtResponse;
import com.enigmazer.clef.dto.subject.student.SubjectDetailsStudentResponse;
import com.enigmazer.clef.dto.subject.student.SubjectJoinRequest;
import com.enigmazer.clef.dto.subject.student.SubjectsSummaryStudentResponse;
import com.enigmazer.clef.dto.subject.teacher.*;
import com.enigmazer.clef.dto.user.TeacherProfileResponse;
import com.enigmazer.clef.entity.*;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.EnrollmentMapper;
import com.enigmazer.clef.mapper.SubjectMapper;
import com.enigmazer.clef.mapper.UserMapper;
import com.enigmazer.clef.repository.EnrollmentRepository;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.repository.UserRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.storage.StorageService;
import com.enigmazer.clef.util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    private final StorageService storageService;

    private final SubjectMapper subjectMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public SubjectDetailsTeacherResponse createSubject(SubjectCreationRequest request, Long teacherId) {
        User teacher = userRepository.getReferenceById(teacherId);

        String name = request.name().trim();
        if (subjectRepository.existsByNameAndTeacherId(name, teacherId)) {
            throw new ResourceAlreadyExistsException("Subject " + name + " already exists");
        }

        String description = null;
        if(request.description() != null) {
             description = request.description().trim();
        }
        Subject savedSubject = subjectRepository.save(
                Subject.builder()
                .name(name)
                .description(description)
                .teacher(teacher)
                .joinCode(getJoinCode())
                .build()
        );

        log.info("Subject created [name={}, id={}, teacherId={}]",
                savedSubject.getName(), savedSubject.getId(), teacherId);
        return subjectMapper.toTeacherResponse(savedSubject);
    }

    @Override
    public List<SubjectsSummaryTeacherResponse> listTeacherSubjects(Long teacherId) {
        log.debug("Returned all subjects [teacherId={}]", teacherId);
        return subjectRepository.findAllByTeacherIdAndIsArchivedFalse(teacherId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    @Override
    public List<SubjectsSummaryTeacherResponse> listArchivedTeacherSubjects(Long teacherId) {
        log.debug("Returned all archived subjects [teacherId={}]", teacherId);
        return subjectRepository.findAllByTeacherIdAndIsArchivedTrue(teacherId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    @Override
    public SubjectDetailsTeacherResponse getTeacherSubjectDetails(Long subjectId, Long teacherId) {
        Subject subject = subjectRepository.findSubjectDetailByIdAndTeacherId(subjectId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        log.debug("Returned subject details [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toTeacherResponse(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public SubjectUpdateResponse updateSubject(Long subjectId, SubjectUpdateRequest request, Long teacherId){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        if (request.name() != null && !request.name().isBlank()) {
            String trimmed = request.name().trim();
            if (subjectRepository.existsByNameAndTeacherId(trimmed, teacherId)) {
                throw new ResourceAlreadyExistsException("Subject " + trimmed + " already exists");
            }
            subject.setName(request.name().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            subject.setDescription(request.description().trim());
        }

        log.info("Successfully updated subject [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    public SubjectUpdateResponse toggleSubjectLock(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        boolean lock = !subject.isLocked();
        subject.setLocked(lock);

        log.info("Toggled lock state [oldLock={}, newLock={}, " +
                "subjectId={}, teacherId={}]", !lock, lock, subject.getId(), teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public SubjectUpdateResponse toggleSubjectArchive(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        boolean archive = !subject.isArchived();
        subject.setArchived(archive);

        log.info("Toggled archive state [oldArchive={}, newArchive={}, " +
                "subjectId={}, teacherId={}]", !archive, archive, subject.getId(), teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public SubjectUpdateResponse setCurrentTopic(
            Long subjectId,
            SubjectCurrentNextTopicRequest request,
            Long teacherId
    ){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(
                        request.topicId(), request.unitId(), subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (topic.getCompletedAt() != null) {
            throw new InvalidRequestException("A completed topic cannot be set as current topic");
        }

        if (subject.getNextTopic() != null && subject.getNextTopic().getId().equals(request.topicId())) {
            throw new InvalidRequestException("Current and next topic cannot be the same");
        }

        subject.setCurrentTopic(topic);

        log.info("Changed current topic for subject [newTopicId={} " +
                "subjectId={}, teacherId={}]", topic.getId(), subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public SubjectUpdateResponse setNextTopic(
            Long subjectId,
            SubjectCurrentNextTopicRequest request,
            Long teacherId
    ){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(
                        request.topicId(), request.unitId(), subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (topic.getCompletedAt() != null) {
            throw new InvalidRequestException("A completed topic cannot be set as next topic");
        }

        if (subject.getCurrentTopic() != null && subject.getCurrentTopic().getId().equals(request.topicId())) {
            throw new InvalidRequestException("Next and current topic cannot be the same");
        }

        subject.setNextTopic(topic);

        log.info("Changed next topic for subject [newTopicId={} " +
                "subjectId={}, teacherId={}]", topic.getId(), subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public void deleteSubject(Long subjectId, Long teacherId) {
       Subject subject = subjectRepository.findSubjectDetailByIdAndTeacherId(subjectId, teacherId)
               .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

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
    public List<SubjectsSummaryStudentResponse> listStudentSubjects(Long studentId) {
        log.debug("Returned all subjects [studentId={}]", studentId);
        return enrollmentRepository.findAllEnrolledSubjectsByStudentId(studentId)
                .stream()
                .map(enrollmentMapper::toSubjectSummaryResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "subjects", key = "#subjectId")
    public SubjectDetailsStudentResponse getStudentSubjectDetails(Long subjectId, Long studentId) {
        Subject subject = subjectRepository.findSubjectDetailByIdAndStudentId(subjectId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        log.debug("Returned subject details [subjectId={}, studentId={}]", subjectId, studentId);
        return subjectMapper.toStudentResponse(subject);
    }

    @Override
    public TeacherProfileResponse getTeacherProfile(Long subjectId, Long studentId) {
        User teacher = subjectRepository.findTeacherByIdAndStudentId(subjectId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        log.debug("Returned teacher profile [subjectId={}, studentId={}]", subjectId, studentId);
        return userMapper.toTeacherProfileResponse(teacher);
    }

    @Override
    @Transactional
    public SubjectsSummaryStudentResponse joinSubject(SubjectJoinRequest request, Long studentId) {
        Subject subject = subjectRepository.findByJoinCode(request.joinCode()).orElseThrow(
                () -> new ResourceNotFoundException("Subject not found with Join Code: " + request.joinCode())
        );

        if (subject.getTeacher().getId().equals(studentId)){
            throw new InvalidRequestException("You cannot join a subject you own");
        }

        if (subject.isLocked() || subject.isArchived()) {
            throw new InvalidRequestException("This subject is not accepting new enrollments");
        }

        User student = userRepository.getReferenceById(studentId);

        if (enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subject.getId())) {
            throw new ResourceAlreadyExistsException("You are already enrolled in this subject");
        }

        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .subject(subject)
                .build()
        );

        log.info("Student successfully joined subject [studentId={}, " +
                "subjectId={}, teacherId={}]", studentId, subject.getId(), subject.getTeacher().getId());
        return enrollmentMapper.toSubjectSummaryResponse(saved);
    }

    @Override
    public SubjectUpdatedAtResponse getSubjectUpdatedAt(Long subjectId, Long userId) {
        Subject subject = subjectRepository.findSubjectByIdAndUserId(subjectId, userId).orElseThrow(
                () -> new ResourceNotFoundException("Subject not found")
        );

        return new SubjectUpdatedAtResponse(subject.getUpdatedAt());
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