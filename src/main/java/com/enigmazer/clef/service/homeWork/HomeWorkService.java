package com.enigmazer.clef.service.homeWork;

import com.enigmazer.clef.dto.homework.HomeWorkUpdateRequest;
import com.enigmazer.clef.dto.homework.HomeworkResponse;

public interface HomeWorkService {
    HomeworkResponse updateHomeWork(Long subjectId, Long homeWorkId, HomeWorkUpdateRequest request, Long teacherId);

    void deleteHomeWork(Long subjectId, Long homeWorkId, Long teacherId);
}
