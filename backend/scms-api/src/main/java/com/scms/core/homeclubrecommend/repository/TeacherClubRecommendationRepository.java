package com.scms.core.homeclubrecommend.repository;

import com.scms.core.homeclubrecommend.domain.TeacherClubRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TeacherClubRecommendationRepository extends JpaRepository<TeacherClubRecommendationEntity, Long> {
    List<TeacherClubRecommendationEntity> findAllByManagerUserIdOrderByOrderNoAscIdAsc(Long managerUserId);

    List<TeacherClubRecommendationEntity> findAllByManagerUserIdInOrderByManagerUserIdAscOrderNoAscIdAsc(Collection<Long> managerUserIds);

    List<TeacherClubRecommendationEntity> findAllByOrderByManagerUserIdAscOrderNoAscIdAsc();

    void deleteAllByManagerUserId(Long managerUserId);
}
