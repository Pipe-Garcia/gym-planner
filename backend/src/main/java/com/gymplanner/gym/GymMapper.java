package com.gymplanner.gym;

import com.gymplanner.gym.dto.GymResponse;
import com.gymplanner.gym.dto.UpdateGymRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GymMapper {

    GymResponse toResponse(Gym gym);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateGymRequest request, @MappingTarget Gym gym);
}
