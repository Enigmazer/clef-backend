package com.enigmazer.clef.service.homeWork;

import com.enigmazer.clef.dto.homework.HomeWorkUpdateRequest;
import com.enigmazer.clef.dto.homework.HomeworkCreationRequest;
import com.enigmazer.clef.dto.homework.HomeworkResponse;
import com.enigmazer.clef.dto.page.PageResponse;

public interface HomeWorkService {
    HomeworkResponse createHomework(Long subjectId, HomeworkCreationRequest request, Long teacherId);

    PageResponse<HomeworkResponse> getHomeWorkPage(Long subjectId, String filter, int page, Long userId);
    
    HomeworkResponse updateHomeWork(Long subjectId, Long homeWorkId, HomeWorkUpdateRequest request, Long teacherId);

    void deleteHomeWork(Long subjectId, Long homeWorkId, Long teacherId);
}
