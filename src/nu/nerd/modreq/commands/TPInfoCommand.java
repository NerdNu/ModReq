package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the in-game command /tpinfo.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class TPInfoCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private CompletableFuture<Void> future;
    private TPIdCommand tpIdCommand;
    private CheckCommand checkCommand;

    /**
     * Creates a new {@code TPInfoCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public TPInfoCommand(ModReq plugin, TPIdCommand tpIdCommand, CheckCommand checkCommand) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.future = plugin.getCompleteableFuture();
        this.tpIdCommand = tpIdCommand;
        this.checkCommand = checkCommand;
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {
        tpIdCommand.execute(player, name, args);
        checkCommand.execute(player, name, args);
        return true;
    }
}
