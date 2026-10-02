package com.scms.core.clubappworkspace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ClubAppWorkspaceProjectDemoBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClubAppWorkspaceProjectDemoBootstrapService.class);

    private final ClubAppWorkspaceProjectDemoService projectDemoService;

    public ClubAppWorkspaceProjectDemoBootstrapService(ClubAppWorkspaceProjectDemoService projectDemoService) {
        this.projectDemoService = projectDemoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        try {
            projectDemoService.ensureDefaultCyclingGuardianDemo();
        } catch (Exception exception) {
            LOGGER.warn("bootstrap cycling guardian project demo failed", exception);
        }
    }
}
