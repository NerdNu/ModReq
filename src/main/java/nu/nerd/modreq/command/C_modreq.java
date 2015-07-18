/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.nerd.modreq.command;

import java.util.UUID;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author c45y
 */
public class C_modreq implements CommandExecutor {

    private final ModReq _plugin;

    public C_modreq(ModReq plugin) {
        _plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("modreq") || !sender.hasPermission("modreq.request")) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You cannot create a modreq from console.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please supply a message. " + ChatColor.GOLD + "/modreq <request>");
            return true;
        }
        _plugin.environment.clear();
        
        String senderName = ChatColor.stripColor(sender.getName());
        UUID senderUUID = null;
        Player player = null;
        String request = String.join(" ", args);
        if (sender instanceof Player) {
            player = (Player)sender;
            senderUUID = player.getUniqueId();
        }

        if (_plugin.getRequestTable().getNumRequestFromUser(senderUUID) < _plugin.getPluginConfig().MAX_REQUESTS) {
            Request req = new Request();
            req.setPlayerUUID(senderUUID);
            req.setPlayerName(senderName);
            String r = ChatColor.translateAlternateColorCodes('&', request);
            r = ChatColor.stripColor(r);
            req.setRequest(r);
            req.setRequestTime(System.currentTimeMillis());
            String location = String.format("%s,%f,%f,%f,%f,%f", player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
            req.setServerLocation(_plugin.getServerName());
            req.setRequestLocation(location);
            req.setStatus(Request.RequestStatus.OPEN);

            _plugin.getRequestTable().save(req);
            _plugin.environment.put("request_id", String.valueOf(req.getId()));
            _plugin.messageMods(_plugin.getPluginConfig().MOD__NEW_REQUEST);
            _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__REQUEST_FILED);
        } else {
            _plugin.environment.put("max_requests", _plugin.getPluginConfig().MAX_REQUESTS.toString());
            _plugin.sendMessage(sender, _plugin.getPluginConfig().GENERAL__MAX_REQUESTS);
        }

        return true;
    }
}

