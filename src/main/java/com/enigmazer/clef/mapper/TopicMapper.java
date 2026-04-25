package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.subject.teacher.SubjectCurrentNextTopicResponse;
import com.enigmazer.clef.dto.topic.HomeworkTopicResponse;
import com.enigmazer.clef.dto.topic.TopicCompleteResponse;
import com.enigmazer.clef.entity.Topic;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {SimpleUnitMapper.class})
public interface TopicMapper {

    SubjectCurrentNextTopicResponse toResponse(Topic topic);

    TopicCompleteResponse toTopicCompleteResponse(Topic topic);

    HomeworkTopicResponse toHomeworkTopicResponse(Topic topic);
}