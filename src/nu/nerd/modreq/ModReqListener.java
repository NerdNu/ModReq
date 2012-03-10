package nu.nerd.modreq;

import java.util.List;

import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.Request.RequestStatus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

class ModReqListener implements Listener {
    private final ModReq plugin;

    public ModReqListener(ModReq instance) {
        plugin = instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("modreq.check")) {
            int open = plugin.reqTable.getTotalRequest(RequestStatus.OPEN, RequestStatus.CLAIMED);
            event.getPlayer().sendMessage(ChatColor.GREEN + "There are " + open + " open mod requests. Type /check to see them.");
        }
        
        List<Request> missedClosed = plugin.reqTable.getMissedClosedRequests(ChatColor.stripColor(event.getPlayer().getName()));
        
        for (Request req : missedClosed) {
        	String doneMessage = req.getCloseMessage();
    		String message = "";
    		if (doneMessage != null && !doneMessage.isEmpty()) {
    			message = String.format("%s completed your request - %s%s", req.getAssignedMod(), ChatColor.GRAY, doneMessage);
    		} else {
    			message = String.format("%s completed your request", req.getAssignedMod());
    		}
    		event.getPlayer().sendMessage(ChatColor.GREEN + message);
        	req.setCloseSeenByUser(true);
        	
        	plugin.reqTable.save(req);
        }
    }
}
