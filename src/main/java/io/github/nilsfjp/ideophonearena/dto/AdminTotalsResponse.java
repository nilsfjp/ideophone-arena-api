package io.github.nilsfjp.ideophonearena.dto;

public class AdminTotalsResponse {

    private long users;
    private long sessions;
    private long completedSessions;
    private long answers;

    public AdminTotalsResponse(long users, long sessions, long completedSessions, long answers) {
        this.users = users;
        this.sessions = sessions;
        this.completedSessions = completedSessions;
        this.answers = answers;
    }

    public long getUsers() {
        return users;
    }

    public long getSessions() {
        return sessions;
    }

    public long getCompletedSessions() {
        return completedSessions;
    }

    public long getAnswers() {
        return answers;
    }
}
