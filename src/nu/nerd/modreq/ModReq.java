package nu.nerd.modreq;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.Request.RequestStatus;
import nu.nerd.modreq.database.RequestTable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

public class ModReq extends JavaPlugin {

	RequestTable reqTable;
	
    @Override
    public void onEnable() {
    	setupDatabase();
    	reqTable = new RequestTable(this);
    }

    @Override
    public void onDisable() {
        // tear down
    }
    
    public void setupDatabase() {

        try {
            getDatabase().find(Request.class).findRowCount();
        } catch (PersistenceException ex) {
            getLogger().log(Level.INFO, "First run, initializing database.");
            installDDL();
        }
    }
    
    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Request.class);
        return list;
    }

	@Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		String senderName = ChatColor.stripColor(sender.getName());
        if (command.getName().equalsIgnoreCase("modreq")) {
            if (args.length == 0) {
                return false;
            }

            StringBuilder request = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; i++) {
                request.append(" ").append(args[i]);
            }
            // submit a modreq here, no need for a permission check
            
            if (sender instanceof Player) {
            	Player player = (Player)sender;
	            Request req = new Request();
	            req.setPlayerName(sender.getName());
	            req.setRequest(req.toString());
	            req.setRequestLocation(player.getLocation());
	            req.setStatus(RequestStatus.OPEN);
            }
        }
        else if (command.getName().equalsIgnoreCase("check")) {
            if (sender.hasPermission("modreq.check")) {
                if (args.length == 0) {
                    // get page 1
                }
                else if (args[0].startsWith("p:")) {
                    // get a different page
                }
                else {
                    // check a specific request
                }
            }
            else {
                // only show their own requests
            }
        }
        else if (command.getName().equalsIgnoreCase("tp-id")) {
            if (args.length == 0) {
                return false;
            }
            int requestId = 0;
            try {
            	requestId = Integer.parseInt(args[0]);
            	
	            if (sender instanceof Player) {
	            	Player player = (Player)sender;
		            Request req = reqTable.getRequest(requestId);
		            player.sendMessage(ChatColor.GREEN + "[ModReq] Teleporting you to request " + requestId);
		            player.teleport(req.getRequestLocation());
	            }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        }
        else if (command.getName().equalsIgnoreCase("claim")) {
            if (args.length == 0) {
                return false;
            }
            int requestId = 0;
            try {
            	requestId = Integer.parseInt(args[0]);
            	
	            if (sender instanceof Player) {
	            	Player player = (Player)sender;
		            Request req = reqTable.getRequest(requestId);
		            
		            if (req.getStatus() == RequestStatus.OPEN) {
		            	req.setStatus(RequestStatus.CLAIMED);
		            	req.setAssignedMod(senderName);
		            	reqTable.save(req);
		            	
		            	// Send Claimed Message to Mods.
		            }
	            }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        }
        else if (command.getName().equalsIgnoreCase("unclaim")) {
            if (args.length == 0) {
                return false;
            }
            int requestId = 0;
            
            try {
            	requestId = Integer.parseInt(args[0]);
            	
	            if (sender instanceof Player) {
	            	Player player = (Player)sender;
		            Request req = reqTable.getRequest(requestId);
		            if (req.getAssignedMod().equalsIgnoreCase(senderName) && req.getStatus() == RequestStatus.CLAIMED) {
		            	req.setStatus(RequestStatus.OPEN);
		            	req.setAssignedMod(null);
		            	reqTable.save(req);
		            	
		            	// Send Unclaimed Message to Mods.
		            }
	            }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        }
        else if (command.getName().equalsIgnoreCase("done")) {
            if (args.length == 0) {
                return false;
            }
            
            int requestId = 0;
            
            try {
            	requestId = Integer.parseInt(args[0]);
            	
            	String doneMessage = null;
            	
            	if (args.length > 1) {
            		StringBuilder doneMessageBuilder = new StringBuilder(args[0]);
                    for (int i = 1; i < args.length; i++) {
                        doneMessageBuilder.append(" ").append(args[i]);
                    }
                    
                    doneMessage = doneMessageBuilder.toString();
            	}
            	
            	Request req = reqTable.getRequest(requestId);
            	
		        if (sender.hasPermission("modreq.done")) {
		            // Send done message to mods
		        }
		        else {
		        	if (!req.getPlayerName().equalsIgnoreCase(senderName)) {
		        		req = null;
		        		// Send error message to user.
		        	}
		        }
		        
		        if (req != null) {
		        	req.setStatus(RequestStatus.CLOSED);
		            req.setCloseMessage(doneMessage);
		            
		            Player requestCreator = getServer().getPlayerExact(req.getPlayerName());
		            if (requestCreator != null) {
		            	if (!requestCreator.getName().equalsIgnoreCase(senderName)) {
		            		requestCreator.sendMessage(senderName + " completed your mod request.");
		            	}
		            	else {
		            		// Message mods to say that a request no longer needs to be handled. 
		            	}
		            	req.setCloseSeenByUser(true);
		            }
		            reqTable.save(req);
		        }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        }
        else if (command.getName().equalsIgnoreCase("reopen")) {
            if (args.length == 0) {
                return false;
            }
            int requestId = 0;
            
            try {
            	requestId = Integer.parseInt(args[0]);
            	
	            if (sender instanceof Player) {
	            	Player player = (Player)sender;
		            Request req = reqTable.getRequest(requestId);
		            if ((req.getAssignedMod().equalsIgnoreCase(senderName) && req.getStatus() == RequestStatus.CLAIMED) || req.getStatus() == RequestStatus.CLOSED) {
		            	req.setStatus(RequestStatus.OPEN);
		            	req.setAssignedMod(null);
		            	req.setCloseSeenByUser(false);
		            	reqTable.save(req);
		            	
		            	// Send Unclaimed Message to Mods.
		            }
	            }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        }

        return true;
    }
	
	public void messageMods(String message) {
        String permission = "modreq.mod";
        this.getServer().broadcast(message, permission);

        Set<Permissible> subs = getServer().getPluginManager().getPermissionSubscriptions(permission);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission(permission) && !subs.contains(player)) {
                player.sendMessage(message);
            }
        }
    }
}
