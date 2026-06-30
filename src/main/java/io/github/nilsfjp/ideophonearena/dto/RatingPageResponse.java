package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;

public class RatingPageResponse {

    private List<RatingResponse> entries;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public RatingPageResponse(List<RatingResponse> entries, int page, int size, long totalElements,
            int totalPages) {
        this.entries = entries;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<RatingResponse> getEntries() {
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
