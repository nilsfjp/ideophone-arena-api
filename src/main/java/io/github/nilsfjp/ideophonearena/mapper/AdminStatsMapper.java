package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.AdminConditionStatsResponse;
import io.github.nilsfjp.ideophonearena.dto.AdminModalityStatsResponse;
import io.github.nilsfjp.ideophonearena.dto.AdminTotalsResponse;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.ModalityAnswerStatsProjection;
import org.springframework.stereotype.Component;

@Component
public class AdminStatsMapper {

    public AdminTotalsResponse toTotalsResponse(long users, long sessions, long completedSessions, long answers) {
        return new AdminTotalsResponse(users, sessions, completedSessions, answers);
    }

    public AdminConditionStatsResponse toConditionStatsResponse(ConditionName conditionName, long sessions,
            long answers, long correct) {
        return new AdminConditionStatsResponse(conditionName.name(), sessions, answers, correct,
                accuracy(answers, correct));
    }

    public AdminModalityStatsResponse toModalityStatsResponse(ModalityAnswerStatsProjection projection) {
        long answers = valueOrZero(projection.getAnswers());
        long correct = valueOrZero(projection.getCorrect());
        return new AdminModalityStatsResponse(projection.getModality().name(), answers, correct,
                accuracy(answers, correct));
    }

    private double accuracy(long answers, long correct) {
        return answers == 0 ? 0.0 : (double) correct / answers;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
