package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nu.nerd.modreq.utils.MessageUtils.messageMods;
import static nu.nerd.modreq.utils.MessageUtils.sendMessage;
import static nu.nerd.modreq.utils.RequestUtils.getRequest;

/**
 * Represents the in-game command /claim.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class ClaimCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private CompletableFuture<Void> future;
    private Map<UUID, Integer> claimedIds;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    /**
     * Creates a new {@code ClaimCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public ClaimCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.future = plugin.getCompleteableFuture();
        this.claimedIds = plugin.getClaimedIds();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {

        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();

        getRequest(player, playerUUID, args[0], false, claimedIds,
                reqTable, environment, configuration, plugin).thenAcceptAsync(request -> {
            if (request == null) {
                return;
            }
            int requestId = request.getId();
            Request.RequestStatus status = request.getStatus();

            bukkitScheduler.runTask(plugin, () -> {
                if (status == Request.RequestStatus.OPEN) {
                    request.setStatus(Request.RequestStatus.CLAIMED);
                    request.setAssignedModUUID(playerUUID);
                    request.setAssignedMod(playerName);
                    reqTable.save(request);

                    environment.put("mod", playerName);
                    environment.put("request_id", String.valueOf(requestId));
                    messageMods(configuration.MOD__REQUEST_TAKEN, environment, configuration);
                    environment.remove("mod");
                    environment.remove("request_id");
                    claimedIds.put(playerUUID, requestId);

                } else if (status == Request.RequestStatus.CLOSED) {
                    sendMessage(player, configuration.MOD__ALREADY_CLOSED, environment, configuration);

                } else if (status == Request.RequestStatus.CLAIMED) {
                    if (request.getAssignedModUUID().equals(playerUUID)) {
                        // Already claimed by command sender. Update most recent claim.
                        claimedIds.put(playerUUID, requestId);
                    } else {
                        sendMessage(player, configuration.MOD__ALREADY_CLAIMED, environment, configuration);
                    }
                }
            });
        });
        return true;
    }
}
