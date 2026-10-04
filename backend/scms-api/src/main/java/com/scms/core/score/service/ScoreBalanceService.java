package com.scms.core.score.service;

import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.reward.repository.RewardOrderRepository;
import com.scms.core.score.repository.ScoreRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoreBalanceService {

    private final ScoreRecordRepository scoreRecordRepository;
    private final RewardOrderRepository rewardOrderRepository;

    public ScoreBalanceService(ScoreRecordRepository scoreRecordRepository,
                               RewardOrderRepository rewardOrderRepository) {
        this.scoreRecordRepository = scoreRecordRepository;
        this.rewardOrderRepository = rewardOrderRepository;
    }

    public long getTotalScore(Long userId) {
        return normalize(scoreRecordRepository.sumScoreByUserId(userId));
    }

    public long getRedeemedScore(Long userId) {
        return normalize(rewardOrderRepository.sumScoreCostByUserIdAndStatusIn(
                userId,
                List.of(RewardOrderStatus.PENDING, RewardOrderStatus.COMPLETED)
        ));
    }

    public long getBalance(Long userId) {
        return getTotalScore(userId) - getRedeemedScore(userId);
    }

    private long normalize(Long value) {
        return value == null ? 0L : value;
    }
}
