package com.gymplanner.gym;

import com.gymplanner.gym.dto.GymResponse;
import com.gymplanner.gym.dto.UpdateGymRequest;
import com.gymplanner.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymRepository gymRepository;
    private final GymMapper gymMapper;

    @Transactional(readOnly = true)
    public GymResponse getCurrentGym(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Gym not found."));
        return gymMapper.toResponse(gym);
    }

    @Transactional
    public GymResponse updateCurrentGym(Long gymId, UpdateGymRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Gym not found."));
        gymMapper.updateEntity(request, gym);
        return gymMapper.toResponse(gymRepository.save(gym));
    }
}
