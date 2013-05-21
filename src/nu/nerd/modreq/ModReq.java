package nu.nerd.modreq;

import com.avaje.ebean.CallableSql;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlUpdate;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.Request.RequestStatus;
import nu.nerd.modreq.database.RequestTable;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

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
    
    public boolean setupDatabase() {
        try {
            getDatabase().find(Request.class).findRowCount();
        } catch (PersistenceException ex) {
            getLogger().log(Level.INFO, "First run, initializing database.");
            installDDL();
            return true;
        }
        
        return false;
    }
    
    public void resetDatabase() {
        List<Request> reqs = reqTable.getRequestPage(0, 1000, true, RequestStatus.OPEN, RequestStatus.CLAIMED);
        
        removeDDL();
        
        if (setupDatabase()) {
        
            for (Request r : reqs) {
                Request req = new Request();
                req.setPlayerName(r.getPlayerName());
                req.setRequest(r.getRequest());
                req.setRequestTime(r.getRequestTime());
                req.setRequestLocation(r.getRequestLocation());
                req.setStatus(r.getStatus());
                if (r.getStatus() == RequestStatus.CLAIMED) {
                    req.setAssignedMod(r.getAssignedMod());
                }
                req.setFlagForAdmin(r.isFlagForAdmin());

                reqTable.save(req);
            }
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
        boolean includeElevated = sender.hasPermission("modreq.cleardb");
        String senderName = ChatColor.stripColor(sender.getName());
        if (sender instanceof ConsoleCommandSender) {
            senderName = "Console";
        }
        if (command.getName().equalsIgnoreCase("modreq")) {
            if (args.length == 0) {
                return false;
            }

            StringBuilder request = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; i++) {
                request.append(" ").append(args[i]);
            }
            
            if (sender instanceof Player) {
                Player player = (Player)sender;
                
                if (reqTable.getNumRequestFromUser(senderName) < 5) {
                    Request req = new Request();
                    req.setPlayerName(senderName);
                    req.setRequest(request.toString());
                    req.setRequestTime(System.currentTimeMillis());
                    String location = String.format("%s,%f,%f,%f", player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
                    req.setRequestLocation(location);
                    req.setStatus(RequestStatus.OPEN);

                    reqTable.save(req);

                    messageMods(ChatColor.GREEN + "New request. Type /check for more");
                    sender.sendMessage(ChatColor.GREEN + "Request has been filed. Please be patient for a moderator to complete your request.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You already have 5 open requests, please wait for them to be completed.");
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("check")) {
            
            int offset = args[0].equalsIgnoreCase("-admin") ? 1 : 0;
            
            int page = 1;
            int requestId = 0;
            int totalRequests = 0;
            String limitName = null;
            
            if (args.length > offset && !args[offset].startsWith("p:")) {
                try {
                    requestId = Integer.parseInt(args[offset]);
                    page = 0;
                    
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "You must provide a number for requests.");
                    return true;
                }
            }
            
            if (sender.hasPermission("modreq.check")) {
                
                
                
                if (args.length == offset) {
                    page = 1;
                }
                else if (args[offset].startsWith("p:")) {
                    try {
                        page = Integer.parseInt(args[offset].substring(2));
                        
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "You must provide a number for pages.");
                        return true;
                    }
                }
                
                if(offset == 1){
                    includeElevated = true;
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
                    requests.addAll(reqTable.getRequestPage(page - 1, 5, includeElevated, RequestStatus.OPEN, RequestStatus.CLAIMED));
                    totalRequests = reqTable.getTotalRequest(includeElevated, RequestStatus.OPEN, RequestStatus.CLAIMED);
                }
            } else if (requestId > 0) {
                Request req = reqTable.getRequest(requestId);
                if (req != null) {
                    totalRequests = 1;
                    if (limitName != null && req.getPlayerName().equalsIgnoreCase(limitName)) {
                        requests.add(req);
                    } else if (limitName == null) {
                        requests.add(req);
                    } else {
                        totalRequests = 0;
                    }
                } else {
                    totalRequests = 0;
                }
            }
            
            if (totalRequests == 0) {
                if (limitName != null) {
                    if (requestId > 0) {
                        sender.sendMessage(ChatColor.GREEN + "Either that request doesn't exist, or you do not have permission to view it.");
                    }
                    else {
                        sender.sendMessage(ChatColor.GREEN + "You don't have any outstanding mod requests.");
                    }
                }
                else {
                    sender.sendMessage(ChatColor.GREEN + "There are currently no open mod requests.");
                }
            } else if (totalRequests == 1 && requestId > 0) {
                messageRequestToPlayer(sender, requests.get(0));
            } else if (totalRequests > 0) {
                if (page > 1 && requests.size() == 0) {
                    sender.sendMessage(ChatColor.RED + "There are no requests on that page.");
                } else {
                    boolean showPage = true;
                    if (limitName != null) {
                        showPage = false;
                    }
                    messageRequestListToPlayer(sender, requests, page, totalRequests, showPage);
                }
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
                    Location loc = stringToLocation(req.getRequestLocation());
                    player.teleport(loc);
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
                        
                        messageMods(String.format("%s%s is now handling request #%d", ChatColor.GREEN, senderName, requestId));
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
                        
                        messageMods(String.format("%s%s has unclaimed request #%d", ChatColor.GREEN, senderName, requestId));
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
                    StringBuilder doneMessageBuilder = new StringBuilder(args[1]);
                    for (int i = 2; i < args.length; i++) {
                        doneMessageBuilder.append(" ").append(args[i]);
                    }
                    
                    doneMessage = doneMessageBuilder.toString();
                }
                
                Request req = reqTable.getRequest(requestId);
                
                if (req != null && req.getStatus() == RequestStatus.CLOSED) {
                	sender.sendMessage(ChatColor.RED + "Request Already Closed.");
                }
                else {
                	if (sender.hasPermission("modreq.done") && req != null) {
	                    String msg = "";
	                    msg = String.format("%sRequest #%d has been completed by %s", ChatColor.GREEN, requestId, senderName);
	                    messageMods(msg);
	                    
	                    if (doneMessage != null && doneMessage.length() != 0) {
	                        msg = String.format("Close Message - %s%s", ChatColor.GRAY, doneMessage);
	                        messageMods(msg);
	                    }
	                }
	                else {
	                    if (!req.getPlayerName().equalsIgnoreCase(senderName)) {
	                        req = null;
	                        
	                        sender.sendMessage(String.format("%s[ModReq] Error, you can only close your own requests.", ChatColor.RED));
	                    }
	                }
	                
	                if (req != null) {
	                    req.setStatus(RequestStatus.CLOSED);
	                    req.setCloseTime(System.currentTimeMillis());
	                    req.setCloseMessage(doneMessage);
	                    req.setAssignedMod(senderName);
	                    
	                    Player requestCreator = getServer().getPlayerExact(req.getPlayerName());
	                    if (requestCreator != null) {
	                        if (!requestCreator.getName().equalsIgnoreCase(senderName)) {
	                            String message = "";
	                            if (doneMessage != null && doneMessage.length() != 0) {
	                                message = String.format("%s completed your request - %s%s", senderName, ChatColor.GRAY, doneMessage);
	                            } else {
	                                message = String.format("%s completed your request", senderName);
	                            }
	                            requestCreator.sendMessage(ChatColor.GREEN + message);
	                        }
	                        else {
	                            if (!sender.hasPermission("modreq.done")) {
	                                messageMods(ChatColor.GREEN + String.format("Request #%d no longer needs to be handled", requestId));
	                                sender.sendMessage(ChatColor.GREEN + String.format("Request #%d has been closed by you.", requestId));
	                            }
	                        }
	                        req.setCloseSeenByUser(true);
	                    }
	                    reqTable.save(req);
	                }
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
                        
                        messageMods(ChatColor.GREEN + String.format("[ModReq] Request #%d has been reopened by %s%s.", requestId));
                    }
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        } else if (command.getName().equalsIgnoreCase("elevate")) {
            if (args.length == 0) {
                return false;
            }
            int requestId = 0;
            
            try {
                requestId = Integer.parseInt(args[0]);
                
                Request req = reqTable.getRequest(requestId);
                if (req.getStatus() == RequestStatus.OPEN) {
                    req.setFlagForAdmin(true);
                    messageMods(String.format("%s[ModReq] Request #%d has been flagged for admin.", ChatColor.GREEN, req.getId()));
                    reqTable.save(req);
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "[ModReq] Error: Expected a number for request.");
            }
        } else if ( command.getName().equalsIgnoreCase("mr-reset")) {
            try {
                resetDatabase();
                sender.sendMessage(ChatColor.GREEN + "[ModReq] Database has been reset.");
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Failed to reset database", ex);
            }
        }

        return true;
    }
    
    private Location stringToLocation(String requestLocation) {
        Location loc;
        double x, y, z;
        String world;
        String[] split = requestLocation.split(",");
        world = split[0];
        x = Double.parseDouble(split[1]);
        y = Double.parseDouble(split[2]);
        z = Double.parseDouble(split[3]);
        
        loc = new Location(getServer().getWorld(world), x, y, z);
        
        return loc;
    }
    
    private String timestampToDateString(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("MMM.d@k.m.s");
        return format.format(cal.getTime());
    }

    private void messageRequestToPlayer(CommandSender sender, Request req) {
        List<String> messages = new ArrayList<String>();
        ChatColor onlineStatus = ChatColor.RED;
        
        if (getServer().getPlayerExact(req.getPlayerName()) != null) {
            onlineStatus = ChatColor.GREEN;
        }
        Location loc = stringToLocation(req.getRequestLocation());
        String location = String.format("%s, %d, %d, %d", loc.getWorld().getName(), Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));
        
        messages.add(String.format("%sMod Request #%d - %s%s%s", ChatColor.AQUA, req.getId(), ChatColor.YELLOW, req.getStatus().toString(), ((req.getStatus() == RequestStatus.CLAIMED)?" by " + req.getAssignedMod():"")));
        messages.add(String.format("%sFiled by %s%s%s at %s%s%s at %s%s", ChatColor.YELLOW, onlineStatus, req.getPlayerName(), ChatColor.YELLOW, ChatColor.GREEN, timestampToDateString(req.getRequestTime()), ChatColor.YELLOW, ChatColor.GREEN, location));
        messages.add(String.format("%s%s", ChatColor.GRAY, req.getRequest()));
        
        sender.sendMessage(messages.toArray(new String[1]));
    }
    
    private void messageRequestListToPlayer(CommandSender sender, List<Request> reqs, int page, int totalRequests, boolean showPage) {
        List<String> messages = new ArrayList<String>();
        
        messages.add(String.format("%s---- %d Mod Requests ----", ChatColor.AQUA, totalRequests));
        for (Request r : reqs) {
            ChatColor onlineStatus = ChatColor.RED;
            String message = "";
            if (r.getRequest().length() > 20) {
                message = r.getRequest().substring(0, 17) + "...";
            } else {
                message = r.getRequest();
            }
            if (getServer().getPlayerExact(r.getPlayerName()) != null) {
                onlineStatus = ChatColor.GREEN;
            }
            try {
                messages.add(String.format("%s#%d.%s [%s%s%s] %s by %s%s%s - %s%s", ChatColor.GOLD, r.getId(), ((r.isFlagForAdmin())?(ChatColor.AQUA + " [ADMIN]" + ChatColor.GOLD):""), ChatColor.GREEN ,((r.getStatus() == RequestStatus.CLAIMED)?r.getAssignedMod():r.getStatus().toString()), ChatColor.GOLD, timestampToDateString(r.getRequestTime()), onlineStatus, r.getPlayerName(), ChatColor.GOLD, ChatColor.GRAY, message));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        if (showPage) {
            messages.add(String.format("%s---- Page %d of %d ----", ChatColor.AQUA, page, (int)Math.ceil(totalRequests / 5.0)));
        }
        
        sender.sendMessage(messages.toArray(new String[1]));
    }
    
    public void messageMods(String message) {
        String permission = "modreq.notice";
        this.getServer().broadcast(message, permission);

        Set<Permissible> subs = getServer().getPluginManager().getPermissionSubscriptions(permission);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission(permission) && !subs.contains(player)) {
                player.sendMessage(message);
            }
        }
    }
}
