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
    private boolean practice;
    private boolean targetMeaningListedFirst;
    private TranslationResponse translations;
    private IdeophoneChoiceResponse left;
    private IdeophoneChoiceResponse right;
    private TimingResponse timing;

    public RoundResponse(String sessionUuid, Long roundId, String targetTranslation, ConditionName conditionName,
            int difficultyLevel, boolean practice, boolean targetMeaningListedFirst,
            TranslationResponse translations, IdeophoneChoiceResponse left,
            IdeophoneChoiceResponse right, TimingResponse timing) {
        this(false, null, sessionUuid, roundId, targetTranslation, conditionName, difficultyLevel, practice,
                targetMeaningListedFirst, translations, left, right, timing);
    }

    public RoundResponse(boolean completed, String message, String sessionUuid, Long roundId, String targetTranslation,
            ConditionName conditionName, int difficultyLevel, boolean practice, boolean targetMeaningListedFirst,
            TranslationResponse translations, IdeophoneChoiceResponse left, IdeophoneChoiceResponse right,
            TimingResponse timing) {
        this.completed = completed;
        this.message = message;
        this.sessionUuid = sessionUuid;
        this.roundId = roundId;
        this.targetTranslation = targetTranslation;
        this.conditionName = conditionName;
        this.difficultyLevel = difficultyLevel;
        this.practice = practice;
        this.targetMeaningListedFirst = targetMeaningListedFirst;
        this.translations = translations;
        this.left = left;
        this.right = right;
        this.timing = timing;
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

    public boolean isPractice() {
        return practice;
    }

    // Seed-drawn presentation flag (see the contract's derivation section):
    // whether the target meaning is listed first of the two meaning lines.
    // False on completed-session sentinels, which carry no translations.
    public boolean isTargetMeaningListedFirst() {
        return targetMeaningListedFirst;
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
