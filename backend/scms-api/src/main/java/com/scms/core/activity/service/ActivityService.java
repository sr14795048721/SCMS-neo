package com.scms.core.activity.service;

import com.scms.core.activity.domain.ActivityEntity;
import com.scms.core.activity.dto.ActivityResponse;
import com.scms.core.activity.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list() {
        return activityRepository.findAllPublicVisible()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityResponse toResponse(ActivityEntity entity) {
        return new ActivityResponse(
                entity.getId(),
                entity.getClubId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCapacity(),
                entity.getStatus().name(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
