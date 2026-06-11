package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.AdminConditionStatsResponse;
import io.github.nilsfjp.ideophonearena.dto.AdminModalityStatsResponse;
import io.github.nilsfjp.ideophonearena.dto.AdminStatsResponse;
import io.github.nilsfjp.ideophonearena.dto.AdminTotalsResponse;
import io.github.nilsfjp.ideophonearena.mapper.AdminStatsMapper;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.ConditionAnswerStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.ConditionSessionCountProjection;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatsService {

    private final AppUserRepository appUserRepository;
    private final GameSessionRepository gameSessionRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final AdminStatsMapper adminStatsMapper;

    public AdminStatsService(AppUserRepository appUserRepository, GameSessionRepository gameSessionRepository,
            PlayerAnswerRepository playerAnswerRepository, AdminStatsMapper adminStatsMapper) {
        this.appUserRepository = appUserRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.adminStatsMapper = adminStatsMapper;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        AdminTotalsResponse totals = adminStatsMapper.toTotalsResponse(
                appUserRepository.count(),
                gameSessionRepository.count(),
                gameSessionRepository.countByCompletedAtNotNull(),
                playerAnswerRepository.count());

        List<AdminModalityStatsResponse> byModality = playerAnswerRepository.aggregateAnswersByModality()
                .stream()
                .map(adminStatsMapper::toModalityStatsResponse)
                .toList();

        return new AdminStatsResponse(totals, mergeConditionStats(), byModality);
    }

    // A condition can have sessions without answers (or vice versa once data is
    // pruned), so the two aggregates are merged on the condition key with zeros
    // for the missing side.
    private List<AdminConditionStatsResponse> mergeConditionStats() {
        Map<ConditionName, Long> sessionsByCondition = new LinkedHashMap<>();
        for (ConditionSessionCountProjection row : gameSessionRepository.countSessionsByCondition()) {
            sessionsByCondition.put(row.getConditionName(), valueOrZero(row.getSessions()));
        }

        Map<ConditionName, ConditionAnswerStatsProjection> answersByCondition = new LinkedHashMap<>();
        for (ConditionAnswerStatsProjection row : playerAnswerRepository.aggregateAnswersByCondition()) {
            answersByCondition.put(row.getConditionName(), row);
        }

        List<AdminConditionStatsResponse> merged = new ArrayList<>();
        for (ConditionName conditionName : ConditionName.values()) {
            long sessions = sessionsByCondition.getOrDefault(conditionName, 0L);
            ConditionAnswerStatsProjection answerStats = answersByCondition.get(conditionName);
            long answers = answerStats == null ? 0L : valueOrZero(answerStats.getAnswers());
            long correct = answerStats == null ? 0L : valueOrZero(answerStats.getCorrect());
            if (sessions == 0 && answers == 0) {
                continue;
            }
            merged.add(adminStatsMapper.toConditionStatsResponse(conditionName, sessions, answers, correct));
        }
        return merged;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
