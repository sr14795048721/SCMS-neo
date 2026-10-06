package com.scms.core.competition;

import com.scms.core.competition.domain.CompetitionEntity;
import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;
import com.scms.core.competition.repository.CompetitionRepository;
import com.scms.core.competition.service.CompetitionReminderService;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitionReminderServiceTest {

    private CompetitionRepository competitionRepository;
    private NotificationService notificationService;
    private UserRepository userRepository;
    private CompetitionReminderService service;

    @BeforeEach
    void setUp() {
        competitionRepository = mock(CompetitionRepository.class);
        notificationService = mock(NotificationService.class);
        userRepository = mock(UserRepository.class);
        service = new CompetitionReminderService(competitionRepository, notificationService, userRepository);
    }

    @Test
    void shouldNotifyAdminsWhenDeadlineWithinWindow() {
        CompetitionEntity competition = competitionEntity(1L, Instant.now().plusSeconds(2 * 24 * 3600));
        when(competitionRepository.findAllByStatus(CompetitionStatus.UPCOMING)).thenReturn(List.of(competition));
        when(userRepository.findAllSystemAdmins()).thenReturn(List.of(admin(10L), admin(11L)));

        int notified = service.runReminderScan();

        assertEquals(1, notified);
        verify(notificationService).create(eq(10L), eq("COMPETITION"), anyString(), anyString(), anyString());
        verify(notificationService).create(eq(11L), eq("COMPETITION"), anyString(), anyString(), anyString());
        verify(competitionRepository).saveAll(List.of(competition));
    }

    @Test
    void shouldNotNotifyWhenDeadlineFarInFuture() {
        CompetitionEntity competition = competitionEntity(1L, Instant.now().plusSeconds(30 * 24 * 3600));
        when(competitionRepository.findAllByStatus(CompetitionStatus.UPCOMING)).thenReturn(List.of(competition));
        when(userRepository.findAllSystemAdmins()).thenReturn(List.of(admin(10L)));

        int notified = service.runReminderScan();

        assertEquals(0, notified);
        verify(notificationService, never()).create(eq(10L), eq("COMPETITION"), anyString(), anyString(), anyString());
        verify(competitionRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldNotNotifyAgainAfterDeadlineReminderSent() {
        CompetitionEntity competition = competitionEntity(1L, Instant.now().plusSeconds(2 * 24 * 3600));
        competition.setLastDeadlineNotifiedAt(Instant.now());
        when(competitionRepository.findAllByStatus(CompetitionStatus.UPCOMING)).thenReturn(List.of(competition));
        when(userRepository.findAllSystemAdmins()).thenReturn(List.of(admin(10L)));

        int notified = service.runReminderScan();

        assertEquals(0, notified);
        verify(notificationService, never()).create(eq(10L), eq("COMPETITION"), anyString(), anyString(), anyString());
    }

    private CompetitionEntity competitionEntity(Long id, Instant applyDeadline) {
        CompetitionEntity entity = new CompetitionEntity();
        setId(entity, id);
        entity.setName("青少年科技创新大赛");
        entity.setApplyDeadline(applyDeadline);
        entity.setStartDate(Instant.now().plusSeconds(40 * 24 * 3600));
        return entity;
    }

    private UserEntity admin(Long id) {
        UserEntity entity = new UserEntity();
        setId(entity, id);
        return entity;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
