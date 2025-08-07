package nu.nerd.modreq.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static nu.nerd.modreq.utils.MessageUtils.sendMessage;

public final class RequestUtils {

    private RequestUtils() {}

    /**
     * Parse the specified command argument (usually args[0] in onCommand()) as an integer request ID and return the
     * corresponding Request.
     * <p>
     * If useLastClaimedId is true, an ID of "-" is accepted as a reference to the most recently claimed request
     * <p>
     * Errors are messaged to the command sender in the case of a malformed request ID, or if the ID does not correspond
     * to a database entry.
     *
     * @param arg              the command argument to parse.
     * @param senderUUID       UUID of the CommandSender.
     * @param useLastClaimedId if true, then an arg of "-" is considered to be a synonym for the ID of the most recently
     *                         claimed request.
     * @return the corresponding Request, or null if not found.
     */
    public static CompletableFuture<Request> getRequest(
            CommandSender sender,
            UUID senderUUID, String arg,
            boolean useLastClaimedId,
            Map<UUID, Integer> claimedIds,
            RequestTable reqTable,
            Map<String, String> environment,
            Configuration configuration,
            ModReq plugin
    ) {

        int requestId = 0;

        if (arg.equals("-")) {
            if (useLastClaimedId) {
                Integer claimedId = claimedIds.get(senderUUID);
                if (claimedId != null) {
                    requestId = claimedId;
                }
            }
        } else {
            try {
                requestId = Integer.parseInt(arg);
            } catch (NumberFormatException ex) {
            }
        }

        if (requestId == 0) {
            sendMessage(sender, configuration.GENERAL__REQUEST_NUMBER, environment, configuration);
            return null;
        }

        return reqTable.getRequest(requestId).thenCompose(request -> {
            if (request == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sendMessage(sender, configuration.GENERAL__REQUEST_ERROR, environment, configuration);
                });
            }
            return CompletableFuture.completedFuture(request);
        });
    }

    public static void loadClaimedIds(Map<UUID, Integer> claimedIds, File claimsFile, ComponentLogger logger) {
        YamlConfiguration ymlConfiguration = new YamlConfiguration();
        try {
            if (claimsFile.isFile()) {
                ymlConfiguration.load(claimsFile);
                for (String uuidString : ymlConfiguration.getKeys(false)) {
                    try {
                        claimedIds.put(UUID.fromString(uuidString), ymlConfiguration.getInt(uuidString));
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            logger.debug(Component.text("Cannot read " + claimsFile.getPath()));
        } catch (IOException ex) {
            logger.debug(Component.text("Error reading " + claimsFile.getPath()));
        } catch (InvalidConfigurationException ex) {
            logger.debug(Component.text("Cannot parse " + claimsFile.getPath()));
        }
    }

    public static void saveClaimedIds(Map<UUID, Integer> claimedIds, File claimsFile, ComponentLogger logger) {
        YamlConfiguration ymlConfiguration = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> claim : claimedIds.entrySet()) {
            ymlConfiguration.set(claim.getKey().toString(), claim.getValue());
        }

        try {
            ymlConfiguration.save(claimsFile);
        } catch (IOException ex) {
            logger.debug(Component.text("Most recently claimed requests could not be saved in " + claimsFile.getPath()));
        }
    }

}
