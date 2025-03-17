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

import static nu.nerd.modreq.utils.MessageUtils.*;
import static nu.nerd.modreq.utils.MessageUtils.sendMessage;
import static nu.nerd.modreq.utils.RequestUtils.getRequest;
import static org.bukkit.Bukkit.getServer;

/**
 * Represents the in-game command /done.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class DoneCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private Map<UUID, Integer> claimedIds;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    /**
     * Creates a new {@code DoneCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public DoneCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.claimedIds = plugin.getClaimedIds();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {

        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();

        getRequest(player, playerUUID, args[0], true, claimedIds, reqTable, environment,
                configuration, plugin).thenAcceptAsync(request -> {
                    bukkitScheduler.runTask(plugin, () -> {
                        if (request == null) {
                            return;
                        }
                        int requestId = request.getId();

                        String doneMessage = concatArgs(args, 1);
                        boolean successfullyClosed = true;

                        if (request.getStatus() == Request.RequestStatus.CLOSED) {
                            sendMessage(player, configuration.MOD__ALREADY_CLOSED, environment, configuration);
                            successfullyClosed = false;
                        } else {
                            // Moderator doing /done.
                            if (player.hasPermission("modreq.done")) {
                                environment.put("mod", playerName);
                                environment.put("request_id", String.valueOf(requestId));
                                messageMods(configuration.MOD__COMPLETED, environment, configuration);
                                environment.remove("request_id");
                                environment.remove("mod");

                                if (!doneMessage.isEmpty()) {
                                    environment.put("close_message", doneMessage);
                                    messageMods(configuration.MOD__COMPLETED_MESSAGE, environment, configuration);
                                    environment.remove("close_message");
                                }
                            } else {
                                // Player doing /done.
                                if (request.getPlayerUUID() != null && !request.getPlayerUUID().equals(playerUUID)) {
                                    sendMessage(player, configuration.GENERAL__CLOSE_ERROR, environment, configuration);
                                    successfullyClosed = false;
                                }
                            }
                        }

                        if (successfullyClosed) {
                            request.setStatus(Request.RequestStatus.CLOSED);
                            request.setCloseTime(System.currentTimeMillis());
                            request.setCloseMessage(doneMessage);
                            request.setAssignedModUUID(playerUUID);
                            request.setAssignedMod(playerName);

                            Player requestCreator = getServer().getPlayerExact(request.getPlayerName());
                            if (requestCreator != null) {
                                // Message request creator immediately if online.
                                request.setCloseSeenByUser(true);

                                if (requestCreator.getUniqueId().equals(playerUUID)) {
                                    // Request closed by player.
                                    if (!player.hasPermission("modreq.done")) {
                                        environment.put("request_id", String.valueOf(requestId));
                                        messageMods(configuration.MOD__DELETED, environment, configuration);
                                        sendMessage(player, configuration.GENERAL__DELETED, environment, configuration);
                                        environment.remove("request_id");
                                    }
                                } else {
                                    // Request closed by moderator.
                                    environment.put("close_message", doneMessage);
                                    environment.put("mod", playerName);
                                    environment.put("request_id", String.valueOf(requestId));
                                    if (!doneMessage.isEmpty()) {
                                        sendMessage(requestCreator, configuration.GENERAL__COMPLETED_MESSAGE,
                                                environment, configuration);
                                    } else {
                                        sendMessage(requestCreator, configuration.GENERAL__COMPLETED, environment,
                                                configuration);
                                    }
                                    environment.remove("close_message");
                                    environment.remove("mod");
                                    environment.remove("request_id");
                                }
                            }
                            reqTable.save(request);
                        }
                    });
                });
        return true;
    }
}
