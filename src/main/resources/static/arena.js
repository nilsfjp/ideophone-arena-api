const TOKEN_KEY = "ideophone-arena-token";

const state = {
  authMode: "register",
  token: localStorage.getItem(TOKEN_KEY),
  username: localStorage.getItem("ideophone-arena-username") || "",
  sessionUuid: "",
  round: null,
  phase: "signed-out",
  choiceStartedAt: 0,
  sequenceId: 0,
  playbackCleanup: null,
};

const $ = (id) => document.getElementById(id);

const nodes = {
  sessionMeta: $("session-meta"),
  logoutButton: $("logout-button"),
  authPanel: $("auth-panel"),
  authForm: $("auth-form"),
  registerTab: $("register-tab"),
  loginTab: $("login-tab"),
  emailField: $("email-field"),
  emailInput: $("email-input"),
  usernameInput: $("username-input"),
  passwordInput: $("password-input"),
  authSubmit: $("auth-submit"),
  startPanel: $("start-panel"),
  conditionSelect: $("condition-select"),
  difficultyInput: $("difficulty-input"),
  startButton: $("start-button"),
  trialPanel: $("trial-panel"),
  fixationStage: $("fixation-stage"),
  roundStage: $("round-stage"),
  targetTranslation: $("target-translation"),
  otherTranslation: $("other-translation"),
  choiceTarget: $("choice-target"),
  leftCard: $("left-card"),
  rightCard: $("right-card"),
  leftMedia: $("left-media"),
  rightMedia: $("right-media"),
  leftPlay: $("left-play"),
  rightPlay: $("right-play"),
  leftKana: $("left-kana"),
  rightKana: $("right-kana"),
  leftRomaji: $("left-romaji"),
  rightRomaji: $("right-romaji"),
  choiceStage: $("choice-stage"),
  leftChoice: $("left-choice"),
  rightChoice: $("right-choice"),
  feedbackPanel: $("feedback-panel"),
  feedbackTitle: $("feedback-title"),
  selectedKana: $("selected-kana"),
  correctKana: $("correct-kana"),
  scoreLine: $("score-line"),
  nextButton: $("next-button"),
  statusLine: $("status-line"),
};

function setStatus(message) {
  nodes.statusLine.textContent = message || "";
}

function show(node, visible) {
  node.classList.toggle("hidden", !visible);
}

function setAuthMode(mode) {
  state.authMode = mode;
  nodes.registerTab.classList.toggle("active", mode === "register");
  nodes.loginTab.classList.toggle("active", mode === "login");
  show(nodes.emailField, mode === "register");
  nodes.emailInput.required = mode === "register";
  nodes.authSubmit.textContent = mode === "register" ? "Register" : "Login";
}

function renderShell() {
  const signedIn = Boolean(state.token);
  nodes.sessionMeta.textContent = signedIn
    ? `${state.username || "Player"}${state.sessionUuid ? ` · ${state.sessionUuid}` : ""}`
    : "Signed out";
  show(nodes.logoutButton, signedIn);
  show(nodes.authPanel, !signedIn);
  show(nodes.startPanel, signedIn && !state.sessionUuid);
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (state.token) {
    headers.set("Authorization", `Bearer ${state.token}`);
  }

  const response = await fetch(path, {
    ...options,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = { message: text };
    }
  }
  if (!response.ok) {
    throw new Error(body?.message || response.statusText || "Request failed");
  }
  return body;
}

async function handleAuth(event) {
  event.preventDefault();
  setStatus("");
  const payload = {
    username: nodes.usernameInput.value.trim(),
    password: nodes.passwordInput.value,
  };
  if (state.authMode === "register") {
    payload.email = nodes.emailInput.value.trim();
  }

  try {
    const response = await api(`/api/auth/${state.authMode}`, {
      method: "POST",
      body: payload,
    });
    state.token = response.token;
    state.username = response.username || payload.username;
    localStorage.setItem(TOKEN_KEY, state.token);
    localStorage.setItem("ideophone-arena-username", state.username);
    renderShell();
  } catch (error) {
    setStatus(error.message);
  }
}

async function startSession() {
  nodes.startButton.disabled = true;
  setStatus("Starting session...");
  try {
    cancelTrialSequence();
    const session = await api("/api/game/sessions", {
      method: "POST",
      body: {
        conditionName: nodes.conditionSelect.value,
        difficultyLevel: Number(nodes.difficultyInput.value || 1),
      },
    });
    state.sessionUuid = session.sessionUuid;
    renderShell();
    await loadNextRound();
  } catch (error) {
    setStatus(error.message);
  } finally {
    nodes.startButton.disabled = false;
  }
}

async function loadNextRound() {
  const sequenceId = beginTrialSequence();
  setStatus("Loading round...");
  show(nodes.feedbackPanel, false);
  show(nodes.trialPanel, true);
  try {
    state.round = await api(`/api/game/sessions/${encodeURIComponent(state.sessionUuid)}/rounds/next`);
    if (!isCurrentSequence(sequenceId)) {
      return;
    }
    if (state.round.completed) {
      const message = state.round.message || "Session complete.";
      state.round = null;
      state.sessionUuid = "";
      show(nodes.trialPanel, false);
      show(nodes.feedbackPanel, false);
      renderShell();
      setStatus(message);
      return;
    }
    renderRound();
    await runTrialSequence(sequenceId);
  } catch (error) {
    if (!isCurrentSequence(sequenceId)) {
      return;
    }
    state.round = null;
    show(nodes.trialPanel, false);
    if (/complete/i.test(error.message)) {
      state.sessionUuid = "";
      renderShell();
      setStatus("Session complete.");
      return;
    }
    setStatus(error.message);
  }
}

function beginTrialSequence() {
  cancelCurrentPlayback();
  state.sequenceId += 1;
  return state.sequenceId;
}

function cancelTrialSequence() {
  state.sequenceId += 1;
  state.phase = "idle";
  state.round = null;
  cancelCurrentPlayback();
  show(nodes.leftPlay, false);
  show(nodes.rightPlay, false);
  pauseMedia(nodes.leftMedia);
  pauseMedia(nodes.rightMedia);
}

function isCurrentSequence(sequenceId) {
  return state.sequenceId === sequenceId && Boolean(state.round);
}

function cancelCurrentPlayback() {
  if (state.playbackCleanup) {
    state.playbackCleanup();
    state.playbackCleanup = null;
  }
}

function pauseMedia(media) {
  media.pause();
  media.removeAttribute("src");
  media.load();
}

function renderRound() {
  const round = state.round;
  const target = round.targetTranslation || round.translations?.target || round.prompt;
  nodes.targetTranslation.textContent = target;
  nodes.otherTranslation.textContent = round.translations?.other || "";
  nodes.choiceTarget.textContent = target;
  renderOption("left", round.left);
  renderOption("right", round.right);
  nodes.leftChoice.textContent = `${round.left.kana}${round.left.romaji ? ` · ${round.left.romaji}` : ""}`;
  nodes.rightChoice.textContent = `${round.right.kana}${round.right.romaji ? ` · ${round.right.romaji}` : ""}`;
}

function renderOption(side, option) {
  nodes[`${side}Kana`].textContent = option.kana;
  nodes[`${side}Romaji`].textContent = option.romaji || "";
  const media = nodes[`${side}Media`];
  show(nodes[`${side}Play`], false);
  media.src = option.stimulusUrl || (option.stimulusFile ? `/stimuli/${option.stimulusFile}` : "");
}

async function runTrialSequence(sequenceId) {
  state.phase = "fixation";
  show(nodes.fixationStage, true);
  show(nodes.roundStage, false);
  show(nodes.choiceStage, false);
  setCardVisibility(false, false);
  setStatus("");

  await sleep(state.round.timing?.fixationMs ?? 800);
  if (!isCurrentSequence(sequenceId)) {
    return;
  }
  show(nodes.fixationStage, false);
  show(nodes.roundStage, true);

  state.phase = "left-playing";
  setCardVisibility(true, false);
  await playStimulus(nodes.leftMedia, nodes.leftPlay, sequenceId);
  if (!isCurrentSequence(sequenceId)) {
    return;
  }

  state.phase = "right-playing";
  setCardVisibility(false, true);
  await playStimulus(nodes.rightMedia, nodes.rightPlay, sequenceId);
  if (!isCurrentSequence(sequenceId)) {
    return;
  }

  await sleep(state.round.timing?.preChoiceDelayMs ?? 0);
  if (!isCurrentSequence(sequenceId)) {
    return;
  }
  state.phase = "choice";
  state.choiceStartedAt = performance.now();
  setCardVisibility(true, true);
  show(nodes.choiceStage, true);
}

function setCardVisibility(leftVisible, rightVisible) {
  nodes.leftCard.classList.toggle("inactive", !leftVisible);
  nodes.rightCard.classList.toggle("inactive", !rightVisible);
}

function playStimulus(media, playButton, sequenceId) {
  return new Promise((resolve) => {
    let done = false;
    let fallbackTimer = 0;
    const finish = () => {
      if (done) {
        return;
      }
      done = true;
      if (fallbackTimer) {
        window.clearTimeout(fallbackTimer);
      }
      media.removeEventListener("ended", finish);
      media.removeEventListener("error", fallback);
      show(playButton, false);
      playButton.onclick = null;
      if (state.playbackCleanup === cleanup) {
        state.playbackCleanup = null;
      }
      resolve();
    };
    const fallback = () => {
      if (fallbackTimer) {
        window.clearTimeout(fallbackTimer);
      }
      fallbackTimer = window.setTimeout(finish, 900);
    };
    const waitForManualPlay = () => {
      if (!isCurrentSequence(sequenceId)) {
        finish();
        return;
      }
      setStatus("Click Play to continue.");
      show(playButton, true);
      playButton.onclick = async () => {
        show(playButton, false);
        setStatus("");
        try {
          media.currentTime = 0;
          await media.play();
        } catch {
          fallback();
        }
      };
    };
    const cleanup = () => {
      if (done) {
        return;
      }
      media.removeEventListener("ended", finish);
      media.removeEventListener("error", fallback);
      if (fallbackTimer) {
        window.clearTimeout(fallbackTimer);
      }
      playButton.onclick = null;
      show(playButton, false);
      media.pause();
      if (state.playbackCleanup === cleanup) {
        state.playbackCleanup = null;
      }
      done = true;
      resolve();
    };
    state.playbackCleanup = cleanup;
    media.addEventListener("ended", finish, { once: true });
    media.addEventListener("error", fallback, { once: true });
    if (!isCurrentSequence(sequenceId) || !media.getAttribute("src")) {
      fallback();
      return;
    }
    media.currentTime = 0;
    const playPromise = media.play();
    if (playPromise) {
      playPromise.catch(waitForManualPlay);
    }
  });
}

async function submitChoice(side) {
  if (state.phase !== "choice") {
    return;
  }
  state.phase = "submitting";
  const option = state.round[side];
  const responseTimeMs = Math.max(0, Math.round(performance.now() - state.choiceStartedAt));
  setStatus("Submitting...");
  try {
    const result = await api(`/api/game/sessions/${encodeURIComponent(state.sessionUuid)}/answers`, {
      method: "POST",
      body: {
        roundId: state.round.roundId,
        selectedIdeophoneId: option.ideophoneId,
        responseTimeMs,
      },
    });
    renderFeedback(result);
  } catch (error) {
    state.phase = "choice";
    setStatus(error.message);
  }
}

function renderFeedback(result) {
  show(nodes.trialPanel, false);
  show(nodes.feedbackPanel, true);
  nodes.feedbackTitle.textContent = result.correct ? "Correct" : "Incorrect";
  nodes.feedbackPanel.classList.toggle("correct", result.correct);
  nodes.feedbackPanel.classList.toggle("incorrect", !result.correct);
  nodes.selectedKana.textContent = result.selectedKana || "";
  nodes.correctKana.textContent = result.correctKana || "";
  const accuracy = result.totalAnswered
    ? Math.round((result.totalCorrect / result.totalAnswered) * 100)
    : 0;
  nodes.scoreLine.textContent = `${result.totalCorrect} / ${result.totalAnswered} (${accuracy}%)`;
  setStatus("");
}

function logout() {
  cancelTrialSequence();
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem("ideophone-arena-username");
  state.token = "";
  state.username = "";
  state.sessionUuid = "";
  state.round = null;
  show(nodes.trialPanel, false);
  show(nodes.feedbackPanel, false);
  renderShell();
}

function sleep(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

nodes.registerTab.addEventListener("click", () => setAuthMode("register"));
nodes.loginTab.addEventListener("click", () => setAuthMode("login"));
nodes.authForm.addEventListener("submit", handleAuth);
nodes.logoutButton.addEventListener("click", logout);
nodes.startButton.addEventListener("click", startSession);
nodes.leftChoice.addEventListener("click", () => submitChoice("left"));
nodes.rightChoice.addEventListener("click", () => submitChoice("right"));
nodes.nextButton.addEventListener("click", loadNextRound);

setAuthMode("register");
renderShell();
