package com.scms.core.homeclubrecommend;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.homeclubrecommend.repository.TeacherClubRecommendationRepository;
import com.scms.core.homeclubrecommend.service.TeacherClubRecommendationService;
import com.scms.core.manager.repository.ManagerInfoRepository;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherClubRecommendationServiceTest {

    private TeacherClubRecommendationRepository teacherClubRecommendationRepository;
    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private UserRepository userRepository;
    private ManagerInfoRepository managerInfoRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private TeacherClubRecommendationService teacherClubRecommendationService;

    @BeforeEach
    void setUp() {
        teacherClubRecommendationRepository = mock(TeacherClubRecommendationRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        userRepository = mock(UserRepository.class);
        managerInfoRepository = mock(ManagerInfoRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);
        teacherClubRecommendationService = new TeacherClubRecommendationService(
                teacherClubRecommendationRepository,
                clubRepository,
                clubStudentMemberRepository,
                userRepository,
                managerInfoRepository,
                currentUserProvider,
                auditService
        );
    }

    @Test
    void updateManagerRecommendationsShouldRejectDuplicateClubs() {
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(8L, "manager", UserRole.CLUB_MANAGER));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> teacherClubRecommendationService.updateManagerRecommendations(List.of(2L, 2L))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void updateManagerRecommendationsShouldReturnOrderedSelections() {
        ClubEntity first = club(2L, "Art Club");
        ClubEntity second = club(3L, "Music Club");
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(8L, "manager", UserRole.CLUB_MANAGER));
        when(clubRepository.findAllById(any())).thenReturn(List.of(first, second));
        when(teacherClubRecommendationRepository.findAllByManagerUserIdOrderByOrderNoAscIdAsc(8L)).thenReturn(List.of(
                recommendation(8L, 2L, (short) 1),
                recommendation(8L, 3L, (short) 2)
        ));

        var responses = teacherClubRecommendationService.updateManagerRecommendations(List.of(2L, 3L));

        assertEquals(2, responses.size());
        assertEquals(1, responses.get(0).orderNo());
        assertEquals("Art Club", responses.get(0).clubName());
        assertEquals(2, responses.get(1).orderNo());
        verify(teacherClubRecommendationRepository).deleteAllByManagerUserId(eq(8L));
        verify(teacherClubRecommendationRepository).flush();
    }

    private ClubEntity club(Long id, String name) {
        ClubEntity entity = new ClubEntity();
        setClubId(entity, id);
        entity.setName(name);
        entity.setType("Culture");
        entity.setDescription("desc");
        entity.setStatus(ClubStatus.ACTIVE);
        entity.setCreatedBy(1L);
        return entity;
    }

    private com.scms.core.homeclubrecommend.domain.TeacherClubRecommendationEntity recommendation(Long managerId, Long clubId, short orderNo) {
        com.scms.core.homeclubrecommend.domain.TeacherClubRecommendationEntity entity =
                new com.scms.core.homeclubrecommend.domain.TeacherClubRecommendationEntity();
        entity.setManagerUserId(managerId);
        entity.setClubId(clubId);
        entity.setOrderNo(orderNo);
        return entity;
    }

    private void setClubId(ClubEntity entity, Long id) {
        try {
            var field = ClubEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
