package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.topic.TopicResponse;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;
import com.enigmazer.clef.entity.Topic;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TopicMaterialMapper.class})
public interface SimpleTopicMapper {
    // Maps a Topic without its Unit
    TopicResponse toResponse(Topic topic);

    TopicUpdateResponse toUpdateResponse(Topic topic);
}