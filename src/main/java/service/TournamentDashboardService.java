package service;

import model.match.Event;
import model.match.GroupMatch;
import model.match.Match;
import model.match.RedCard;
import model.match.YellowCard;
import model.participant.Player;
import model.participant.Team;
import model.tournament.Group;
import model.tournament.Tournament;

import java.util.HashMap;
import java.util.Map;

/*
 * Calcula la información resumida que se muestra
 * en el dashboard principal del campeonato.
 */
public class TournamentDashboardService {

    // Devuelve el primer partido de grupos que todavía no fue jugado.
    public GroupMatch getNextGroupMatch(
            Tournament tournament) {

        for (Group group :
                tournament.getGroups()) {

            for (GroupMatch match :
                    group.getMatches()) {

                if (!match.isPlayed()) {
                    return match;
                }
            }
        }

        return null;
    }

    public int getPlayedMatches(
            Tournament tournament) {

        int played = 0;

        for (Match match :
                tournament.getAllMatches()) {

            if (match.isPlayed()) {
                played++;
            }
        }

        return played;
    }

    public Player getTopScorer(
            Tournament tournament) {

        Player best = null;

        for (Team team :
                tournament.getTeams()) {

            for (Player player :
                    team.getSquad()) {

                if (best == null
                        || player.getGoals()
                        > best.getGoals()) {

                    best =
                            player;
                }
            }
        }

        if (best != null
                && best.getGoals() == 0) {

            return null;
        }

        return best;
    }

    public Player getMostMinutesPlayer(
            Tournament tournament) {

        Player best = null;

        for (Team team :
                tournament.getTeams()) {

            for (Player player :
                    team.getSquad()) {

                if (best == null
                        || player.getMinutesPlayed()
                        > best.getMinutesPlayed()) {

                    best =
                            player;
                }
            }
        }

        if (best != null
                && best.getMinutesPlayed() == 0) {

            return null;
        }

        return best;
    }

    // Calcula el equipo con menor puntaje disciplinario.
    public Team getFairPlayTeam(
            Tournament tournament) {

        Map<Team, Integer> points =
                new HashMap<>();

        for (Team team :
                tournament.getTeams()) {

            points.put(
                    team,
                    0
            );
        }

        for (Match match :
                tournament.getAllMatches()) {

            for (Event event :
                    match.getEvents()) {

                Team team =
                        event.getTeam();

                if (event
                        instanceof YellowCard) {

                    points.put(
                            team,
                            points.get(team) + 1
                    );
                }

                if (event
                        instanceof RedCard) {

                    points.put(
                            team,
                            points.get(team) + 3
                    );
                }
            }
        }

        Team best =
                null;

        for (Team team :
                tournament.getTeams()) {

            if (best == null
                    || points.get(team)
                    < points.get(best)) {

                best =
                        team;

            } else if (points.get(team)
                    .equals(
                            points.get(best)
                    )
                    && team.getName()
                    .compareToIgnoreCase(
                            best.getName()
                    ) < 0) {

                best =
                        team;
            }
        }

        return best;
    }

    public int getFairPlayPoints(
            Tournament tournament,
            Team selectedTeam) {

        if (selectedTeam == null) {
            return 0;
        }

        int points = 0;

        for (Match match :
                tournament.getAllMatches()) {

            for (Event event :
                    match.getEvents()) {

                if (event.getTeam()
                        != selectedTeam) {

                    continue;
                }

                if (event
                        instanceof YellowCard) {

                    points++;
                }

                if (event
                        instanceof RedCard) {

                    points += 3;
                }
            }
        }

        return points;
    }

    public Team findPlayerTeam(
            Tournament tournament,
            Player player) {

        if (player == null) {
            return null;
        }

        for (Team team :
                tournament.getTeams()) {

            if (team.getSquad()
                    .contains(player)) {

                return team;
            }
        }

        return null;
    }
}