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

// Coordina la ejecución de partidos, fechas y fases respetando el orden del campeonato.
// Simula los 90 minutos y deja las tandas pendientes para su ejecución manual; al completar
// una fase habilita la siguiente y actualiza el campeón cuando se resuelve la final.

public class TournamentExecutionService {
    private MatchSimulator simulator;

    public TournamentExecutionService() {
        simulator = new MatchSimulator();
    }

    public TournamentExecutionService(long seed) {
        simulator = new MatchSimulator(seed);
    }

    public MatchSimulator getSimulator() {
        return simulator;
    }

    public List<Match> getPendingPenaltyMatches(Tournament tournament) {
        List<Match> pending = new ArrayList<>();
        for (Match match : getCurrentPhaseMatches(tournament)) {
            if (requiresPenalties(match)) {
                pending.add(match);
            }
        }
        return pending;
    }

    public List<Match> getCurrentPlayableMatches(Tournament tournament) {
        List<Match> phaseMatches = getCurrentPhaseMatches(tournament);
        LocalDate nextDate = null;
        for (Match match : phaseMatches) {
            if (!match.isPlayed() && (nextDate == null || match.getMatchDate().isBefore(nextDate))) {
                nextDate = match.getMatchDate();
            }
        }
        List<Match> playable = new ArrayList<>();
        if (nextDate == null) {
            return playable;
        }
        for (Match match : phaseMatches) {
            if (!match.isPlayed() && match.getMatchDate().equals(nextDate)) {
                playable.add(match);
            }
        }
        playable.sort(Comparator.comparing(match -> match.getHomeTeam().getName()));
        return playable;
    }

    public List<Match> getCurrentPhaseMatches(Tournament tournament) {
        return getMatchesOfStage(tournament, getCurrentStage(tournament));
    }

    public static List<Match> getMatchesOfStage(Tournament tournament, TournamentStage stage) {
        List<Match> matches = new ArrayList<>();
        if (tournament == null || stage == null) {
            return matches;
        }
        if (stage == TournamentStage.GROUP_STAGE) {
            for (Group group : tournament.getGroups()) {
                matches.addAll(group.getMatches());
            }
        } else if (stage == TournamentStage.QUARTER_FINALS) {
            matches.addAll(tournament.getQuarterFinalFirstLegs());
            matches.addAll(tournament.getQuarterFinalSecondLegs());
        } else if (stage == TournamentStage.SEMI_FINALS) {
            matches.addAll(tournament.getSemiFinalFirstLegs());
            matches.addAll(tournament.getSemiFinalSecondLegs());
        } else if (stage == TournamentStage.FINAL && tournament.getFinalMatch() != null) {
            matches.add(tournament.getFinalMatch());
        }
        matches.sort( Comparator.comparing(Match::getMatchDate).thenComparing(match -> match.getHomeTeam().getName()) );
        return matches;
    }

    public boolean isMatchPlayable(Tournament tournament, Match match) {
        if (match == null || match.isPlayed()) {
            return false;
        }
        for (Match playable : getCurrentPlayableMatches(tournament)) {
            if (playable == match) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUnplayedMatchesInCurrentPhase(Tournament tournament) {
        for (Match match : getCurrentPhaseMatches(tournament)) {
            if (!match.isPlayed()) {
                return true;
            }
        }
        return false;
    }

    public TournamentStage getCurrentStage(Tournament tournament) {
        if (tournament == null) {
            return TournamentStage.NOT_STARTED;
        }
        if (tournament.getChampion() != null) {
            return TournamentStage.FINISHED;
        }
        if (tournament.getFinalMatch() != null) {
            return TournamentStage.FINAL;
        }
        if (!tournament.getSemiFinalFirstLegs().isEmpty()) {
            return TournamentStage.SEMI_FINALS;
        }
        if (!tournament.getQuarterFinalFirstLegs().isEmpty()) {
            return TournamentStage.QUARTER_FINALS;
        }
        if (!tournament.getGroups().isEmpty()) {
            return TournamentStage.GROUP_STAGE;
        }
        return TournamentStage.NOT_STARTED;
    }

    public int getPlayedMatchesInCurrentPhase(Tournament tournament) {
        int played = 0;
        for (Match match : getCurrentPhaseMatches(tournament)) {
            if (match.isPlayed()) {
                played++;
            }
        }
        return played;
    }

    public int getTotalMatchesInCurrentPhase(Tournament tournament) {
        return getCurrentPhaseMatches(tournament).size();
    }

    public String getCurrentRoundName(Tournament tournament) {
        TournamentStage stage = getCurrentStage(tournament);
        List<Match> playable = getCurrentPlayableMatches(tournament);
        if (stage == TournamentStage.FINISHED) {
            return "TOURNAMENT FINISHED";
        }
        if (playable.isEmpty() && !getPendingPenaltyMatches(tournament).isEmpty()) {
            return formatStage(stage) + " - PENALTIES PENDING";
        }
        if (playable.isEmpty()) {
            return formatStage(stage);
        }
        Match next = playable.get(0);
        if (stage == TournamentStage.GROUP_STAGE) {
            return getGroupMatchdayName(tournament, next.getMatchDate());
        }
        if (stage == TournamentStage.QUARTER_FINALS) {
            return next.asFirstLeg().isPresent() ? "QUARTER-FINALS - FIRST LEG" : "QUARTER-FINALS - SECOND LEG";
        }
        if (stage == TournamentStage.SEMI_FINALS) {
            return next.asFirstLeg().isPresent() ? "SEMI-FINALS - FIRST LEG" : "SEMI-FINALS - SECOND LEG";
        }
        if (stage == TournamentStage.FINAL) {
            return "FINAL";
        }
        return formatStage(stage);
    }

    private String getGroupMatchdayName(Tournament tournament, LocalDate date) {
        List<LocalDate> dates = new ArrayList<>();
        for (Group group : tournament.getGroups()) {
            for (GroupMatch match : group.getMatches()) {
                if (!dates.contains(match.getMatchDate())) {
                    dates.add(match.getMatchDate());
                }
            }
        }
        dates.sort(LocalDate::compareTo);
        for (int i = 0; i < dates.size(); i++) {
            if (dates.get(i).equals(date)) {
                return "GROUP STAGE - MATCHDAY " + (i + 1);
            }
        }
        return "GROUP STAGE";
    }

    public void playMatch(Tournament tournament, Match match, List<Stadium> stadiums) {
        validateTournamentAndMatch(tournament, match);
        if (match.isPlayed()) {
            throw new IllegalStateException("This match has already been played.");
        }
        if (!isMatchPlayable(tournament, match)) {
            throw new IllegalStateException("This match is currently locked.");
        }
        simulator.simulateRegularTime(match);
        if (match == tournament.getFinalMatch()) {
            tournament.simulateFinal(simulator);
        }
        synchronizeTournament(tournament, stadiums);
    }

    public void simulatePenalties(Tournament tournament, Match match, List<Stadium> stadiums) {
        validateTournamentAndMatch(tournament, match);
        if (!requiresPenalties(match)) {
            throw new IllegalStateException("This match does not have a pending penalty shootout.");
        }
        simulator.simulatePenaltyShootout(match);
        if (match == tournament.getFinalMatch()) {
            tournament.simulateFinal(simulator);
        }
        synchronizeTournament(tournament, stadiums);
    }

    public boolean requiresPenalties(Match match) {
        return match != null && match.isPlayed() && match.requiresTieBreak();
    }

    public int simulateCurrentMatchday(Tournament tournament, List<Stadium> stadiums) {
        List<Match> playableMatches = getCurrentPlayableMatches(tournament);
        for (Match match : playableMatches) {
            playMatch(tournament, match, stadiums);
        }
        return playableMatches.size();
    }

    public int simulateRemainingPhase(Tournament tournament, List<Stadium> stadiums) {
        if (getCurrentStage(tournament) == TournamentStage.FINISHED) {
            return 0;
        }
        List<Match> phaseMatches = getCurrentPhaseMatches(tournament);
        int simulated = 0;
        for (Match match : phaseMatches) {
            if (!match.isPlayed()) {
                playMatch(tournament, match, stadiums);
                simulated++;
            }
        }
        return simulated;
    }

    private void synchronizeTournament(Tournament tournament, List<Stadium> stadiums) {
        if (allGroupMatchesPlayed(tournament) && tournament.getQuarterFinalFirstLegs().isEmpty()) {
            tournament.generateQuarterFinals(stadiums);
            return;
        }
        if (!tournament.getQuarterFinalSecondLegs().isEmpty() && allMatchesPlayed(tournament.getQuarterFinalFirstLegs())
                && allKnockoutSeriesResolved(tournament.getQuarterFinalSecondLegs())
                && tournament.getSemiFinalFirstLegs().isEmpty()) {
            tournament.generateSemiFinals();
            return;
        }
        if (!tournament.getSemiFinalSecondLegs().isEmpty() && allMatchesPlayed(tournament.getSemiFinalFirstLegs())
                && allKnockoutSeriesResolved(tournament.getSemiFinalSecondLegs())
                && tournament.getFinalMatch() == null) {
            tournament.generateFinal();
        }
    }

    private boolean allGroupMatchesPlayed(Tournament tournament) {
        if (tournament.getGroups().isEmpty()) {
            return false;
        }
        for (Group group : tournament.getGroups()) {
            for (GroupMatch match : group.getMatches()) {
                if (!match.isPlayed()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean allMatchesPlayed(List<? extends Match> matches) {
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

    private boolean allKnockoutSeriesResolved(List<SecondLegMatch> secondLegMatches) {
        if (secondLegMatches.isEmpty()) {
            return false;
        }
        for (SecondLegMatch match : secondLegMatches) {
            if (!match.isPlayed() || match.getWinner() == null) {
                return false;
            }
        }
        return true;
    }

    private void validateTournamentAndMatch(Tournament tournament, Match match) {
        if (tournament == null) {
            throw new IllegalArgumentException("Tournament cannot be null.");
        }
        if (match == null) {
            throw new IllegalArgumentException("Match cannot be null.");
        }
    }

    private String formatStage(TournamentStage stage) {
        return stage.getLabel();
    }
}
