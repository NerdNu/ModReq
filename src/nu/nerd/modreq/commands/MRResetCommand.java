package nu.nerd.modreq.commands;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Note;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents the in-game command /mr-reset.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class MRResetCommand implements CommandHandler {

    private final ModReq plugin;
    ComponentLogger logger;
    String DATABASE_URL;

    /**
     * Creates a new {@code MRResetCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public MRResetCommand(ModReq plugin) {
        this.plugin = plugin;
        logger = plugin.getComponentLogger();
        this.DATABASE_URL = plugin.getDatabaseUrl();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {
        logger.debug(Component.text("Resetting database..."), Level.INFO);

        try {
            ConnectionSource connectionSource = new JdbcPooledConnectionSource(DATABASE_URL);
            Dao requestDao = DaoManager.createDao(connectionSource, Request.class);
            Dao noteDao = DaoManager.createDao(connectionSource, Note.class);

            List<Request> requests = requestDao.queryForAll();
            List<Note> notes = noteDao.queryForAll();

            TableUtils.dropTable(connectionSource, Request.class, true);
            TableUtils.dropTable(connectionSource, Note.class, true);
            plugin.setupDatabase();

            logger.debug(Component.text("Restoring " + requests.size() + " requests and "
                    + notes.size() + "notes."), Level.INFO);
            for (Request request : requests) {
                requestDao.create(request);
            }
            for (Note note : notes) {
                noteDao.create(note);
            }

        } catch (SQLException exception) {
            logger.debug(Component.text("Database reset failed!"), Level.SEVERE);
            return false;
        }
        logger.debug(Component.text("Done resetting database!"), Level.INFO);
        return true;
    }
}
