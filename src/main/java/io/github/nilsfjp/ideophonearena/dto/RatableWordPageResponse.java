package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;

public class RatableWordPageResponse {

    private List<RatableWordResponse> entries;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public RatableWordPageResponse(List<RatableWordResponse> entries, int page, int size, long totalElements,
            int totalPages) {
        this.entries = entries;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<RatableWordResponse> getEntries() {
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
