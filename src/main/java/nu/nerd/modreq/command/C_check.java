/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.nerd.modreq.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Note;
import nu.nerd.modreq.database.Request;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author c45y
 */
public class C_check implements CommandExecutor {

    private final ModReq _plugin;

    public C_check(ModReq plugin) {
        _plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("check")) {
            return false;
        }

        _plugin.environment.clear();

        String senderName = ChatColor.stripColor(sender.getName());
        UUID senderUUID = null;
        String request = String.join(" ", args);
        if (sender instanceof Player) {
            senderUUID = ((Player) sender).getUniqueId();
        }
        if (sender instanceof ConsoleCommandSender) {
            senderName = "Console";
        }

        // Setting page > 0 triggers a page listing.
        int page = 1;
        int requestId = 0;
        int totalRequests = 0;
        String searchTerm = null;
        UUID limitUUID = null;
        boolean showNotes = true;
        boolean includeExternal = false;
        boolean includeElevated = sender.hasPermission("modreq.cleardb");

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--admin") || arg.equalsIgnoreCase("-a")) {
                includeElevated = true;
            } else if (arg.equalsIgnoreCase("--external")) {
                includeExternal = true;
            } else if (arg.startsWith("p:")) {
                page = Integer.parseInt(arg.substring(2));
            } else if (arg.equalsIgnoreCase("--page") || arg.equalsIgnoreCase("-p")) {
                if (i + 1 > args.length) {
                    _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__PAGE_ERROR);
                    return true;
                } else {
                    try {
                        page = Integer.parseInt(args[i + 1]);
                        i++;
                    } catch (NumberFormatException ex) {
                        _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__PAGE_ERROR);
                        return true;
                    }
                }
            } else if (arg.equalsIgnoreCase("--search") || arg.equalsIgnoreCase("-s")) {
                if (i + 1 < args.length) {
                    searchTerm = args[i + 1];
                    i++;
                } else {
                    _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__SEARCH_ERROR);
                    return true;
                }
            } else {
                try {
                    requestId = Integer.parseInt(arg);
                    page = 0;
                } catch (NumberFormatException ex) {
                    _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__REQUEST_NUMBER);
                    return true;
                }
            }
        }

        if (!sender.hasPermission("modreq.check")) {
            if (sender instanceof Player) {
                limitUUID = senderUUID;
            }
            showNotes = false;
        }

        List<Request> requests = new ArrayList<Request>();

        if (page > 0) {
            if (limitUUID != null) {
                requests.addAll(_plugin.getRequestTable().getUserRequests(limitUUID));
                totalRequests = requests.size();
            } else {
                requests.addAll(_plugin.getRequestTable().getRequestPage(page - 1, 5, includeElevated, includeExternal, searchTerm, Request.RequestStatus.OPEN, Request.RequestStatus.CLAIMED));
                totalRequests = _plugin.getRequestTable().getTotalRequest(includeElevated, searchTerm, Request.RequestStatus.OPEN, Request.RequestStatus.CLAIMED);
            }
        } else if (requestId > 0) {
            Request req = _plugin.getRequestTable().getRequest(requestId);
            List<Note> notes = _plugin.getNoteTable().getRequestNotes(req);

            if (req != null) {
                totalRequests = 1;
                if (limitUUID != null && req.getPlayerUUID().equals(limitUUID)) {
                    requests.add(req);
                } else if (limitUUID == null) {
                    requests.add(req);
                } else {
                    totalRequests = 0;
                }
            } else {
                totalRequests = 0;
            }
        }

        if (totalRequests == 0) {
            if (limitUUID != null) {
                if (requestId > 0) {
                    _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__REQUEST_ERROR);
                } else {
                    _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__NO_REQUESTS);
                }
            } else {
                _plugin.sendMessage(sender, _plugin.getPluginConfig().MOD__NO_REQUESTS);
            }
        } else if (totalRequests == 1 && requestId > 0) {
            _plugin.messageRequestToPlayer(sender, requests.get(0), showNotes);
        } else if (totalRequests > 0) {
            if (page > 1 && requests.isEmpty()) {
                _plugin.sendMessage(sender, _plugin.getPluginConfig().MOD__EMPTY_PAGE);
            } else {
                boolean showPage = true;
                if (limitUUID != null) {
                    showPage = false;
                }
                _plugin.messageRequestListToPlayer(sender, requests, page, totalRequests, showPage);
            }
        } else {
            // there was an error.
        }
        return true;
    }
}
