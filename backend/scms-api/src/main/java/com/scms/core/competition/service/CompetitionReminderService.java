package com.scms.core.competition.service;

import com.scms.core.competition.domain.CompetitionEntity;
import com.scms.core.competition.domain.CompetitionEntity.CompetitionStatus;
import com.scms.core.competition.repository.CompetitionRepository;
import com.scms.core.notification.service.NotificationService;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Sends in-app notifications to all admins when an upcoming competition's
 * registration deadline or start date is approaching.
 */
@Service
public class CompetitionReminderService {

    private static final int REMIND_DAYS_BEFORE = 7;
    private static final String COMPETITIONS_TARGET_PATH = "/sys-admin/competitions";

    private final CompetitionRepository competitionRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public CompetitionReminderService(CompetitionRepository competitionRepository,
                                      NotificationService notificationService,
                                      UserRepository userRepository) {
        this.competitionRepository = competitionRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Scans upcoming competitions and notifies admins for due reminders.
     * Returns how many reminders were sent.
     */
    @Transactional
    public int runReminderScan() {
        Instant now = Instant.now();
        Instant horizon = now.plus(REMIND_DAYS_BEFORE, ChronoUnit.DAYS);
        List<CompetitionEntity> upcoming = competitionRepository.findAllByStatus(CompetitionStatus.UPCOMING);
        List<Long> adminIds = userRepository.findAllSystemAdmins().stream()
                .map(UserEntity::getId)
                .toList();
        if (adminIds.isEmpty()) {
            return 0;
        }

        int notified = 0;
        for (CompetitionEntity competition : upcoming) {
            boolean deadlineDue = isWithinWindow(competition.getApplyDeadline(), now, horizon)
                    && competition.getLastDeadlineNotifiedAt() == null;
            if (deadlineDue) {
                notifyAdmins(adminIds, "比赛报名即将截止：" + competition.getName(),
                        "「" + competition.getName() + "」报名将于 " + formatDate(competition.getApplyDeadline()) + " 截止，请尽快确认参赛安排。");
                competition.setLastDeadlineNotifiedAt(now);
                notified += 1;
            }

            boolean startDue = isWithinWindow(competition.getStartDate(), now, horizon)
                    && competition.getLastStartNotifiedAt() == null;
            if (startDue) {
                notifyAdmins(adminIds, "比赛即将开始：" + competition.getName(),
                        "「" + competition.getName() + "」将于 " + formatDate(competition.getStartDate()) + " 开始，请做好赛前准备。");
                competition.setLastStartNotifiedAt(now);
                notified += 1;
            }
        }

        if (notified > 0) {
            competitionRepository.saveAll(upcoming);
        }
        return notified;
    }

    private boolean isWithinWindow(Instant value, Instant now, Instant horizon) {
        return value != null && !value.isBefore(now) && !value.isAfter(horizon);
    }

    private void notifyAdmins(List<Long> adminIds, String title, String content) {
        for (Long adminId : adminIds) {
            notificationService.create(adminId, "COMPETITION", title, content, COMPETITIONS_TARGET_PATH);
        }
    }

    private String formatDate(Instant value) {
        return value == null ? "待定" : value.toString().substring(0, 10);
    }
}
