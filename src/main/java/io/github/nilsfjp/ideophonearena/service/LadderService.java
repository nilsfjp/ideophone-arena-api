package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.LadderFloorResponse;
import io.github.nilsfjp.ideophonearena.dto.LadderFloorsResponse;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.LadderMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.LadderFloorScoreProjection;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Builds the Perception Ladder overview: floors in climb/hierarchy order, each with its
// easy->hard pairs and the caller's own progress. Floor PRESENCE is data-driven -- only
// floors whose map codes have served trials appear -- so the array shape stays correct as
// floors come online. Sequential unlock is not enforced here (28B states it in words);
// replay is allowed (a new session per floor, best score stands).
@Service
public class LadderService {

    private final LadderFloors ladderFloors;
    private final TrialRepository trialRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final AppUserRepository appUserRepository;
    private final LadderMapper ladderMapper;

    public LadderService(LadderFloors ladderFloors, TrialRepository trialRepository,
            PlayerAnswerRepository playerAnswerRepository, AppUserRepository appUserRepository,
            LadderMapper ladderMapper) {
        this.ladderFloors = ladderFloors;
        this.trialRepository = trialRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.appUserRepository = appUserRepository;
        this.ladderMapper = ladderMapper;
    }

    @Transactional(readOnly = true)
    public LadderFloorsResponse getFloors(UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        Set<String> servedCodes = servedLadderPairCodes();
        Map<Modality, BestScore> bestByFloor = bestScoresByFloor(user.getId());

        List<LadderFloorResponse> floors = new ArrayList<>();
        for (LadderFloors.Floor floor : ladderFloors.hierarchy()) {
            List<String> presentCodes = floor.getPairCodes().stream()
                    .filter(servedCodes::contains)
                    .toList();
            if (presentCodes.isEmpty()) {
                continue;
            }
            BestScore best = bestByFloor.get(floor.getModality());
            boolean cleared = best != null;
            Integer bestCorrect = cleared ? best.correct() : null;
            Integer bestAnswered = cleared ? best.answered() : null;
            floors.add(ladderMapper.toFloorResponse(floor.getModality(), presentCodes, cleared,
                    bestCorrect, bestAnswered));
        }
        return ladderMapper.toFloorsResponse(floors);
    }

    // The ladder pair codes that currently have a served (non-practice) trial.
    private Set<String> servedLadderPairCodes() {
        List<String> allLadderCodes = ladderFloors.hierarchy().stream()
                .flatMap(floor -> floor.getPairCodes().stream())
                .toList();
        Set<String> served = new HashSet<>();
        for (Trial trial : trialRepository.findScoredTrialsByPairCodes(allLadderCodes)) {
            served.add(trial.getPairing().getPairCode());
        }
        return served;
    }

    // The caller's best completed session per floor: highest correct (answered tiebreak).
    // Every completed floor session answers the same number of pairs, so this is just the
    // top score; "cleared" is simply having any completed session for the floor.
    private Map<Modality, BestScore> bestScoresByFloor(Long userId) {
        Map<Modality, BestScore> best = new EnumMap<>(Modality.class);
        for (LadderFloorScoreProjection row : playerAnswerRepository.findCompletedLadderSessionScores(userId)) {
            Modality floor = row.getFloor();
            if (floor == null) {
                continue;
            }
            int correct = row.getCorrect() == null ? 0 : row.getCorrect().intValue();
            int answered = row.getAnswered() == null ? 0 : row.getAnswered().intValue();
            BestScore current = best.get(floor);
            if (current == null || correct > current.correct()
                    || (correct == current.correct() && answered > current.answered())) {
                best.put(floor, new BestScore(correct, answered));
            }
        }
        return best;
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private record BestScore(int correct, int answered) {
    }
}
