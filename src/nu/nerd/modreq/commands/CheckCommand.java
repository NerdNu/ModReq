package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static nu.nerd.modreq.utils.MessageUtils.*;

/**
 * Represents the in-game command /check.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class CheckCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();
    private Map<UUID, Integer> claimedIds;

    /**
     * Creates a new {@code CheckCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public CheckCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.claimedIds = plugin.getClaimedIds();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {
        int page = 1;
        int requestId = 0;
        String searchTerm = null;
        UUID limitUUID = null;
        boolean showNotes = true;
        boolean includeElevated = player.hasPermission("modreq.cleardb");

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // If the player wants to see admin (elevated) modreqs
            if (arg.equalsIgnoreCase("--admin") || arg.equalsIgnoreCase("-a")) {
                includeElevated = true;

                // If the player wants to specify a page (shorthand)
            } else if (arg.startsWith("p:")) {
                page = Integer.parseInt(arg.substring(2));

                // If the player wants to specify a page
            } else if (arg.equalsIgnoreCase("--page") || arg.equalsIgnoreCase("-p")) {
                if (i + 1 >= args.length) {
                    sendMessage(player, configuration.GENERAL__PAGE_ERROR, environment, configuration);
                    return true;
                } else {
                    try {
                        page = Integer.parseInt(args[i + 1]);
                        i++;
                    } catch (NumberFormatException exception) {
                        sendMessage(player, configuration.GENERAL__PAGE_ERROR, environment, configuration);
                        return true;
                    }
                }

                // If the player wants to search modreqs for a certain keyword
            } else if (arg.equalsIgnoreCase("--search") || arg.equalsIgnoreCase("-s")) {
                if (i + 1 < args.length) {
                    searchTerm = args[i + 1];
                    i++;
                } else {
                    sendMessage(player, configuration.GENERAL__SEARCH_ERROR, environment, configuration);
                    return true;
                }

                // If the player wants to see the information on their current claimed modreq
            } else if (arg.equalsIgnoreCase("-")) {
                Integer claimedId = claimedIds.get(player.getUniqueId());
                if (claimedId != null) {
                    requestId = claimedId;
                    page = 0;
                }

                // Handle the argument as an integer and grab a specific modreq
            } else {
                try {
                    requestId = Integer.parseInt(arg);
                    page = 0;
                } catch (NumberFormatException exception) {
                    sendMessage(player, configuration.GENERAL__REQUEST_NUMBER, environment, configuration);
                    return true;
                }
            }
        }

        if (!player.hasPermission("modreq.check")) {
            limitUUID = player.getUniqueId();
            showNotes = false;
        }

        final UUID finalLimitUUID = limitUUID;
        final boolean finalShowNotes = showNotes;
        final int finalPage = page;
        final int finalRequestId = requestId;
        final boolean finalIncludeElevated = includeElevated;
        final String finalSearchTerm = searchTerm;
        CompletableFuture<Void> future;

        if (page > 0) {
            if (limitUUID != null) {
                future = reqTable.getUserRequests(limitUUID).thenAccept(requests -> {
                    bukkitScheduler.runTask(plugin, () -> {
                        if (requests.isEmpty()) {
                            sendMessage(player, configuration.GENERAL__NO_REQUESTS, environment, configuration);
                        } else {
                            messageRequestListToPlayer(player, requests, finalPage, requests.size(),
                                    true, environment, configuration, plugin, plugin.getNoteTable());
                        }
                    });
                });
            } else {

                CompletableFuture<List<Request>> requestsFuture = reqTable.getRequestPage(
                        page - 1,
                        configuration.PAGE_SIZE,
                        includeElevated,
                        searchTerm,
                        Request.RequestStatus.OPEN,
                        Request.RequestStatus.CLAIMED
                );

                CompletableFuture<Integer> countFuture = reqTable.getTotalRequest(
                        finalIncludeElevated,
                        finalSearchTerm,
                        Request.RequestStatus.OPEN,
                        Request.RequestStatus.CLAIMED
                );

                future = CompletableFuture.allOf(requestsFuture, countFuture).thenAccept(ignore -> {
                    List<Request> requests = requestsFuture.join();
                    int totalCount = countFuture.join();

                    bukkitScheduler.runTask(plugin, () -> {
                        if (totalCount == 0) {
                            sendMessage(player, configuration.GENERAL__NO_REQUESTS, environment, configuration);
                        } else if (finalPage > 1 && requests.isEmpty()) {
                            sendMessage(player, configuration.MOD__EMPTY_PAGE, environment, configuration);
                        } else {
                            messageRequestListToPlayer(player, requests, finalPage, totalCount,
                                    true, environment, configuration, plugin, plugin.getNoteTable());
                        }
                    });

                });

            }
        } else if (requestId > 0) {
            future = reqTable.getRequest(requestId).thenAccept(request -> {
                bukkitScheduler.runTask(plugin, () -> {
                    if (request == null) {
                        // Request doesn't exist
                        sendMessage(player, configuration.GENERAL__REQUEST_ERROR, environment, configuration);
                    } else if (finalLimitUUID != null && !request.getPlayerUUID().equals(finalLimitUUID)) {
                        // Request exists but player doesn't have permission to view it
                        sendMessage(player, configuration.GENERAL__REQUEST_ERROR, environment, configuration);
                    } else {
                        // Show the request details
                        messageRequestToPlayer(player, request, finalShowNotes, environment,
                                configuration, plugin, plugin.getNoteTable());
                    }
                });
            });
        } else {
            future = CompletableFuture.completedFuture(null);

            bukkitScheduler.runTask(plugin, () ->
                    sendMessage(player, configuration.GENERAL__NO_REQUESTS, environment, configuration));
        }
        return true;
    }
}
