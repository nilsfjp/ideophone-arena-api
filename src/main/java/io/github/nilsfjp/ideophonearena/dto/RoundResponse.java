package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;

public class RoundResponse {

    private boolean completed;
    private String message;
    private String sessionUuid;
    private Long roundId;
    private String targetTranslation;
    private ConditionName conditionName;
    private int difficultyLevel;
    private TranslationResponse translations;
    private IdeophoneChoiceResponse left;
    private IdeophoneChoiceResponse right;
    private TimingResponse timing;

    public RoundResponse(String sessionUuid, Long roundId, String targetTranslation, ConditionName conditionName,
            int difficultyLevel, TranslationResponse translations, IdeophoneChoiceResponse left,
            IdeophoneChoiceResponse right, TimingResponse timing) {
        this(false, null, sessionUuid, roundId, targetTranslation, conditionName, difficultyLevel, translations, left,
                right, timing);
    }

    private RoundResponse(boolean completed, String message, String sessionUuid, Long roundId, String targetTranslation,
            ConditionName conditionName, int difficultyLevel, TranslationResponse translations,
            IdeophoneChoiceResponse left, IdeophoneChoiceResponse right, TimingResponse timing) {
        this.completed = completed;
        this.message = message;
        this.sessionUuid = sessionUuid;
        this.roundId = roundId;
        this.targetTranslation = targetTranslation;
        this.conditionName = conditionName;
        this.difficultyLevel = difficultyLevel;
        this.translations = translations;
        this.left = left;
        this.right = right;
        this.timing = timing;
    }

    public static RoundResponse completed(String sessionUuid, ConditionName conditionName, int difficultyLevel,
            String message) {
        return new RoundResponse(true, message, sessionUuid, null, null, conditionName, difficultyLevel, null, null,
                null, null);
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getMessage() {
        return message;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public Long getRoundId() {
        return roundId;
    }

    public String getTargetTranslation() {
        return targetTranslation;
    }

    public String getPrompt() {
        return targetTranslation;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public TranslationResponse getTranslations() {
        return translations;
    }

    public IdeophoneChoiceResponse getLeft() {
        return left;
    }

    public IdeophoneChoiceResponse getRight() {
        return right;
    }

    public TimingResponse getTiming() {
        return timing;
    }
}
