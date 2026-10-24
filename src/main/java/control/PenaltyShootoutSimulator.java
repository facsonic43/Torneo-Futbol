package control;

import model.match.Match;
import model.match.PenaltyTaken;
import model.participant.Player;
import model.participant.Position;

import java.util.List;
import java.util.Random;

/*
 * Simula una definición por penales.
 * La probabilidad de convertir depende de la diferencia entre
 * la valoración del pateador y la valoración del arquero rival.
 *
 * Se mantiene una probabilidad base del 75% y se limita el
 * resultado entre 55% y 90% para conservar el componente aleatorio.
 */
public class PenaltyShootoutSimulator {

    private Random random;

    public PenaltyShootoutSimulator(Random random) {
        this.random = random;
    }

    /*
     * Simula la tanda completa.
     * Primero se ejecutan hasta cinco penales por equipo y,
     * si continúan empatados, comienza la muerte súbita.
     */
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

        if (homeTakers.isEmpty()
                || awayTakers.isEmpty()) {

            throw new IllegalStateException(
                    "There are not enough players to perform the penalty shootout."
            );
        }

        /*
         * El arquero local enfrenta a los pateadores visitantes
         * y el arquero visitante enfrenta a los pateadores locales.
         */
        Player homeGoalkeeper =
                findGoalkeeper(
                        homeTakers
                );

        Player awayGoalkeeper =
                findGoalkeeper(
                        awayTakers
                );

        int homePenalties = 0;
        int awayPenalties = 0;

        int homeTaken = 0;
        int awayTaken = 0;

        boolean finished = false;

        /*
         * Serie inicial de hasta cinco penales por equipo.
         */
        for (int round = 0;
             round < 5 && !finished;
             round++) {

            Player homeTaker =
                    homeTakers.get(
                            round % homeTakers.size()
                    );

            boolean homeScore =
                    simulatePenalty(
                            match,
                            homeTaker,
                            awayGoalkeeper,
                            true
                    );

            homeTaken++;

            if (homeScore) {
                homePenalties++;
            }

            /*
             * Comprobamos si el visitante todavía puede alcanzar
             * matemáticamente al equipo local.
             */
            if (cannotCatchUp(
                    homePenalties,
                    awayPenalties,
                    5 - awayTaken)) {

                finished = true;

            } else {

                Player awayTaker =
                        awayTakers.get(
                                round % awayTakers.size()
                        );

                boolean awayScore =
                        simulatePenalty(
                                match,
                                awayTaker,
                                homeGoalkeeper,
                                false
                        );

                awayTaken++;

                if (awayScore) {
                    awayPenalties++;
                }

                int homeRemaining =
                        5 - homeTaken;

                int awayRemaining =
                        5 - awayTaken;

                if (cannotCatchUp(
                        homePenalties,
                        awayPenalties,
                        awayRemaining)) {

                    finished = true;

                } else if (cannotCatchUp(
                        awayPenalties,
                        homePenalties,
                        homeRemaining)) {

                    finished = true;
                }
            }
        }

        /*
         * Si después de los primeros cinco penales siguen empatados,
         * comienza la muerte súbita.
         */
        int round = 5;

        while (!finished
                && homePenalties == awayPenalties) {

            Player homeTaker =
                    homeTakers.get(
                            round % homeTakers.size()
                    );

            Player awayTaker =
                    awayTakers.get(
                            round % awayTakers.size()
                    );

            boolean homeScore =
                    simulatePenalty(
                            match,
                            homeTaker,
                            awayGoalkeeper,
                            true
                    );

            boolean awayScore =
                    simulatePenalty(
                            match,
                            awayTaker,
                            homeGoalkeeper,
                            false
                    );

            if (homeScore) {
                homePenalties++;
            }

            if (awayScore) {
                awayPenalties++;
            }

            /*
             * En muerte súbita termina cuando uno convierte
             * y el otro falla dentro de la misma ronda.
             */
            if (homeScore != awayScore) {
                finished = true;
            }

            round++;
        }

        match.setHomePenalties(
                homePenalties
        );

        match.setAwayPenalties(
                awayPenalties
        );
    }

    /*
     * Simula un penal individual.
     * La probabilidad depende del overall del pateador y del arquero rival.
     */
    private boolean simulatePenalty(
            Match match,
            Player taker,
            Player goalkeeper,
            boolean homeTeam) {

        double probability =
                calculatePenaltyProbability(
                        taker,
                        goalkeeper
                );

        boolean scored =
                random.nextDouble()
                        < probability;

        /*
         * El penal se registra como PenaltyTaken.
         * No se suma como gol normal del jugador.
         */
        match.addEvent(
                new PenaltyTaken(
                        90,
                        homeTeam
                                ? match.getHomeTeam()
                                : match.getAwayTeam(),
                        taker,
                        scored
                )
        );

        return scored;
    }

    /*
     * Calcula la probabilidad de convertir.
     *
     * Base: 75%.
     * Cada punto de diferencia entre pateador y arquero
     * modifica la probabilidad en 0,5%.
     *
     * Ejemplo:
     * Pateador 85 - Arquero 75:
     * 0.75 + (10 * 0.005) = 0.80 = 80%.
     */
    private double calculatePenaltyProbability(
            Player taker,
            Player goalkeeper) {

        double kickerRating =
                taker.getOverall();

        double goalkeeperRating =
                goalkeeper.getOverall();

        double probability =
                0.75
                        + (
                        kickerRating
                                - goalkeeperRating
                ) * 0.005;

        /*
         * Evita probabilidades demasiado altas o demasiado bajas.
         */
        probability =
                Math.max(
                        0.55,
                        probability
                );

        probability =
                Math.min(
                        0.90,
                        probability
                );

        return probability;
    }

    /*
     * Busca al arquero que terminó el partido en cancha.
     *
     * Si por una expulsión o lesión no queda un arquero natural,
     * utiliza al jugador con mayor overall como arquero de emergencia.
     */
    private Player findGoalkeeper(
            List<Player> players) {

        for (Player player :
                players) {

            if (player.getPosition()
                    == Position.GOALKEEPER) {

                return player;
            }
        }

        Player emergencyGoalkeeper =
                players.get(0);

        for (Player player :
                players) {

            if (player.getOverall()
                    > emergencyGoalkeeper.getOverall()) {

                emergencyGoalkeeper =
                        player;
            }
        }

        return emergencyGoalkeeper;
    }

    /*
     * Determina si un equipo ya no puede alcanzar al rival
     * con los penales que todavía le quedan por ejecutar.
     */
    private boolean cannotCatchUp(
            int leadingScore,
            int otherScore,
            int otherRemaining) {

        return leadingScore
                > otherScore
                + otherRemaining;
    }
}