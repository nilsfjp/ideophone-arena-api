package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;

public class LeaderboardPageResponse {

    private List<LeaderboardEntryResponse> entries;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public LeaderboardPageResponse(List<LeaderboardEntryResponse> entries, int page, int size, long totalElements,
            int totalPages) {
        this.entries = entries;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<LeaderboardEntryResponse> getEntries() {
        return entries;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
