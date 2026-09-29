package control;

import model.match.Match;
import model.match.PenaltyTaken;
import model.participant.Player;

import java.util.List;
import java.util.Random;

/*
 * Se ocupa únicamente de resolver una tanda de penales.
 * Los goles de la tanda no modifican la estadística de goles
 * de los jugadores.
 */
public class PenaltyShootoutSimulator {

    private Random random;

    public PenaltyShootoutSimulator(
            Random random) {

        this.random =
                random;
    }

    public void simulate(
            Match match,
            List<Player> homePitch,
            List<Player> awayPitch) {

        List<Player> homeTakers =
                homePitch.isEmpty()
                        ? match.getHomeStarters()
                        : homePitch;

        List<Player> awayTakers =
                awayPitch.isEmpty()
                        ? match.getAwayStarters()
                        : awayPitch;

        int homePenalties = 0;
        int awayPenalties = 0;

        for (int i = 0;
             i < 5;
             i++) {

            Player homeTaker =
                    homeTakers.get(
                            i
                                    % homeTakers.size()
                    );

            Player awayTaker =
                    awayTakers.get(
                            i
                                    % awayTakers.size()
                    );

            boolean homeScore =
                    random.nextDouble()
                            < 0.75;

            boolean awayScore =
                    random.nextDouble()
                            < 0.75;

            if (homeScore) {
                homePenalties++;
            }

            if (awayScore) {
                awayPenalties++;
            }

            match.addEvent(
                    new PenaltyTaken(
                            90,
                            match.getHomeTeam(),
                            homeTaker,
                            homeScore
                    )
            );

            match.addEvent(
                    new PenaltyTaken(
                            90,
                            match.getAwayTeam(),
                            awayTaker,
                            awayScore
                    )
            );
        }

        int round = 5;

        while (homePenalties
                == awayPenalties) {

            Player homeTaker =
                    homeTakers.get(
                            round
                                    % homeTakers.size()
                    );

            Player awayTaker =
                    awayTakers.get(
                            round
                                    % awayTakers.size()
                    );

            boolean homeScore =
                    random.nextDouble()
                            < 0.75;

            boolean awayScore =
                    random.nextDouble()
                            < 0.75;

            if (homeScore) {
                homePenalties++;
            }

            if (awayScore) {
                awayPenalties++;
            }

            match.addEvent(
                    new PenaltyTaken(
                            90,
                            match.getHomeTeam(),
                            homeTaker,
                            homeScore
                    )
            );

            match.addEvent(
                    new PenaltyTaken(
                            90,
                            match.getAwayTeam(),
                            awayTaker,
                            awayScore
                    )
            );

            round++;
        }

        match.setHomePenalties(
                homePenalties
        );

        match.setAwayPenalties(
                awayPenalties
        );
    }
}