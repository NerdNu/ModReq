package nu.nerd.modreq;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.Request.RequestStatus;
import nu.nerd.modreq.database.RequestTable;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.PagingList;

public class ModReq extends JavaPlugin {
    ModReqListener listener = new ModReqListener(this);

	RequestTable reqTable;
	
    @Override
    public void onEnable() {
    	setupDatabase();
    	reqTable = new RequestTable(this);
        getServer().getPluginManager().registerEvents(listener, this);
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
	            req.setPlayerName(senderName);
	            req.setRequest(req.toString());
	            req.setRequestLocation(player.getLocation());
	            req.setStatus(RequestStatus.OPEN);
            }
        }
        else if (command.getName().equalsIgnoreCase("check")) {
        	int page = 1;
        	int requestId = 0;
        	int totalRequests = 0;
        	String limitName = null;
        	
        	if (args.length > 0 && !args[0].startsWith("p:")) {
        		try {
                	requestId = Integer.parseInt(args[0].substring(3));
                	page = 0;
                	
                } catch (NumberFormatException ex) {
                	requestId = -1;
                }
        	}
        	
            if (sender.hasPermission("modreq.check")) {
            	
                if (args.length == 0) {
                    page = 0;
                }
                else if (args[0].startsWith("p:")) {
                    try {
                    	page = Integer.parseInt(args[0].substring(3));
                    	
                    } catch (NumberFormatException ex) {
                    	page = -1;
                    }
                }
            }
            else {
                limitName = senderName;
            }
            
            List<Request> requests = new ArrayList<Request>();
            
            if (page > 0) {
            	if (limitName != null) {
            		requests.addAll(reqTable.getUserRequests(limitName));
            		totalRequests = requests.size();
            	} else {
            		requests.addAll(reqTable.getRequestPage(page, 5, RequestStatus.OPEN));
            		totalRequests = reqTable.getTotalOpenRequest();
            	}
            } else if (requestId > 0) {
            	Request req = reqTable.getRequest(requestId);
            	totalRequests = 1;
            	if (limitName != null && req.getPlayerName().equalsIgnoreCase(limitName)) {
            		requests.add(req);
            	} else if (limitName == null) {
            		requests.add(req);
            	} else {
            		totalRequests = 0;
            	}
            }
            
            if (totalRequests == 0) {
            	if (limitName != null) {
            		sender.sendMessage(ChatColor.GREEN + "You don't have any outstanding mod requests.");
            	}
            	else {
            		sender.sendMessage(ChatColor.GREEN + "There are currently no open mod requests.");
            	}
            } else if (totalRequests == 1 && requestId > 0) {
            	// Send single mod request details
            } else if (totalRequests > 0) {
            	// Send list of requests
            } else {
            	// there was an error.
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
