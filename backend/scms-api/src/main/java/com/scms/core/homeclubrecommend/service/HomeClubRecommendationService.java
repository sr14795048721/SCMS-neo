package com.scms.core.homeclubrecommend.service;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.homeclubrecommend.domain.HomeClubRecommendationEntity;
import com.scms.core.homeclubrecommend.dto.AdminHomeClubRecommendationResponse;
import com.scms.core.homeclubrecommend.dto.PublicHomeRecommendedClubResponse;
import com.scms.core.homeclubrecommend.repository.HomeClubRecommendationRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HomeClubRecommendationService {

    private static final int MAX_RECOMMENDATIONS = 4;

    private final HomeClubRecommendationRepository homeClubRecommendationRepository;
    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public HomeClubRecommendationService(HomeClubRecommendationRepository homeClubRecommendationRepository,
                                         ClubRepository clubRepository,
                                         ClubStudentMemberRepository clubStudentMemberRepository,
                                         CurrentUserProvider currentUserProvider,
                                         AuditService auditService) {
        this.homeClubRecommendationRepository = homeClubRecommendationRepository;
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminHomeClubRecommendationResponse> listAdmin() {
        requireAdmin();
        return buildAdminResponses(homeClubRecommendationRepository.findAllByOrderBySlotNoAsc());
    }

    @Transactional
    public List<AdminHomeClubRecommendationResponse> updateRecommendations(List<Long> clubIds) {
        AuthenticatedUser admin = requireAdmin();
        List<Long> normalizedClubIds = normalizeClubIds(clubIds);
        validateRecommendedClubs(normalizedClubIds);

        // Flush the delete before re-inserting, otherwise the unique club/slot
        // constraints can still see old rows within the same transaction.
        homeClubRecommendationRepository.deleteAllInBatch();
        homeClubRecommendationRepository.flush();
        if (!normalizedClubIds.isEmpty()) {
            List<HomeClubRecommendationEntity> entities = buildRecommendationEntities(normalizedClubIds);
            homeClubRecommendationRepository.saveAll(entities);
        }

        auditService.create(
                "ADMIN_HOME_CLUB_RECOMMENDATIONS_UPDATED",
                admin.userId(),
                "HOME_CLUB_RECOMMENDATIONS",
                null,
                "Administrator updated homepage club recommendations"
        );
        return buildAdminResponses(homeClubRecommendationRepository.findAllByOrderBySlotNoAsc());
    }

    @Transactional(readOnly = true)
    public List<PublicHomeRecommendedClubResponse> listPublic() {
        return buildPublicResponses(homeClubRecommendationRepository.findAllByOrderBySlotNoAsc());
    }

    @Transactional
    public void removeRecommendationsForClub(Long clubId) {
        homeClubRecommendationRepository.deleteAllByClubId(clubId);
    }

    @Transactional
    public void removeRecommendationsForInactiveClubs(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return;
        }
        homeClubRecommendationRepository.deleteAllByClubIdIn(clubIds);
    }

    private List<AdminHomeClubRecommendationResponse> buildAdminResponses(List<HomeClubRecommendationEntity> recommendations) {
        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<Long> clubIds = recommendations.stream().map(HomeClubRecommendationEntity::getClubId).toList();
        Map<Long, ClubEntity> clubMap = clubRepository.findAllById(clubIds)
                .stream()
                .filter(club -> club.getStatus() == ClubStatus.ACTIVE)
                .collect(Collectors.toMap(ClubEntity::getId, Function.identity()));
        Map<Long, Long> memberCounts = loadMemberCounts(clubMap.keySet());

        return recommendations.stream()
                .map(item -> {
                    ClubEntity club = clubMap.get(item.getClubId());
                    if (club == null) {
                        return null;
                    }
                    return new AdminHomeClubRecommendationResponse(
                            item.getSlotNo(),
                            club.getId(),
                            club.getName(),
                            club.getType(),
                            club.getDescription(),
                            memberCounts.getOrDefault(club.getId(), 0L)
                    );
                })
                .filter(item -> item != null)
                .toList();
    }

    private List<PublicHomeRecommendedClubResponse> buildPublicResponses(List<HomeClubRecommendationEntity> recommendations) {
        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<Long> clubIds = recommendations.stream().map(HomeClubRecommendationEntity::getClubId).toList();
        Map<Long, ClubEntity> clubMap = clubRepository.findAllById(clubIds)
                .stream()
                .filter(club -> club.getStatus() == ClubStatus.ACTIVE)
                .collect(Collectors.toMap(ClubEntity::getId, Function.identity()));
        Map<Long, Long> memberCounts = loadMemberCounts(clubMap.keySet());

        return recommendations.stream()
                .map(item -> {
                    ClubEntity club = clubMap.get(item.getClubId());
                    if (club == null) {
                        return null;
                    }
                    return new PublicHomeRecommendedClubResponse(
                            club.getId(),
                            club.getName(),
                            club.getType(),
                            club.getDescription(),
                            memberCounts.getOrDefault(club.getId(), 0L)
                    );
                })
                .filter(item -> item != null)
                .toList();
    }

    private List<HomeClubRecommendationEntity> buildRecommendationEntities(List<Long> clubIds) {
        return java.util.stream.IntStream.range(0, clubIds.size())
                .mapToObj(index -> {
                    HomeClubRecommendationEntity entity = new HomeClubRecommendationEntity();
                    entity.setClubId(clubIds.get(index));
                    entity.setSlotNo((short) (index + 1));
                    return entity;
                })
                .toList();
    }

    private void validateRecommendedClubs(List<Long> clubIds) {
        if (clubIds.isEmpty()) {
            return;
        }

        List<ClubEntity> clubs = clubRepository.findAllById(clubIds);
        if (clubs.size() != clubIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid recommended clubs");
        }

        boolean hasInactiveClub = clubs.stream().anyMatch(club -> club.getStatus() != ClubStatus.ACTIVE);
        if (hasInactiveClub) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "recommended clubs must be active");
        }
    }

    private List<Long> normalizeClubIds(List<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return List.of();
        }

        if (clubIds.size() > MAX_RECOMMENDATIONS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "too many recommended clubs");
        }

        Set<Long> normalized = new LinkedHashSet<>();
        for (Long clubId : clubIds) {
            if (clubId == null || clubId <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid club ids");
            }
            boolean added = normalized.add(clubId);
            if (!added) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "duplicate recommended clubs");
            }
        }
        return List.copyOf(normalized);
    }

    private Map<Long, Long> loadMemberCounts(Collection<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return clubStudentMemberRepository.countByClubIds(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return user;
    }
}
