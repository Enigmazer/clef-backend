package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.topicMaterial.TopicMaterialResponse;
import com.enigmazer.clef.entity.TopicMaterial;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TopicMaterialMapper {

    TopicMaterialResponse toResponse(TopicMaterial topicMaterial);
}
