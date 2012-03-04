package nu.nerd.modreq;

import org.bukkit.ChatColor;
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
            int open = plugin.reqTable.getTotalOpenRequest();
            event.getPlayer().sendMessage(ChatColor.GREEN + "There are " + open + " open mod requests. Type /check to see them.");
        }
    }
}
