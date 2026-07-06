package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Presentation;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresentationRepository extends JpaRepository<Presentation, Long> {

    // The scripted appearances of the given words in one condition -- the
    // display_form/script_code the CHOOSING serving path renders verbatim.
    List<Presentation> findByWordIdInAndConditionName(Collection<Long> wordIds, ConditionName conditionName);
}
