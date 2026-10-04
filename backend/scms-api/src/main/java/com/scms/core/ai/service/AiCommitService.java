package com.scms.core.ai.service;

import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiCommitService {

    private static final Logger log = LoggerFactory.getLogger(AiCommitService.class);
    private static final String UNAVAILABLE_MESSAGE = "ai service unavailable";

    private final ScmsAiCompletionService completionService;
    private final CurrentUserProvider currentUserProvider;

    public AiCommitService(ScmsAiCompletionService completionService,
                           CurrentUserProvider currentUserProvider) {
        this.completionService = completionService;
        this.currentUserProvider = currentUserProvider;
    }

    public AiCommitResponse completeCommit(String commit) {
        String normalizedCommit = commit == null ? "" : commit.trim();
        if (normalizedCommit.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "commit is required");
        }
        if (!completionService.isConfigured()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        }

        AuthenticatedUser user = currentUserProvider.getRequiredUser();
        long startedAt = System.nanoTime();
        try {
            AiCommitResponse payload = completionService.complete(normalizedCommit);
            log.info(
                    "AI commit completed userId={} role={} provider={} model={} finishReason={} durationMs={}",
                    user.userId(),
                    user.role(),
                    payload.provider(),
                    payload.model(),
                    payload.finishReason(),
                    elapsedMillis(startedAt)
            );
            return payload;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "AI commit request failed userId={} role={} durationMs={}",
                    user.userId(),
                    user.role(),
                    elapsedMillis(startedAt),
                    exception
            );
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
