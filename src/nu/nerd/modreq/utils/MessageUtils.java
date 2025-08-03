package nu.nerd.modreq.utils;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Note;
import nu.nerd.modreq.database.NoteTable;
import nu.nerd.modreq.database.Request;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static nu.nerd.modreq.utils.DataUtils.stringToLocation;
import static nu.nerd.modreq.utils.DataUtils.timestampToDateString;
import static org.bukkit.Bukkit.getServer;

public final class MessageUtils {

    private MessageUtils() {
    }

    public static void sendMessage(CommandSender sender, String message, Map<String, String> environment, Configuration configuration) {
        message = buildMessage(message, environment, configuration);
        sender.sendMessage(message);
    }

    public static void messageMods(String message, Map<String, String> environment, Configuration configuration) {
        String permission = "modreq.notice";
        message = buildMessage(message, environment, configuration);
        getServer().broadcast(message, permission);

        Set<Permissible> subs = getServer().getPluginManager().getPermissionSubscriptions(permission);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission(permission) && !subs.contains(player)) {
                player.sendMessage(message);
            }
        }
    }

    public static String concatArgs(String[] args, int first) {
        StringBuilder builder = new StringBuilder();
        String sep = "";
        for (int i = first; i < args.length; ++i) {
            builder.append(sep).append(args[i]);
            sep = " ";
        }
        return builder.toString();
    }

    public static String buildMessage(String inputMessage, Map<String, String> environment, Configuration configuration) {
        String message = inputMessage;

        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equalsIgnoreCase("player")) {
                if (getServer().getPlayerExact(value) != null) {
                    value = configuration.COLOUR_ONLINE + value;
                } else {
                    value = configuration.COLOUR_OFFLINE + value;
                }
            }
            message = message.replace("{" + key + "}", value);
        }
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    public static void messageRequestListToPlayer(
            CommandSender sender,
            List<Request> reqs,
            int page,
            int totalRequests,
            boolean showPage,
            Map<String, String> environment,
            Configuration configuration,
            ModReq plugin,
            NoteTable noteTable
    ) {
        String[] requestMessages = new String[reqs.size()];
        AtomicInteger completedCount = new AtomicInteger(0);

        Map<String, String> headerEnv = new HashMap<>(environment);
        headerEnv.put("num_requests", String.valueOf(totalRequests));
        sender.sendMessage(buildMessage(configuration.GENERAL__LIST__HEADER, headerEnv, configuration));

        if (reqs.isEmpty()) {
            if (showPage) {
                sendFooter(sender, environment, page, totalRequests, configuration);
            }
            return;
        }

        for (int i = 0; i < reqs.size(); i++) {
            final Request request = reqs.get(i);
            final int index = i;

            noteTable.getNoteCount(request).thenAccept(noteCount -> {
                try {
                    Map<String, String> requestEnvironment = new HashMap<>(environment);
                    requestEnvironment.put("request_id", String.valueOf(request.getId()));
                    requestEnvironment.put("note_count", noteCount > 0 ? ChatColor.RED + " [" + noteCount + "]" : "");
                    requestEnvironment.put("admin", (request.isFlagForAdmin() ? (ChatColor.AQUA + " [ADMIN]") : ""));
                    requestEnvironment.put("mod", (request.getStatus() == Request.RequestStatus.CLAIMED ? (request.getAssignedMod()) : ""));
                    requestEnvironment.put("status",
                            request.getStatus() == Request.RequestStatus.CLAIMED ? "" :
                            request.getStatus() == Request.RequestStatus.CLOSED ? "CLOSED by " + request.getAssignedMod() :
                            request.getStatus().toString());
                    requestEnvironment.put("time", timestampToDateString(request.getRequestTime(), configuration.DATE_FORMAT));
                    requestEnvironment.put("player", request.getPlayerName());
                    requestEnvironment.put("request_message", request.getRequest());

                    requestMessages[index] = buildMessage(configuration.GENERAL__LIST__ITEM, requestEnvironment, configuration);

                    if (completedCount.incrementAndGet() == reqs.size()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (String message : requestMessages) {
                                sender.sendMessage(message);
                            }

                            if (showPage) {
                                sendFooter(sender, environment, page, totalRequests, configuration);
                            }
                        });
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error processing request #" + request.getId() + ": " + e.getMessage());
                    e.printStackTrace();

                    requestMessages[index] = ChatColor.RED + "Error displaying request #" + request.getId();

                    if (completedCount.incrementAndGet() == reqs.size()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (String message : requestMessages) {
                                sender.sendMessage(message);
                            }

                            if (showPage) {
                                sendFooter(sender, environment, page, totalRequests, configuration);
                            }
                        });
                    }
                }
            });
        }
    }

    private static void sendFooter(
            CommandSender sender,
            Map<String, String> baseEnvironment,
            int page,
            int totalRequests,
            Configuration configuration
    ) {
        Map<String, String> footerEnv = new HashMap<>(baseEnvironment);
        int numpages = (int) Math.ceil(totalRequests / (float) configuration.PAGE_SIZE);
        footerEnv.put("page", String.valueOf(page));
        footerEnv.put("num_pages", String.valueOf(numpages));
        sender.sendMessage(buildMessage(configuration.GENERAL__LIST__FOOTER, footerEnv, configuration));
    }

    public static void messageRequestToPlayer(
            CommandSender sender,
            Request req,
            boolean showNotes,
            Map<String, String> environment,
            Configuration configuration,
            ModReq plugin,
            NoteTable noteTable
    ) {
        List<String> messages = new ArrayList<>();
        Location loc = stringToLocation(req.getRequestLocation());
        String location = String.format("%s, %d, %d, %d", loc.getWorld().getName(), Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));

        environment.put("status", req.getStatus().toString());
        environment.put("request_id", String.valueOf(req.getId()));
        if (req.getStatus() == Request.RequestStatus.CLAIMED) {
            environment.put("mod", req.getAssignedMod());
            messages.add(buildMessage(configuration.GENERAL__ITEM__HEADER_CLAIMED, environment, configuration));
            environment.remove("mod");
        } else {
            messages.add(buildMessage(configuration.GENERAL__ITEM__HEADER_UNCLAIMED, environment, configuration));
        }
        environment.remove("status");
        environment.remove("request_id");
        environment.put("player", req.getPlayerName());
        environment.put("time", timestampToDateString(req.getRequestTime(), configuration.DATE_FORMAT));
        environment.put("location", location);
        messages.add(buildMessage(configuration.GENERAL__ITEM__DETAILS, environment, configuration));
        environment.remove("player");
        environment.remove("time");
        environment.remove("location");
        environment.put("request_message", req.getRequest());
        messages.add(buildMessage(configuration.GENERAL__ITEM__REQUEST, environment, configuration));
        environment.remove("request_message");

        System.out.println("Show notes reached!");
        System.out.println("showNotes value: " + showNotes);
        if (showNotes) {
            noteTable.getRequestNotes(req).thenAcceptAsync(notes -> {
                int i = 1;

                for (Note note : notes) {
                    System.out.println(note.getNoteBody());
                    Map<String, String> noteEnvironment = new HashMap<>(environment);
                    noteEnvironment.put("id", Integer.toString(i));
                    noteEnvironment.put("user", note.getPlayer());
                    noteEnvironment.put("message", note.getNoteBody());
                    messages.add(buildMessage(configuration.GENERAL__ITEM__NOTE, noteEnvironment, configuration));
                    i++;
                }
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(messages.toArray(new String[0])));
            });
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(messages.toArray(new String[0])));
        }
    }
}
