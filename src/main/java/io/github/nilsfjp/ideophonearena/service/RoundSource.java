package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import java.util.List;

// The per-mode round-serving strategy seam (ADR-2). GameService dispatches through a
// Map<GameMode, RoundSource> built from these beans -- a strategy seam, not a framework.
// A source returns the session's ordered, shuffle-derived scored rounds; getNextRound
// scans them for the first unanswered, submitAnswer rebuilds the target from them, and the
// completion count is their size. Practice (CHOOSING-only) stays in GameService.
public interface RoundSource {

    GameMode mode();

    List<DerivedRound> scoredRounds(GameSession session);
}
