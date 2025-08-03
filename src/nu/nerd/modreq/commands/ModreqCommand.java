package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static nu.nerd.modreq.utils.MessageUtils.*;

/**
 * Represents the in-game command /modreq.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class ModreqCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    /**
     * Creates a new {@code ModreqCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public ModreqCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {

        if(args.length > 0) {
            reqTable.getNumRequestFromUser(player.getUniqueId()).thenAccept(numRequests -> {
                if (numRequests < configuration.MAX_REQUESTS) {
                    Request req = new Request();
                    req.setPlayerUUID(player.getUniqueId());
                    req.setPlayerName(player.getName());
                    String r = ChatColor.translateAlternateColorCodes('&', concatArgs(args, 0));
                    r = ChatColor.stripColor(r);
                    req.setRequest(r);
                    req.setRequestTime(System.currentTimeMillis());
                    String location = String.format("%s,%f,%f,%f,%f,%f",
                            player.getWorld().getName(),
                            player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ(),
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch());
                    req.setRequestLocation(location);
                    req.setStatus(Request.RequestStatus.OPEN);

                    reqTable.saveAndGetId(req).thenAccept(savedReq -> bukkitScheduler.runTask(plugin, () -> {
                        environment.put("request_id", String.valueOf(req.getId()));
                        messageMods(configuration.MOD__NEW_REQUEST, environment, configuration);
                        sendMessage(player, configuration.GENERAL__REQUEST_FILED, environment, configuration);
                    }));
                } else {
                    bukkitScheduler.runTask(plugin, () -> {
                        environment.put("max_requests", Integer.toString(configuration.MAX_REQUESTS));
                        sendMessage(player, configuration.GENERAL__MAX_REQUESTS, environment, configuration);
                    });
                }
            });
            return true;
        } else {
            return false;
        }
    }
}
