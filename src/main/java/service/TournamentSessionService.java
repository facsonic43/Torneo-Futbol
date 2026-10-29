package service;

import dao.StadiumDAO;
import main.loader.TournamentData;
import main.loader.TournamentDataLoader;
import model.match.Stadium;
import model.tournament.Tournament;
import model.tournament.TournamentStage;
import persistence.TournamentPersistenceService;

import java.util.List;

/*
 * Mantiene una única sesión activa del campeonato.
 * Centraliza el torneo actual, los estadios disponibles
 * y la persistencia.
 */
public class TournamentSessionService {

    private Tournament tournament;
    private List<Stadium> stadiums;

    private TournamentPersistenceService persistenceService;

    private final String saveFile;
    private boolean loadedFromSave;

    public TournamentSessionService() {

        persistenceService =
                new TournamentPersistenceService();

        saveFile =
                "saves/tournament.dat";
    }

    public Tournament createNewTournament()
            throws Exception {

        refreshStadiums();

        TournamentDataLoader loader =
                new TournamentDataLoader();

        TournamentData data =
                loader.load(
                        "torneo.json"
                );

        Tournament newTournament =
                new Tournament(
                        data.getTeams(),
                        data.getReferees()
                );

        newTournament.drawGroups();

        newTournament.generateGroupMatches(
                stadiums
        );

        // 2. Guardamos la referencia en el atributo de sesión
        this.tournament = newTournament;

        this.loadedFromSave = false;

        return newTournament;
    }

    /*
     * Vuelve a consultar PostgreSQL después de modificar
     * los estadios desde la interfaz.
     */
    public void refreshStadiums()
            throws Exception {

        StadiumDAO stadiumDAO =
                new StadiumDAO();

        stadiums =
                stadiumDAO.findAll();

        if (stadiums.size()
                < 13) {

            throw new IllegalStateException(
                    "At least 13 stadiums are required for the knockout stage."
            );
        }
    }

    public void save()
            throws Exception {

        persistenceService
                .saveTournament(
                        tournament,
                        saveFile
                );
    }

    public Tournament load()
            throws Exception {

        if (!persistenceService
                .saveExists(
                        saveFile
                )) {

            throw new IllegalStateException(
                    "There is no saved tournament."
            );
        }

        tournament =
                persistenceService
                        .loadTournament(
                                saveFile
                        );

        loadedFromSave = true;

        return tournament;
    }

    /** True when the current tournament was restored from the save file, false when it is new. */
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

        return persistenceService
                .getStage(
                        tournament
                );
    }

    public boolean saveExists() {

        return persistenceService
                .saveExists(
                        saveFile
                );
    }

}
