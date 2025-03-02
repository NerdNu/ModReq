package nu.nerd.modreq;

import java.util.List;

import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.Request.RequestStatus;

import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitScheduler;

class ModReqListener implements Listener {

	private final ModReq plugin;
	private RequestTable reqTable;
	BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

	public ModReqListener(ModReq plugin) {
		this.plugin = plugin;
		this.reqTable = plugin.getReqTable();
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (event.getPlayer().hasPermission("modreq.check")) {
			boolean includeElevated = event.getPlayer().hasPermission("modreq.cleardb");
			reqTable.getTotalRequest(includeElevated, null, RequestStatus.OPEN, RequestStatus.CLAIMED)
					.thenAcceptAsync(total -> {
						if (total > 0) {
							bukkitScheduler.runTask(plugin, () -> {
								event.getPlayer().sendMessage(ChatColor.GREEN + "There are " + total +
										" open mod requests. Type /check to see them.");
							});
						}
					});
		}

		reqTable.getMissedClosedRequests(event.getPlayer().getUniqueId()).thenAcceptAsync(missedClosed -> {
			for (Request req : missedClosed) {
				String doneMessage = req.getCloseMessage();
				String message = "";
				if (doneMessage != null && doneMessage.length() != 0) {
					message = String.format("%s completed your request - %s%s", req.getAssignedMod(), ChatColor.GRAY, doneMessage);
				} else {
					message = String.format("%s completed your request", req.getAssignedMod());
				}
				String finalMessage = message;
				bukkitScheduler.runTask(plugin, () -> {
					event.getPlayer().sendMessage(ChatColor.GREEN + finalMessage);
				});
				req.setCloseSeenByUser(true);

				plugin.getReqTable().save(req);
			}
		});
	}
}
