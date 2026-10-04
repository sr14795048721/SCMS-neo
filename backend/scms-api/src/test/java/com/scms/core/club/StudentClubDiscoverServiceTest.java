package com.scms.core.club;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubJoinRequestEntity;
import com.scms.core.club.domain.ClubJoinRequestStatus;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.StudentDiscoverClubsResponse;
import com.scms.core.club.repository.ClubJoinRequestRepository;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.club.service.ClubDutySupport;
import com.scms.core.club.service.StudentClubDiscoverService;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentClubDiscoverServiceTest {

    private ClubRepository clubRepository;
    private ClubStudentMemberRepository clubStudentMemberRepository;
    private ClubJoinRequestRepository clubJoinRequestRepository;
    private CurrentUserProvider currentUserProvider;
    private ClubDutySupport clubDutySupport;
    private StudentClubDiscoverService studentClubDiscoverService;

    @BeforeEach
    void setUp() {
        clubRepository = mock(ClubRepository.class);
        clubStudentMemberRepository = mock(ClubStudentMemberRepository.class);
        clubJoinRequestRepository = mock(ClubJoinRequestRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        clubDutySupport = mock(ClubDutySupport.class);

        studentClubDiscoverService = new StudentClubDiscoverService(
                clubRepository,
                clubStudentMemberRepository,
                clubJoinRequestRepository,
                currentUserProvider,
                clubDutySupport
        );

        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(12L, "student", UserRole.STUDENT));
        when(clubStudentMemberRepository.countByClubIds(any())).thenReturn(List.of());
    }

    @Test
    void getDiscoverClubsShouldReturnJoinedClubAndExcludeItFromCandidates() {
        ClubEntity joinedClub = club(7L, "Art Club");
        ClubEntity candidateClub = club(8L, "Music Club");

        when(clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(12L))
                .thenReturn(List.of(member(7L, 12L)));
        when(clubJoinRequestRepository.findAllByStudentUserIdAndStatusOrderByCreatedAtAscIdAsc(12L, ClubJoinRequestStatus.PENDING))
                .thenReturn(List.of());
        when(clubRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus.ACTIVE))
                .thenReturn(List.of(candidateClub, joinedClub));
        when(clubRepository.findAllById(any())).thenReturn(List.of(joinedClub, candidateClub));

        StudentDiscoverClubsResponse response = studentClubDiscoverService.getDiscoverClubs();

        assertNotNull(response.joinedClub());
        assertEquals(7L, response.joinedClub().clubId());
        assertNull(response.pendingRequest());
        assertEquals(1, response.clubs().size());
        assertEquals(8L, response.clubs().get(0).clubId());
    }

    @Test
    void getDiscoverClubsShouldReturnPendingRequestWhenPresent() {
        ClubEntity pendingClub = club(9L, "Debate Club");

        when(clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(12L))
                .thenReturn(List.of());
        when(clubJoinRequestRepository.findAllByStudentUserIdAndStatusOrderByCreatedAtAscIdAsc(12L, ClubJoinRequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest(21L, 9L, 12L)));
        when(clubRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus.ACTIVE))
                .thenReturn(List.of(pendingClub));
        when(clubRepository.findAllById(any())).thenReturn(List.of(pendingClub));

        StudentDiscoverClubsResponse response = studentClubDiscoverService.getDiscoverClubs();

        assertNull(response.joinedClub());
        assertNotNull(response.pendingRequest());
        assertEquals(9L, response.pendingRequest().clubId());
        assertEquals(0, response.clubs().size());
    }

    @Test
    void getDiscoverClubsShouldIgnoreDirtyMembershipAndPendingRequestWithoutClubId() {
        ClubEntity candidateClub = club(8L, "Music Club");

        when(clubStudentMemberRepository.findAllByStudentUserIdOrderByCreatedAtAscIdAsc(12L))
                .thenReturn(List.of(member(null, 12L)));
        when(clubJoinRequestRepository.findAllByStudentUserIdAndStatusOrderByCreatedAtAscIdAsc(12L, ClubJoinRequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest(21L, null, 12L)));
        when(clubRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus.ACTIVE))
                .thenReturn(List.of(candidateClub));
        when(clubRepository.findAllById(List.of(8L))).thenReturn(List.of(candidateClub));

        StudentDiscoverClubsResponse response = studentClubDiscoverService.getDiscoverClubs();

        assertNull(response.joinedClub());
        assertNull(response.pendingRequest());
        assertEquals(1, response.clubs().size());
        assertEquals(8L, response.clubs().get(0).clubId());
        assertTrue(response.clubs().stream().noneMatch(item -> item.clubId() == null));
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

    private ClubStudentMemberEntity member(Long clubId, Long studentUserId) {
        ClubStudentMemberEntity entity = new ClubStudentMemberEntity();
        entity.setClubId(clubId);
        entity.setStudentUserId(studentUserId);
        return entity;
    }

    private ClubJoinRequestEntity pendingRequest(Long id, Long clubId, Long studentUserId) {
        ClubJoinRequestEntity entity = new ClubJoinRequestEntity();
        setJoinRequestId(entity, id);
        entity.setClubId(clubId);
        entity.setStudentUserId(studentUserId);
        entity.setReason("apply");
        entity.setStatus(ClubJoinRequestStatus.PENDING);
        setJoinRequestTimestamps(entity);
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

    private void setJoinRequestId(ClubJoinRequestEntity entity, Long id) {
        try {
            var field = ClubJoinRequestEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void setJoinRequestTimestamps(ClubJoinRequestEntity entity) {
        try {
            var createdAt = ClubJoinRequestEntity.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(entity, Instant.now());
            var updatedAt = ClubJoinRequestEntity.class.getDeclaredField("updatedAt");
            updatedAt.setAccessible(true);
            updatedAt.set(entity, Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
