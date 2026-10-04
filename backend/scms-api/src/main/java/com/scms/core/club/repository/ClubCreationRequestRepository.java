package com.scms.core.club.repository;

import com.scms.core.club.domain.ClubCreationRequestEntity;
import com.scms.core.club.domain.ClubCreationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubCreationRequestRepository extends JpaRepository<ClubCreationRequestEntity, Long> {
    List<ClubCreationRequestEntity> findAllByApplicantManagerUserIdOrderByCreatedAtDescIdDesc(Long applicantManagerUserId);

    List<ClubCreationRequestEntity> findAllByOrderByCreatedAtDescIdDesc();

    boolean existsByApplicantManagerUserIdAndStatus(Long applicantManagerUserId, ClubCreationRequestStatus status);

    Optional<ClubCreationRequestEntity> findByIdAndApplicantManagerUserId(Long id, Long applicantManagerUserId);
}
