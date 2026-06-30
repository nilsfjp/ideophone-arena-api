package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.RatingPageResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingResponse;
import io.github.nilsfjp.ideophonearena.model.Rating;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getIdeophone().getId(),
                rating.getRating(),
                rating.getResponseTimeMs(),
                rating.getRatedAt()
        );
    }

    public RatingPageResponse toPageResponse(Page<Rating> page) {
        List<RatingResponse> entries = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new RatingPageResponse(entries, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
