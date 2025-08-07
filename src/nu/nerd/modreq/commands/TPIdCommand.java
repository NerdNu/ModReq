package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nu.nerd.modreq.utils.DataUtils.stringToLocation;
import static nu.nerd.modreq.utils.MessageUtils.sendMessage;
import static nu.nerd.modreq.utils.RequestUtils.getRequest;

/**
 * Represents the in-game command /tp-id.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class TPIdCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private Map<UUID, Integer> claimedIds;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    /**
     * Creates a new {@code TPIdCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public TPIdCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.claimedIds = plugin.getClaimedIds();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {

        UUID playerUUID = player.getUniqueId();
        Map<String, String> reqEnvironment = new HashMap<>();

        getRequest(player, playerUUID, args[0], true, claimedIds, reqTable, environment,
                configuration, plugin).thenAcceptAsync(request -> {
            if (request == null) {
                return;
            }

            reqEnvironment.put("request_id", String.valueOf(request.getId()));
            bukkitScheduler.runTask(plugin, () -> {
                sendMessage(player, configuration.MOD__TELEPORT, reqEnvironment, configuration);
                Location loc = stringToLocation(request.getRequestLocation());
                player.teleport(loc);
            });
        });
        return true;
    }
}
