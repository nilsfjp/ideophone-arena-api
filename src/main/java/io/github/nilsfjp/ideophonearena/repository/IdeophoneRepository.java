package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeophoneRepository extends JpaRepository<Ideophone, Long> {

    Optional<Ideophone> findByStimulusFile(String stimulusFile);

    List<Ideophone> findByModality(Modality modality);
}
