package com.scms.core.homeclubrecommend;

import com.scms.core.audit.service.AuditService;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.homeclubrecommend.domain.HomeClubRecommendationEntity;
import com.scms.core.homeclubrecommend.repository.HomeClubRecommendationRepository;
import com.scms.core.homeclubrecommend.service.HomeClubRecommendationService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeClubRecommendationServiceTest {

    private HomeClubRecommendationRepository homeClubRecommendationRepository;
    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private CurrentUserProvider currentUserProvider;
    private AuditService auditService;
    private HomeClubRecommendationService homeClubRecommendationService;

    @BeforeEach
    void setUp() {
        homeClubRecommendationRepository = mock(HomeClubRecommendationRepository.class);
        clubRepository = mock(ClubRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        auditService = mock(AuditService.class);

        homeClubRecommendationService = new HomeClubRecommendationService(
                homeClubRecommendationRepository,
                clubRepository,
                clubStudentMemberRepository,
                currentUserProvider,
                auditService
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(1L, "admin", UserRole.ADMIN));
    }

    @Test
    void updateRecommendationsShouldPersistManualOrder() {
        ClubEntity firstClub = buildClub(11L, "Art Club");
        ClubEntity secondClub = buildClub(12L, "Music Club");

        when(clubRepository.findAllById(List.of(11L, 12L))).thenReturn(List.of(firstClub, secondClub));
        when(homeClubRecommendationRepository.findAllByOrderBySlotNoAsc()).thenReturn(List.of(
                buildRecommendation(11L, (short) 1),
                buildRecommendation(12L, (short) 2)
        ));

        var response = homeClubRecommendationService.updateRecommendations(List.of(11L, 12L));

        ArgumentCaptor<List<HomeClubRecommendationEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(homeClubRecommendationRepository).deleteAllInBatch();
        verify(homeClubRecommendationRepository).flush();
        verify(homeClubRecommendationRepository).saveAll(captor.capture());
        List<HomeClubRecommendationEntity> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(11L, saved.get(0).getClubId());
        assertEquals(1, saved.get(0).getSlotNo().intValue());
        assertEquals(12L, saved.get(1).getClubId());
        assertEquals(2, saved.get(1).getSlotNo().intValue());
        assertEquals(2, response.size());
        verify(auditService).create(eq("ADMIN_HOME_CLUB_RECOMMENDATIONS_UPDATED"), eq(1L), eq("HOME_CLUB_RECOMMENDATIONS"), eq(null), any(String.class));
    }

    private ClubEntity buildClub(Long id, String name) {
        ClubEntity club = new ClubEntity();
        setClubId(club, id);
        club.setName(name);
        club.setType("Arts");
        club.setStatus(ClubStatus.ACTIVE);
        club.setDescription("desc");
        return club;
    }

    private HomeClubRecommendationEntity buildRecommendation(Long clubId, short slotNo) {
        HomeClubRecommendationEntity entity = new HomeClubRecommendationEntity();
        entity.setClubId(clubId);
        entity.setSlotNo(slotNo);
        return entity;
    }

    private void setClubId(ClubEntity club, Long id) {
        try {
            var field = ClubEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(club, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
