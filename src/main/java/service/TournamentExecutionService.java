package service;

import control.MatchSimulator;
import model.match.FirstLegMatch;
import model.match.GroupMatch;
import model.match.Match;
import model.match.SecondLegMatch;
import model.match.Stadium;
import model.tournament.Group;
import model.tournament.Tournament;
import model.tournament.TournamentStage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Controla la ejecución manual del campeonato.
 * Permite jugar un partido, una fecha completa o toda la fase pendiente,
 * respetando siempre el orden cronológico y generando la etapa siguiente
 * cuando corresponde.
 */
public class TournamentExecutionService {

    private MatchSimulator simulator;

    public TournamentExecutionService() {
        simulator =
                new MatchSimulator();
    }

    // Permite utilizar una simulación repetible durante los tests.
    public TournamentExecutionService(
            long seed) {

        simulator =
                new MatchSimulator(
                        seed
                );
    }

    /*
     * Juega solamente el partido seleccionado.
     * El partido debe pertenecer a la fecha actualmente habilitada.
     */
    public Match playMatch(
            Tournament tournament,
            Match match,
            List<Stadium> stadiums) {

        if (tournament == null) {
            throw new IllegalArgumentException(
                    "Tournament cannot be null."
            );
        }

        if (match == null) {
            throw new IllegalArgumentException(
                    "Match cannot be null."
            );
        }

        if (match.isPlayed()) {
            throw new IllegalStateException(
                    "This match has already been played."
            );
        }

        if (!isMatchPlayable(
                tournament,
                match
        )) {

            throw new IllegalStateException(
                    "This match is currently locked."
            );
        }

        /*
         * La final necesita usar el método del Tournament
         * porque además de simularla debe guardar al campeón.
         */
        if (match
                == tournament.getFinalMatch()) {

            tournament.simulateFinal(
                    simulator
            );

        } else {

            simulator.simulateMatch(
                    match
            );
        }

        synchronizeTournament(
                tournament,
                stadiums
        );

        return match;
    }

    /*
     * Simula todos los partidos pendientes de la fecha actual.
     * No avanza automáticamente sobre la siguiente fecha.
     */
    public int simulateCurrentMatchday(
            Tournament tournament,
            List<Stadium> stadiums) {

        List<Match> playableMatches =
                new ArrayList<>(
                        getCurrentPlayableMatches(
                                tournament
                        )
                );

        int simulated = 0;

        for (Match match :
                playableMatches) {

            playMatch(
                    tournament,
                    match,
                    stadiums
            );

            simulated++;
        }

        return simulated;
    }

    /*
     * Simula todos los partidos que todavía faltan
     * de la fase actual.
     */
    public int simulateRemainingPhase(
            Tournament tournament,
            List<Stadium> stadiums) {

        TournamentStage stage =
                getCurrentStage(
                        tournament
                );

        if (stage
                == TournamentStage.FINISHED) {

            return 0;
        }

        List<Match> phaseMatches =
                new ArrayList<>(
                        getCurrentPhaseMatches(
                                tournament
                        )
                );

        phaseMatches.sort(
                Comparator.comparing(
                        Match::getMatchDate
                )
        );

        int simulated = 0;

        for (Match match :
                phaseMatches) {

            if (!match.isPlayed()) {

                /*
                 * Como están ordenados por fecha,
                 * playMatch siempre recibe un partido habilitado.
                 */
                playMatch(
                        tournament,
                        match,
                        stadiums
                );

                simulated++;
            }
        }

        return simulated;
    }

    /*
     * Devuelve únicamente los partidos que pueden jugarse ahora.
     * Son los partidos pendientes de la fecha más próxima.
     */
    public List<Match> getCurrentPlayableMatches(
            Tournament tournament) {

        List<Match> phaseMatches =
                getCurrentPhaseMatches(
                        tournament
                );

        LocalDate nextDate =
                null;

        for (Match match :
                phaseMatches) {

            if (!match.isPlayed()) {

                if (nextDate == null
                        || match.getMatchDate()
                        .isBefore(
                                nextDate
                        )) {

                    nextDate =
                            match.getMatchDate();
                }
            }
        }

        List<Match> playable =
                new ArrayList<>();

        if (nextDate == null) {
            return playable;
        }

        for (Match match :
                phaseMatches) {

            if (!match.isPlayed()
                    && match.getMatchDate()
                    .equals(
                            nextDate
                    )) {

                playable.add(
                        match
                );
            }
        }

        playable.sort(
                Comparator
                        .comparing(
                                match ->
                                        match
                                                .getHomeTeam()
                                                .getName()
                        )
        );

        return playable;
    }

    // Devuelve todos los partidos correspondientes a la etapa actual.
    public List<Match> getCurrentPhaseMatches(
            Tournament tournament) {

        List<Match> matches =
                new ArrayList<>();

        TournamentStage stage =
                getCurrentStage(
                        tournament
                );

        if (stage
                == TournamentStage.GROUP_STAGE) {

            for (Group group :
                    tournament.getGroups()) {

                matches.addAll(
                        group.getMatches()
                );
            }

        } else if (stage
                == TournamentStage.QUARTER_FINALS) {

            matches.addAll(
                    tournament
                            .getQuarterFinalFirstLegs()
            );

            matches.addAll(
                    tournament
                            .getQuarterFinalSecondLegs()
            );

        } else if (stage
                == TournamentStage.SEMI_FINALS) {

            matches.addAll(
                    tournament
                            .getSemiFinalFirstLegs()
            );

            matches.addAll(
                    tournament
                            .getSemiFinalSecondLegs()
            );

        } else if (stage
                == TournamentStage.FINAL) {

            if (tournament.getFinalMatch()
                    != null) {

                matches.add(
                        tournament
                                .getFinalMatch()
                );
            }
        }

        matches.sort(
                Comparator
                        .comparing(
                                Match::getMatchDate
                        )
                        .thenComparing(
                                match ->
                                        match
                                                .getHomeTeam()
                                                .getName()
                        )
        );

        return matches;
    }

    // Indica si el usuario puede ejecutar un partido en este momento.
    public boolean isMatchPlayable(
            Tournament tournament,
            Match match) {

        if (match == null
                || match.isPlayed()) {

            return false;
        }

        List<Match> playable =
                getCurrentPlayableMatches(
                        tournament
                );

        for (Match current :
                playable) {

            if (current == match) {
                return true;
            }
        }

        return false;
    }

    /*
     * Si una fase terminó, genera solamente la siguiente.
     * No la simula. El usuario sigue teniendo el control.
     */
    public void synchronizeTournament(
            Tournament tournament,
            List<Stadium> stadiums) {

        if (tournament == null) {
            return;
        }

        /*
         * Terminó fase de grupos:
         * generamos cuartos pero no los jugamos.
         */
        if (allGroupMatchesPlayed(
                tournament
        )
                && tournament
                .getQuarterFinalFirstLegs()
                .isEmpty()) {

            tournament.generateQuarterFinals(
                    stadiums
            );

            return;
        }

        /*
         * Terminó cuartos:
         * generamos semifinales.
         */
        if (!tournament
                .getQuarterFinalSecondLegs()
                .isEmpty()
                && allMatchesPlayed(
                tournament
                        .getQuarterFinalFirstLegs()
        )
                && allMatchesPlayed(
                tournament
                        .getQuarterFinalSecondLegs()
        )
                && tournament
                .getSemiFinalFirstLegs()
                .isEmpty()) {

            tournament.generateSemiFinals();

            return;
        }

        /*
         * Terminó semifinal:
         * generamos final.
         */
        if (!tournament
                .getSemiFinalSecondLegs()
                .isEmpty()
                && allMatchesPlayed(
                tournament
                        .getSemiFinalFirstLegs()
        )
                && allMatchesPlayed(
                tournament
                        .getSemiFinalSecondLegs()
        )
                && tournament
                .getFinalMatch()
                == null) {

            tournament.generateFinal();
        }
    }

    public TournamentStage getCurrentStage(
            Tournament tournament) {

        if (tournament == null) {
            return TournamentStage.NOT_STARTED;
        }

        if (tournament.getChampion()
                != null) {

            return TournamentStage.FINISHED;
        }

        if (tournament.getFinalMatch()
                != null) {

            return TournamentStage.FINAL;
        }

        if (!tournament
                .getSemiFinalFirstLegs()
                .isEmpty()) {

            return TournamentStage.SEMI_FINALS;
        }

        if (!tournament
                .getQuarterFinalFirstLegs()
                .isEmpty()) {

            return TournamentStage.QUARTER_FINALS;
        }

        if (!tournament
                .getGroups()
                .isEmpty()) {

            return TournamentStage.GROUP_STAGE;
        }

        return TournamentStage.NOT_STARTED;
    }

    public int getPlayedMatchesInCurrentPhase(
            Tournament tournament) {

        int played = 0;

        for (Match match :
                getCurrentPhaseMatches(
                        tournament
                )) {

            if (match.isPlayed()) {
                played++;
            }
        }

        return played;
    }

    public int getTotalMatchesInCurrentPhase(
            Tournament tournament) {

        return getCurrentPhaseMatches(
                tournament
        ).size();
    }

    /*
     * Devuelve un texto sencillo para mostrar en Match Day.
     */
    public String getCurrentRoundName(
            Tournament tournament) {

        TournamentStage stage =
                getCurrentStage(
                        tournament
                );

        List<Match> playable =
                getCurrentPlayableMatches(
                        tournament
                );

        if (stage
                == TournamentStage.FINISHED) {

            return "TOURNAMENT FINISHED";
        }

        if (playable.isEmpty()) {

            return formatStage(
                    stage
            );
        }

        Match next =
                playable.get(0);

        if (stage
                == TournamentStage.GROUP_STAGE) {

            return getGroupMatchdayName(
                    tournament,
                    next.getMatchDate()
            );
        }

        if (stage
                == TournamentStage.QUARTER_FINALS) {

            if (next
                    instanceof FirstLegMatch) {

                return "QUARTER-FINALS - FIRST LEG";
            }

            return "QUARTER-FINALS - SECOND LEG";
        }

        if (stage
                == TournamentStage.SEMI_FINALS) {

            if (next
                    instanceof FirstLegMatch) {

                return "SEMI-FINALS - FIRST LEG";
            }

            return "SEMI-FINALS - SECOND LEG";
        }

        if (stage
                == TournamentStage.FINAL) {

            return "FINAL";
        }

        return formatStage(
                stage
        );
    }

    private String getGroupMatchdayName(
            Tournament tournament,
            LocalDate date) {

        List<LocalDate> dates =
                new ArrayList<>();

        for (Group group :
                tournament.getGroups()) {

            for (GroupMatch match :
                    group.getMatches()) {

                if (!dates.contains(
                        match.getMatchDate()
                )) {

                    dates.add(
                            match.getMatchDate()
                    );
                }
            }
        }

        dates.sort(
                LocalDate::compareTo
        );

        for (int i = 0;
             i < dates.size();
             i++) {

            if (dates.get(i)
                    .equals(date)) {

                return "GROUP STAGE - MATCHDAY "
                        + (i + 1);
            }
        }

        return "GROUP STAGE";
    }

    private boolean allGroupMatchesPlayed(
            Tournament tournament) {

        if (tournament.getGroups()
                .isEmpty()) {

            return false;
        }

        for (Group group :
                tournament.getGroups()) {

            for (GroupMatch match :
                    group.getMatches()) {

                if (!match.isPlayed()) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean allMatchesPlayed(
            List<? extends Match> matches) {

        if (matches.isEmpty()) {
            return false;
        }

        for (Match match :
                matches) {

            if (!match.isPlayed()) {
                return false;
            }
        }

        return true;
    }

    private String formatStage(
            TournamentStage stage) {

        if (stage
                == TournamentStage.GROUP_STAGE) {

            return "GROUP STAGE";
        }

        if (stage
                == TournamentStage.QUARTER_FINALS) {

            return "QUARTER-FINALS";
        }

        if (stage
                == TournamentStage.SEMI_FINALS) {

            return "SEMI-FINALS";
        }

        if (stage
                == TournamentStage.FINAL) {

            return "FINAL";
        }

        if (stage
                == TournamentStage.FINISHED) {

            return "FINISHED";
        }

        return "NOT STARTED";
    }
}