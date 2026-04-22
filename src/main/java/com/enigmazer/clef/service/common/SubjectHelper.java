package com.enigmazer.clef.service.common;

import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectHelper {

    private final SubjectRepository subjectRepository;


    public Subject findSubjectByIdAndTeacherId(Long subjectId, Long teacherId){
        return subjectRepository.findByIdAndTeacherId(subjectId, teacherId).orElseThrow(
                () -> new ResourceNotFoundException("Subject not found")
        );
    }

    public void checkArchived(Subject subject){
        if(subject.isArchived()){
            throw new InvalidRequestException("Archived subject cannot be modified.");
        }
    }
}
