package com.scms.core.homeclubrecommend.service;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.homeclubrecommend.domain.TeacherClubRecommendationEntity;
import com.scms.core.homeclubrecommend.dto.TeacherClubRecommendationGroupResponse;
import com.scms.core.homeclubrecommend.dto.TeacherClubRecommendationItemResponse;
import com.scms.core.homeclubrecommend.repository.TeacherClubRecommendationRepository;
import com.scms.core.manager.domain.ManagerInfoEntity;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeacherClubRecommendationService {

    private static final int MAX_RECOMMENDATIONS = 4;

    private final TeacherClubRecommendationRepository teacherClubRecommendationRepository;
    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final UserRepository userRepository;
    private final ManagerInfoRepository managerInfoRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public TeacherClubRecommendationService(TeacherClubRecommendationRepository teacherClubRecommendationRepository,
                                            ClubRepository clubRepository,
                                            ClubStudentMemberRepository clubStudentMemberRepository,
                                            UserRepository userRepository,
                                            ManagerInfoRepository managerInfoRepository,
                                            CurrentUserProvider currentUserProvider,
                                            AuditService auditService) {
        this.teacherClubRecommendationRepository = teacherClubRecommendationRepository;
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.userRepository = userRepository;
        this.managerInfoRepository = managerInfoRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TeacherClubRecommendationItemResponse> listManagerRecommendations() {
        AuthenticatedUser manager = requireManager();
        return buildItemResponses(
                teacherClubRecommendationRepository.findAllByManagerUserIdOrderByOrderNoAscIdAsc(manager.userId())
        );
    }

    @Transactional
    public List<TeacherClubRecommendationItemResponse> updateManagerRecommendations(List<Long> clubIds) {
        AuthenticatedUser manager = requireManager();
        List<Long> normalizedClubIds = normalizeClubIds(clubIds);
        validateRecommendedClubs(normalizedClubIds);

        teacherClubRecommendationRepository.deleteAllByManagerUserId(manager.userId());
        teacherClubRecommendationRepository.flush();
        if (!normalizedClubIds.isEmpty()) {
            teacherClubRecommendationRepository.saveAll(buildEntities(manager.userId(), normalizedClubIds));
        }

        auditService.create(
                "MANAGER_CLUB_RECOMMENDATIONS_UPDATED",
                manager.userId(),
                "TEACHER_CLUB_RECOMMENDATIONS",
                String.valueOf(manager.userId()),
                "Club manager updated club recommendation references"
        );
        return listManagerRecommendations();
    }

    @Transactional(readOnly = true)
    public List<TeacherClubRecommendationGroupResponse> listTeacherSubmissionsForAdmin() {
        requireAdmin();
        List<TeacherClubRecommendationEntity> allRecommendations =
                teacherClubRecommendationRepository.findAllByOrderByManagerUserIdAscOrderNoAscIdAsc();
        if (allRecommendations.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TeacherClubRecommendationEntity>> grouped = allRecommendations.stream()
                .collect(Collectors.groupingBy(
                        TeacherClubRecommendationEntity::getManagerUserId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Long> managerUserIds = grouped.keySet().stream().toList();
        Map<Long, UserEntity> managerUserMap = userRepository.findAllByIdInAndRole(managerUserIds, UserRole.CLUB_MANAGER)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        Map<Long, ManagerInfoEntity> managerInfoMap = managerInfoRepository.findAllByUserIdIn(managerUserIds)
                .stream()
                .collect(Collectors.toMap(ManagerInfoEntity::getUserId, Function.identity()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long managerUserId = entry.getKey();
                    UserEntity user = managerUserMap.get(managerUserId);
                    if (user == null) {
                        return null;
                    }
                    ManagerInfoEntity info = managerInfoMap.get(managerUserId);
                    return new TeacherClubRecommendationGroupResponse(
                            managerUserId,
                            resolveManagerName(user, info),
                            info == null ? "" : defaultString(info.getManagerNo()),
                            buildItemResponses(entry.getValue())
                    );
                })
                .filter(item -> item != null)
                .toList();
    }

    private List<TeacherClubRecommendationEntity> buildEntities(Long managerUserId, List<Long> clubIds) {
        return java.util.stream.IntStream.range(0, clubIds.size())
                .mapToObj(index -> {
                    TeacherClubRecommendationEntity entity = new TeacherClubRecommendationEntity();
                    entity.setManagerUserId(managerUserId);
                    entity.setClubId(clubIds.get(index));
                    entity.setOrderNo((short) (index + 1));
                    entity.setRemark(null);
                    return entity;
                })
                .toList();
    }

    private List<TeacherClubRecommendationItemResponse> buildItemResponses(List<TeacherClubRecommendationEntity> recommendations) {
        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<Long> clubIds = recommendations.stream()
                .map(TeacherClubRecommendationEntity::getClubId)
                .distinct()
                .toList();
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
                    return new TeacherClubRecommendationItemResponse(
                            item.getOrderNo(),
                            club.getId(),
                            club.getName(),
                            defaultString(club.getType()),
                            defaultString(club.getDescription()),
                            memberCounts.getOrDefault(club.getId(), 0L),
                            defaultString(item.getRemark())
                    );
                })
                .filter(item -> item != null)
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
        if (clubs.stream().anyMatch(club -> club.getStatus() != ClubStatus.ACTIVE)) {
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
            if (!normalized.add(clubId)) {
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

    private String resolveManagerName(UserEntity user, ManagerInfoEntity info) {
        if (info != null && info.getDisplayName() != null && !info.getDisplayName().isBlank()) {
            return info.getDisplayName();
        }
        return defaultString(user.getUsername());
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private AuthenticatedUser requireManager() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (user.role() != UserRole.CLUB_MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "manager role required");
        }
        return user;
    }

    private AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        if (!user.role().isSystemAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role required");
        }
        return user;
    }
}
