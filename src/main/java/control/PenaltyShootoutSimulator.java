package control;

import model.match.Match;
import model.match.PenaltyTaken;
import model.participant.Player;
import model.participant.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Resuelve una tanda de penales con los jugadores que terminaron el partido.
// Ordena los pateadores por definición, enfrenta sus habilidades a las del arquero y registra
// cada ejecución, incluida la muerte súbita necesaria para obtener un ganador.

public class PenaltyShootoutSimulator {
    private final Random random;

    public PenaltyShootoutSimulator(Random random) {
        this.random = random;
    }

    public Random getRandom() {
        return random;
    }

    void simulate(Match match, List<Player> homePitch, List<Player> awayPitch) {
        List<Player> homeTakers = prepareTakers(homePitch.isEmpty() ? match.getHomeStarters() : homePitch);
        List<Player> awayTakers = prepareTakers(awayPitch.isEmpty() ? match.getAwayStarters() : awayPitch);
        if (homeTakers.isEmpty() || awayTakers.isEmpty()) {
            throw new IllegalStateException("There are not enough players to perform the penalty shootout.");
        }
        Player homeGoalkeeper = findGoalkeeper(homeTakers);
        Player awayGoalkeeper = findGoalkeeper(awayTakers);
        int homePenalties = 0;
        int awayPenalties = 0;
        int homeTaken = 0;
        int awayTaken = 0;
        boolean finished = false;
        for (int round = 0; round < 5 && !finished; round++) {
            Player homeTaker = homeTakers.get(round % homeTakers.size());
            boolean homeScore = simulatePenalty(match, homeTaker, awayGoalkeeper, true);
            homeTaken++;
            if (homeScore) {
                homePenalties++;
            }
            if (cannotCatchUp(homePenalties, awayPenalties, 5 - awayTaken)) {
                finished = true;
            } else {
                Player awayTaker = awayTakers.get(round % awayTakers.size());
                boolean awayScore = simulatePenalty(match, awayTaker, homeGoalkeeper, false);
                awayTaken++;
                if (awayScore) {
                    awayPenalties++;
                }
                int homeRemaining = 5 - homeTaken;
                int awayRemaining = 5 - awayTaken;
                if (cannotCatchUp(homePenalties, awayPenalties, awayRemaining) || cannotCatchUp(awayPenalties,
                        homePenalties, homeRemaining)) {
                    finished = true;
                }
            }
        }
        int round = 5;
        while (!finished && homePenalties == awayPenalties) {
            Player homeTaker = homeTakers.get(round % homeTakers.size());
            Player awayTaker = awayTakers.get(round % awayTakers.size());
            boolean homeScore = simulatePenalty(match, homeTaker, awayGoalkeeper, true);
            boolean awayScore = simulatePenalty(match, awayTaker, homeGoalkeeper, false);
            if (homeScore) {
                homePenalties++;
            }
            if (awayScore) {
                awayPenalties++;
            }
            if (homeScore != awayScore) {
                finished = true;
            }
            round++;
        }
        match.setHomePenalties(homePenalties);
        match.setAwayPenalties(awayPenalties);
    }

    private List<Player> prepareTakers(List<Player> originalList) {
        List<Player> takers = new ArrayList<>(originalList);
        takers.sort((p1, p2) -> Integer.compare(p2.getFinishing(), p1.getFinishing()));
        return takers;
    }

    private boolean simulatePenalty(Match match, Player taker, Player goalkeeper, boolean homeTeam) {
        double probability = calculatePenaltyProbability(taker, goalkeeper);
        boolean scored = random.nextDouble() < probability;
        match.addEvent(new PenaltyTaken(90, homeTeam ? match.getHomeTeam() : match.getAwayTeam(), taker, scored));
        return scored;
    }

    private double calculatePenaltyProbability(Player taker, Player goalkeeper) {
        double kickerRating = taker.getFinishing();
        double goalkeeperRating = (goalkeeper != null) ? goalkeeper.getOverall() : 50.0;
        double probability = 0.75 + (kickerRating - goalkeeperRating) * 0.005;
        probability = Math.max(0.55, probability);
        probability = Math.min(0.90, probability);
        return probability;
    }

    private Player findGoalkeeper(List<Player> players) {
        for (Player player : players) {
            if (player.getPosition() == Position.GOALKEEPER) {
                return player;
            }
        }
        Player emergencyGoalkeeper = players.get(0);
        for (Player player : players) {
            if (player.getOverall() > emergencyGoalkeeper.getOverall()) {
                emergencyGoalkeeper = player;
            }
        }
        return emergencyGoalkeeper;
    }

    private boolean cannotCatchUp(int leadingScore, int otherScore, int otherRemaining) {
        return leadingScore > otherScore + otherRemaining;
    }
}
