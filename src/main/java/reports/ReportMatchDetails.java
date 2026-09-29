package reports;

import model.match.Event;
import model.match.Goal;
import model.match.Match;
import model.match.RedCard;
import model.match.Substitution;
import model.participant.Player;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Datos exclusivos de los reportes, sin modificar los modelos del equipo. */
public class ReportMatchDetails {
    /** El equipo del Goal es el beneficiado, también para un gol en contra. */
    public record GoalDetails(boolean penalty, boolean ownGoal, Player receivingGoalkeeper) { }

    private List<Player> homeStarters = List.of();
    private List<Player> awayStarters = List.of();
    private boolean startingPlayersRecorded;
    private final Map<Goal, GoalDetails> goals = new IdentityHashMap<>();

    public void setStartingPlayers(List<Player> homeStarters, List<Player> awayStarters) {
        this.homeStarters = List.copyOf(homeStarters);
        this.awayStarters = List.copyOf(awayStarters);
        startingPlayersRecorded = true;
    }

    public List<Player> getHomeStarters() { return homeStarters; }

    public List<Player> getAwayStarters() { return awayStarters; }

    public boolean hasStartingPlayers() { return startingPlayersRecorded; }

    public void setGoalDetails(Goal goal, boolean penalty, boolean ownGoal, Player receivingGoalkeeper) {
        Objects.requireNonNull(goal, "El gol es obligatorio");
        if (penalty && ownGoal) {
            throw new IllegalArgumentException("Un gol en contra no puede ser un gol de penal");
        }
        goals.put(goal, new GoalDetails(penalty, ownGoal, receivingGoalkeeper));
    }

    /** null significa que el modelo no aporta esta información; no equivale a false. */
    public GoalDetails getGoalDetails(Goal goal) { return goals.get(goal); }

    /**
     * Calcula minutos a partir de la formación conservada, cambios y expulsiones.
     * Una entrada en el minuto 90 cuenta como participación con cero minutos.
     * Sin titulares registrados sólo se conocen los minutos de los suplentes.
     * Una lesión no acredita por sí sola una salida en el modelo existente.
     */
    public Map<Player, Integer> getPlayerMinutes(Match match) {
        Map<Player, Integer> minutes = new LinkedHashMap<>();
        Map<Player, Integer> entered = new LinkedHashMap<>();
        homeStarters.forEach(player -> entered.put(player, 0));
        awayStarters.forEach(player -> entered.put(player, 0));
        entered.keySet().forEach(player -> minutes.put(player, 0));
        int duration = match.isExtraTimePlayed() ? 120 : 90;

        for (Event event : match.getEvents().stream().sorted(Comparator.comparingInt(Event::getMinute)).toList()) {
            int minute = Math.max(0, Math.min(duration, event.getMinute()));
            if (event instanceof Substitution substitution) {
                finishParticipation(substitution.getPlayerOut(), minute, entered, minutes);
                entered.put(substitution.getPlayerIn(), minute);
                minutes.putIfAbsent(substitution.getPlayerIn(), 0);
            } else if (event instanceof RedCard) {
                finishParticipation(event.getPlayer(), minute, entered, minutes);
            }
        }
        entered.forEach((player, start) -> minutes.merge(player, duration - start, Integer::sum));
        return Map.copyOf(minutes);
    }

    private static void finishParticipation(Player player, int minute,
                                            Map<Player, Integer> entered, Map<Player, Integer> minutes) {
        Integer start = entered.remove(player);
        if (start != null) minutes.merge(player, minute - start, Integer::sum);
    }
}
