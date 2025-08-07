package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nu.nerd.modreq.utils.MessageUtils.messageMods;
import static nu.nerd.modreq.utils.RequestUtils.getRequest;

/**
 * Represents the in-game command /reopen.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class ReopenCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private Map<UUID, Integer> claimedIds;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    /**
     * Creates a new {@code ReopenCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public ReopenCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.claimedIds = plugin.getClaimedIds();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {

        UUID playerUUID = player.getUniqueId();

        getRequest(player, playerUUID, args[0], true, claimedIds, reqTable, environment,
                configuration, plugin).thenAcceptAsync(request -> {
            if (request == null) {
                return;
            }

            if (request.getStatus() == Request.RequestStatus.CLOSED
                    || (request.getStatus() == Request.RequestStatus.CLAIMED && request.getAssignedModUUID()
                    .equals(playerUUID))) {
                request.setStatus(Request.RequestStatus.OPEN);
                request.setAssignedModUUID(null);
                request.setAssignedMod(null);
                request.setCloseSeenByUser(false);
                reqTable.save(request);

                bukkitScheduler.runTask(plugin, () -> {
                    environment.put("mod", player.getName());
                    environment.put("request_id", String.valueOf(request.getId()));

                    messageMods(configuration.MOD__REOPENED, environment, configuration);

                    environment.remove("mod");
                    environment.remove("request_id");
                });
            }
        });
        return true;
    }
}
