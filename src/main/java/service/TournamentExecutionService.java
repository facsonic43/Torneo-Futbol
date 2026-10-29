package service;

import control.MatchSimulator;
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
 * Coordina la ejecución manual del campeonato.
 * La GUI puede jugar un partido, una fecha o una fase completa, pero las
 * tandas de penales quedan pendientes hasta que el usuario las simula.
 */
public class TournamentExecutionService {
    private MatchSimulator simulator;

    public TournamentExecutionService() {
        simulator = new MatchSimulator();
    }

    // Permite una simulación repetible durante los tests.
    public TournamentExecutionService(long seed) {
        simulator = new MatchSimulator(seed);
    }

    // Simula únicamente los 90 minutos del partido seleccionado.
    public Match playMatch(
            Tournament tournament,
            Match match,
            List<Stadium> stadiums) {

        validateTournamentAndMatch(
                tournament,
                match
        );

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

        simulator.simulateRegularTime(
                match
        );

        /*
         * Tournament.simulateFinal() registra al campeón.
         * Como MatchSimulator no vuelve a jugar un Match ya jugado,
         * los 90 minutos no se repiten.
         */
        if (match == tournament.getFinalMatch()) {
            tournament.simulateFinal(
                    simulator
            );
        }

        synchronizeTournament(
                tournament,
                stadiums
        );

        return match;
    }

    // Resuelve únicamente la tanda de penales pendiente del partido elegido.
    public Match simulatePenalties(
            Tournament tournament,
            Match match,
            List<Stadium> stadiums) {

        validateTournamentAndMatch(
                tournament,
                match
        );

        if (!requiresPenalties(match)) {
            throw new IllegalStateException(
                    "This match does not have a pending penalty shootout."
            );
        }

        simulator.simulatePenaltyShootout(
                match
        );

        /*
         * Si es la final, ahora sí Tournament puede registrar
         * al ganador de la tanda como campeón.
         */
        if (match == tournament.getFinalMatch()) {
            tournament.simulateFinal(
                    simulator
            );
        }

        synchronizeTournament(
                tournament,
                stadiums
        );

        return match;
    }

    // Indica si los 90 minutos terminaron pero todavía falta definir por penales.
    public boolean requiresPenalties(Match match) {
        return match != null
                && match.isPlayed()
                && match.requiresTieBreak();
    }

    // Devuelve las definiciones por penales pendientes de la fase actual.
    public List<Match> getPendingPenaltyMatches(
            Tournament tournament) {

        List<Match> pending =
                new ArrayList<>();

        for (Match match :
                getCurrentPhaseMatches(tournament)) {

            if (requiresPenalties(match)) {
                pending.add(match);
            }
        }

        return pending;
    }

    // Simula todos los 90 minutos pendientes de la fecha actual.
    public int simulateCurrentMatchday(
            Tournament tournament,
            List<Stadium> stadiums) {

        List<Match> playableMatches =
                new ArrayList<>(
                        getCurrentPlayableMatches(tournament)
                );

        for (Match match : playableMatches) {
            playMatch(
                    tournament,
                    match,
                    stadiums
            );
        }

        return playableMatches.size();
    }

    /*
     * Simula todos los 90 minutos pendientes de la fase actual.
     * Si alguna serie necesita penales, la siguiente fase no se genera todavía.
     */
    public int simulateRemainingPhase(
            Tournament tournament,
            List<Stadium> stadiums) {

        if (getCurrentStage(tournament)
                == TournamentStage.FINISHED) {

            return 0;
        }

        List<Match> phaseMatches =
                new ArrayList<>(
                        getCurrentPhaseMatches(tournament)
                );

        phaseMatches.sort(
                Comparator.comparing(
                        Match::getMatchDate
                )
        );

        int simulated = 0;

        for (Match match : phaseMatches) {
            if (!match.isPlayed()) {
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
     * Devuelve los partidos pendientes de la fecha más próxima.
     * Un partido ya jugado que espera penales no vuelve a aparecer como jugable.
     */
    public List<Match> getCurrentPlayableMatches(
            Tournament tournament) {

        List<Match> phaseMatches =
                getCurrentPhaseMatches(tournament);

        LocalDate nextDate =
                null;

        for (Match match : phaseMatches) {
            if (!match.isPlayed()
                    && (nextDate == null
                    || match.getMatchDate().isBefore(nextDate))) {

                nextDate =
                        match.getMatchDate();
            }
        }

        List<Match> playable =
                new ArrayList<>();

        if (nextDate == null) {
            return playable;
        }

        for (Match match : phaseMatches) {
            if (!match.isPlayed()
                    && match.getMatchDate().equals(nextDate)) {

                playable.add(match);
            }
        }

        playable.sort(
                Comparator.comparing(
                        match ->
                                match.getHomeTeam().getName()
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
                getCurrentStage(tournament);

        if (stage == TournamentStage.GROUP_STAGE) {
            for (Group group : tournament.getGroups()) {
                matches.addAll(
                        group.getMatches()
                );
            }

        } else if (stage == TournamentStage.QUARTER_FINALS) {
            matches.addAll(
                    tournament.getQuarterFinalFirstLegs()
            );

            matches.addAll(
                    tournament.getQuarterFinalSecondLegs()
            );

        } else if (stage == TournamentStage.SEMI_FINALS) {
            matches.addAll(
                    tournament.getSemiFinalFirstLegs()
            );

            matches.addAll(
                    tournament.getSemiFinalSecondLegs()
            );

        } else if (stage == TournamentStage.FINAL
                && tournament.getFinalMatch() != null) {

            matches.add(
                    tournament.getFinalMatch()
            );
        }

        matches.sort(
                Comparator
                        .comparing(
                                Match::getMatchDate
                        )
                        .thenComparing(
                                match ->
                                        match.getHomeTeam().getName()
                        )
        );

        return matches;
    }

    // Indica si el usuario puede ejecutar los 90 minutos de ese partido ahora.
    public boolean isMatchPlayable(
            Tournament tournament,
            Match match) {

        if (match == null
                || match.isPlayed()) {

            return false;
        }

        for (Match playable :
                getCurrentPlayableMatches(tournament)) {

            if (playable == match) {
                return true;
            }
        }

        return false;
    }

    public boolean hasUnplayedMatchesInCurrentPhase(
            Tournament tournament) {

        for (Match match :
                getCurrentPhaseMatches(tournament)) {

            if (!match.isPlayed()) {
                return true;
            }
        }

        return false;
    }

    /*
     * Genera la etapa siguiente sólo cuando la actual está completamente resuelta.
     * Un partido de vuelta pendiente de penales todavía no tiene ganador.
     */
    public void synchronizeTournament(
            Tournament tournament,
            List<Stadium> stadiums) {

        if (tournament == null) {
            return;
        }

        if (allGroupMatchesPlayed(tournament)
                && tournament
                .getQuarterFinalFirstLegs()
                .isEmpty()) {

            tournament.generateQuarterFinals(
                    stadiums
            );

            return;
        }

        if (!tournament
                .getQuarterFinalSecondLegs()
                .isEmpty()

                && allMatchesPlayed(
                tournament.getQuarterFinalFirstLegs()
        )

                && allKnockoutSeriesResolved(
                tournament.getQuarterFinalSecondLegs()
        )

                && tournament
                .getSemiFinalFirstLegs()
                .isEmpty()) {

            tournament.generateSemiFinals();

            return;
        }

        if (!tournament
                .getSemiFinalSecondLegs()
                .isEmpty()

                && allMatchesPlayed(
                tournament.getSemiFinalFirstLegs()
        )

                && allKnockoutSeriesResolved(
                tournament.getSemiFinalSecondLegs()
        )

                && tournament.getFinalMatch()
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

        int played =
                0;

        for (Match match :
                getCurrentPhaseMatches(tournament)) {

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

    // Texto utilizado por Match Day para indicar la ronda habilitada.
    public String getCurrentRoundName(
            Tournament tournament) {

        TournamentStage stage =
                getCurrentStage(tournament);

        List<Match> playable =
                getCurrentPlayableMatches(tournament);

        if (stage == TournamentStage.FINISHED) {
            return "TOURNAMENT FINISHED";
        }

        if (playable.isEmpty()
                && !getPendingPenaltyMatches(tournament)
                .isEmpty()) {

            return formatStage(stage)
                    + " - PENALTIES PENDING";
        }

        if (playable.isEmpty()) {
            return formatStage(stage);
        }

        Match next =
                playable.get(0);

        if (stage == TournamentStage.GROUP_STAGE) {
            return getGroupMatchdayName(
                    tournament,
                    next.getMatchDate()
            );
        }

        if (stage == TournamentStage.QUARTER_FINALS) {
            return next.asFirstLeg().isPresent()
                    ? "QUARTER-FINALS - FIRST LEG"
                    : "QUARTER-FINALS - SECOND LEG";
        }

        if (stage == TournamentStage.SEMI_FINALS) {
            return next.asFirstLeg().isPresent()
                    ? "SEMI-FINALS - FIRST LEG"
                    : "SEMI-FINALS - SECOND LEG";
        }

        if (stage == TournamentStage.FINAL) {
            return "FINAL";
        }

        return formatStage(stage);
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

            if (dates.get(i).equals(date)) {
                return "GROUP STAGE - MATCHDAY "
                        + (i + 1);
            }
        }

        return "GROUP STAGE";
    }

    private boolean allGroupMatchesPlayed(
            Tournament tournament) {

        if (tournament
                .getGroups()
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

        for (Match match : matches) {
            if (!match.isPlayed()) {
                return false;
            }
        }

        return true;
    }

    /*
     * Una ronda eliminatoria sólo está resuelta cuando cada partido de vuelta
     * fue jugado y cada serie tiene un ganador definitivo.
     */
    private boolean allKnockoutSeriesResolved(
            List<SecondLegMatch> secondLegMatches) {

        if (secondLegMatches.isEmpty()) {
            return false;
        }

        for (SecondLegMatch match :
                secondLegMatches) {

            if (!match.isPlayed()
                    || match.getWinner() == null) {

                return false;
            }
        }

        return true;
    }

    private void validateTournamentAndMatch(
            Tournament tournament,
            Match match) {

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
    }

    private String formatStage(
            TournamentStage stage) {

        if (stage == TournamentStage.GROUP_STAGE) {
            return "GROUP STAGE";
        }

        if (stage == TournamentStage.QUARTER_FINALS) {
            return "QUARTER-FINALS";
        }

        if (stage == TournamentStage.SEMI_FINALS) {
            return "SEMI-FINALS";
        }

        if (stage == TournamentStage.FINAL) {
            return "FINAL";
        }

        if (stage == TournamentStage.FINISHED) {
            return "FINISHED";
        }

        return "NOT STARTED";
    }
}