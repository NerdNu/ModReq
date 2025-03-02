package nu.nerd.modreq;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import nu.nerd.modreq.commands.*;
import nu.nerd.modreq.database.Note;
import nu.nerd.modreq.database.NoteTable;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static nu.nerd.modreq.utils.DataUtils.stringToLocation;
import static nu.nerd.modreq.utils.DataUtils.timestampToDateString;
import static nu.nerd.modreq.utils.MessageUtils.*;
import static nu.nerd.modreq.utils.RequestUtils.*;

public class ModReq extends JavaPlugin {

    /**
     * The JDBC URL for the SQLite DB.
     */
    private static final String DATABASE_URL = "jdbc:sqlite:plugins/ModReq/modreq.db";
    /**
     * The ConnectionSource for database transactions.
     */
    private ConnectionSource connectionSource;
    /**
     * The DAO object representing the Request object.
     */
    private Dao<Request, Integer> requestDao;
    /**
     * The DAO object representing the Note object.
     */
    private Dao<Note, Integer> noteDao;
    /**
     * Idk lol!
     */
    private CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    /**
     * The logger for outputting to the console.
     */
    private ComponentLogger componentLogger = this.getComponentLogger();
    /**
     * Map from UUID of staff member to integer ID of most recently claimed request.
     * <p>
     * This acts as the default request ID when handling "/check -", "/tp-id -" and "/tpinfo -", and synonyms.
     * <p>
     * This collection is saved in most-recent-claims.yml across restarts.
     */
    private Map<UUID, Integer> claimedIds = new HashMap<>();
    /**
     * The file where recent claims are placed to persist through restarts.
     */
    private File claimsFile;
    /**
     * An instance of the RequestTable class. Used for database transactions involving the Request object.
     */
    private RequestTable reqTable;
    /**
     * An instance of the NoteTable class. Used for database transactions involving the Note object.
     */
    private NoteTable noteTable;
    private ModReqListener listener;
    private Configuration config = new Configuration(this);
    private Map<String, String> environment = new HashMap<>();

    // Commands
    CommandRegistry registry;
    CheckCommand checkCommand;
    ClaimCommand claimCommand;
    DoneCommand doneCommand;
    ModreqCommand modreqCommand;
    MRNoteCommand mrNoteCommand;
    MRResetCommand mrResetCommand;
    ReopenCommand reopenCommand;
    TPClaimCommand tpClaimCommand;
    TPIdCommand tpIdCommand;
    UnclaimCommand unclaimCommand;
    ElevateCommand elevateCommand;
    TPInfoCommand tpInfoCommand;



    @Override
    public void onEnable() {
        setupDatabase();
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }

        config.load();

        claimsFile = new File(getDataFolder(), "most-recent-claims.yml");
        loadClaimedIds(claimedIds, claimsFile, componentLogger);

        reqTable = new RequestTable(this);
        noteTable = new NoteTable(this);
        listener = new ModReqListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // Command Initialization
        registry = new CommandRegistry();
        checkCommand = new CheckCommand(this);
        claimCommand = new ClaimCommand(this);
        doneCommand = new DoneCommand(this);
        modreqCommand = new ModreqCommand(this);
        mrNoteCommand = new MRNoteCommand(this);
        mrResetCommand = new MRResetCommand(this);
        reopenCommand = new ReopenCommand(this);
        tpIdCommand = new TPIdCommand(this);
        unclaimCommand = new UnclaimCommand(this);
        elevateCommand = new ElevateCommand(this, unclaimCommand);
        tpInfoCommand = new TPInfoCommand(this, tpIdCommand, checkCommand);
        tpClaimCommand = new TPClaimCommand(this, claimCommand, tpIdCommand, checkCommand);

        // Command Registration
        registry.register("check", checkCommand);
        registry.register("claim", claimCommand);
        registry.register("done", doneCommand);
        registry.register("elevate", elevateCommand);
        registry.register("modreq", modreqCommand);
        registry.register("mr-note", mrNoteCommand);
        registry.register("mr-reset", mrResetCommand);
        registry.register("reopen", reopenCommand);
        registry.register("tpc", tpClaimCommand);
        registry.register("tp-id", tpIdCommand);
        registry.register("tpinfo", tpInfoCommand);
        registry.register("unclaim", unclaimCommand);
    }

    @Override
    public void onDisable() {
        saveClaimedIds(claimedIds, claimsFile, componentLogger);
    }

    public void setupDatabase() {
        try {
            connectionSource = new JdbcPooledConnectionSource(DATABASE_URL);
            TableUtils.createTableIfNotExists(connectionSource, Request.class);
            TableUtils.createTableIfNotExists(connectionSource, Note.class);

            requestDao = DaoManager.createDao(connectionSource, Request.class);
            noteDao = DaoManager.createDao(connectionSource, Note.class);

        } catch (SQLException ex) {
            getLogger().log(Level.INFO, "First run, please initialize database manually. Run the following:");
            getLogger().log(Level.INFO, """
                                       CREATE TABLE modreq_notes (
                    id                        integer primary key,
                    player_uuid               varchar(40) not null,
                    player                    varchar(255) not null,
                    request_id                integer not null,
                    note_body                 varchar(255)
                                       );
                    """);
            getLogger().log(Level.INFO, """
                                       CREATE TABLE modreq_requests (
                    id                        integer primary key,
                    player_uuid               varchar(40) not null,
                    player_name               varchar(255) not null,
                    assigned_mod_uuid         varchar(40),
                    assigned_mod              varchar(255),
                    request                   varchar(255),
                    request_time              bigint not null,
                    status                    integer not null,
                    request_location          varchar(255) not null,
                    close_message             varchar(255),
                    close_time                bigint,
                    close_seen_by_user        integer(1),
                    flag_for_admin            integer(1),
                    constraint ck_modreq_requests_status check (status in (0,1,2))
                                       );
                    """);
        }

    }

    public Dao<Request, Integer> getRequestDao() {
        return requestDao;
    }

    public Dao<Note, Integer> getNoteDao() {
        return noteDao;
    }

    public RequestTable getReqTable() {
        return reqTable;
    }

    public NoteTable getNoteTable() {
        return noteTable;
    }

    /**
     * The getter for the plugin's config instance
     * @return An instance of the plugin's configuration
     */
    public Configuration getConfiguration() {
        return config;
    }

    /**
     * The getter for the plugin's environment variables
     * @return An instance of the plugin's environment variables
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * The getter for the plugin's CompleteableFuture instance
     * @return An instance of the plugin's CompleteableFuture
     */
    public CompletableFuture<Void> getCompleteableFuture() {
        return future;
    }

    /**
     * The getter for the plugin's claimedIds hashmap
     * @return An instance of the plugin's claimedIds hashmap
     */
    public Map<UUID, Integer> getClaimedIds() {
        return claimedIds;
    }

    /**
     * The getter for the plugin's DATABASE_URL instance. This is a JDBC URL for database connectivity
     * @return The plugin's DATABASE_URL instance
     */
    public String getDatabaseUrl() {
        return DATABASE_URL;
    }

    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if(!(sender instanceof Player)) {
            return false;
        }
        CommandHandler handler = registry.getHandler(command.getName());
        if(handler != null) {
            environment.clear();
            return handler.execute((Player) sender, name, args);
        }
        return false;
    }
}