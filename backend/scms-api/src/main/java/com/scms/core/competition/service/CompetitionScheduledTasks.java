package com.scms.core.competition.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduled jobs for competition tracking:
 * 08:00 - scan enabled official-website sources for new leads;
 * 08:30 - send approaching-deadline reminders to all admins.
 */
@Component
public class CompetitionScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(CompetitionScheduledTasks.class);

    private final CompetitionService competitionService;
    private final CompetitionReminderService reminderService;

    public CompetitionScheduledTasks(CompetitionService competitionService,
                                     CompetitionReminderService reminderService) {
        this.competitionService = competitionService;
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scanSources() {
        try {
            int created = competitionService.scanAllEnabledSources();
            if (created > 0) {
                log.info("competition scan created {} new lead(s)", created);
            }
        } catch (Exception e) {
            log.error("competition scan job failed", e);
        }
    }

    @Scheduled(cron = "0 30 8 * * *")
    public void sendReminders() {
        try {
            int sent = reminderService.runReminderScan();
            if (sent > 0) {
                log.info("competition reminder job sent {} notification(s)", sent);
            }
        } catch (Exception e) {
            log.error("competition reminder job failed", e);
        }
    }
}
