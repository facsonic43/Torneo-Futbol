package control;

import model.match.Goal;
import model.match.Match;
import model.participant.Player;
import model.participant.Position;
import model.participant.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simula goles, selecciona al autor y al asistente y registra penales, goles en contra
 * y el arquero que recibió el gol. Comparte el generador aleatorio con el simulador del partido.
 */
class GoalEventSimulator {
    private Random random;

    GoalEventSimulator(Random random) {
        this.random = random;
    }

    void simulateGoal(
            int minute,
            Team scoringTeam,
            List<Player> attackingPitch,
            List<Player> defendingPitch,
            Player goalkeeperConceded,
            Match match,
            boolean homeGoal) {
        boolean ownGoal = random.nextDouble() < 0.03;
        boolean penalty = !ownGoal && random.nextDouble() < 0.15;
        Player scorer;
        Player assist = null;

        if (ownGoal) {
            scorer = pickOwnGoalPlayer(defendingPitch);
        } else {
            scorer = pickScorer(attackingPitch);
            scorer.addGoal();

            if (!penalty) {
                assist = pickAssister(attackingPitch, scorer);

                if (assist != null) {
                    assist.addAssist();
                }
            }
        }

        if (goalkeeperConceded != null) {
            goalkeeperConceded.addGoalConceded();
        }

        if (homeGoal) {
            match.setHomeGoals(match.getHomeGoals() + 1);
        } else {
            match.setAwayGoals(match.getAwayGoals() + 1);
        }

        match.addEvent(new Goal(minute, scoringTeam, scorer, assist, penalty, ownGoal, goalkeeperConceded));
    }

    private Player pickScorer(List<Player> pitch) {
        List<Player> weightedList = new ArrayList<>();

        for (Player player : pitch) {
            int previousGoalsWeight = 1 + player.getGoals() * 3;
            int positionWeight;

            if (player.getPosition() == Position.FORWARD) {
                positionWeight = 5;
            } else if (player.getPosition() == Position.MIDFIELDER) {
                positionWeight = 2;
            } else {
                positionWeight = 1;
            }

            for (int i = 0; i < positionWeight * previousGoalsWeight; i++) {
                weightedList.add(player);
            }
        }

        if (weightedList.isEmpty()) {
            return pitch.get(0);
        }

        return weightedList.get(random.nextInt(weightedList.size()));
    }

    private Player pickAssister(List<Player> pitch, Player scorer) {
        if (pitch.size() < 2) {
            return null;
        }

        List<Player> candidates = new ArrayList<>();

        for (Player player : pitch) {
            if (player != scorer && player.getPosition() != Position.GOALKEEPER) {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (random.nextDouble() < 0.68) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        return null;
    }

    private Player pickOwnGoalPlayer(List<Player> defendingPitch) {
        List<Player> candidates = new ArrayList<>();

        for (Player player : defendingPitch) {
            if (player.getPosition() != Position.GOALKEEPER) {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty()) {
            return defendingPitch.get(random.nextInt(defendingPitch.size()));
        }

        return candidates.get(random.nextInt(candidates.size()));
    }
}
