package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nu.nerd.modreq.utils.MessageUtils.messageMods;
import static nu.nerd.modreq.utils.RequestUtils.getRequest;

/**
 * Represents the in-game command /unclaim.
 * This class provides methods to process the data required by this command.
 *
 * @version 1.0
 * @since 3.0
 */
public class UnclaimCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private CompletableFuture<Void> future;
    private Map<UUID, Integer> claimedIds;

    /**
     * Creates a new {@code UnclaimCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public UnclaimCommand(ModReq plugin) {
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
        boolean isAdmin = player.hasPermission("modreq.cleardb");

        getRequest(player, playerUUID, args[0], true, claimedIds, reqTable, environment,
                configuration, plugin).thenAcceptAsync(request -> {
            if (request == null) {
                return;
            }

            if (request.getStatus() == Request.RequestStatus.CLAIMED &&
                    (isAdmin || request.getAssignedModUUID().equals(playerUUID))) {

                request.setStatus(Request.RequestStatus.OPEN);
                request.setAssignedModUUID(null);
                request.setAssignedMod(null);
                reqTable.save(request);

                environment.put("mod", player.getName());
                environment.put("request_id", String.valueOf(request.getId()));
                messageMods(configuration.MOD__UNCLAIM, environment, configuration);
                environment.remove("mod");
                environment.remove("request_id");
            }
        });
        return true;
    }
}
