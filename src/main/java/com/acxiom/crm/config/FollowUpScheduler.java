package com.acxiom.crm.config;

import com.acxiom.crm.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowUpScheduler {

    private final FollowUpService followUpService;

    /**
     * Daily at 00:01, marks all past-due PLANNED follow-ups as MISSED.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void markOverdueFollowUps() {
        log.info("Running FollowUpScheduler: checking for overdue follow-ups...");
        int count = followUpService.markOverdueAsMissed();
        log.info("FollowUpScheduler completed: {} follow-ups marked MISSED.", count);
    }
}
