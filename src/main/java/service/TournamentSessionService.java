package service;

import dao.StadiumDAO;
import exceptions.InsufficientStadiumsException;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.Stadium;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import persistence.TournamentPersistenceService;

import java.util.List;

// Mantiene el torneo activo, consulta los estadios disponibles y coordina la creación,
// la carga y el guardado de la sesión. También informa si la sesión proviene de una partida guardada.

public class TournamentSessionService {
    private Tournament tournament;
    private List<Stadium> stadiums;

    private TournamentPersistenceService persistenceService;

    private final String saveFile;
    private boolean loadedFromSave;

    public TournamentSessionService() {
        persistenceService = new TournamentPersistenceService();
        saveFile = "saves/tournament.dat";
    }

    public Tournament createNewTournament() throws Exception {
        refreshStadiums();
        TournamentDataLoader loader = new TournamentDataLoader();
        TournamentData data = loader.load("torneo.json");
        Tournament newTournament = new Tournament(data.getTeams(), data.getReferees());
        newTournament.drawGroups();
        newTournament.generateGroupMatches(stadiums);
        this.tournament = newTournament;
        this.loadedFromSave = false;
        return newTournament;
    }

    public void refreshStadiums() throws Exception {
        StadiumDAO stadiumDAO = new StadiumDAO();
        stadiums = stadiumDAO.findAll();
        if (stadiums.size() < 13) {
            throw new InsufficientStadiumsException(13, stadiums.size());
        }
    }

    public void save() throws Exception {
        persistenceService.saveTournament(tournament, saveFile);
    }

    public Tournament load() throws Exception {
        if (!persistenceService.saveExists(saveFile)) {
            throw new IllegalStateException("There is no saved tournament.");
        }
        tournament = persistenceService.loadTournament(saveFile);
        loadedFromSave = true;
        return tournament;
    }

    public boolean isLoadedFromSave() {
        return loadedFromSave;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public List<Stadium> getStadiums() {
        return stadiums;
    }

    public TournamentStage getStage() {
        return persistenceService.getStage(tournament);
    }

    public boolean saveExists() {
        return persistenceService.saveExists(saveFile);
    }
}
