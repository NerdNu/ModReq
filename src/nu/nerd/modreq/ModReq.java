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

public class ModReq extends JavaPlugin 
{
    public final Configuration config = new Configuration(this);
    
    ModReqListener listener = new ModReqListener(this);

    RequestTable reqTable;

    @Override
    public void onEnable() {
        setupDatabase();
        reqTable = new RequestTable(this);
        File config_file = new File(getDataFolder(), "config.yml");
        if (!config_file.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        config.load();
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().log(Level.INFO, getDescription().getName() + " " + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // tear down
        getLogger().log(Level.INFO, getDescription().getName() + " " + getDescription().getVersion() + " disabled.");
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
        List<Request> reqs = reqTable.getRequestPage(0, 1000, RequestStatus.OPEN, RequestStatus.CLAIMED);
        
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

                    messageMods(config.MOD_ALERT_NEW_REQUEST);
                    sender.sendMessage(config.PLAYER_MADE_NEW_REQUEST);
                } else {
                    sender.sendMessage(config.PLAYER_MAX_REQUESTS);
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("check")) {
            int page = 1;
            int requestId = 0;
            int totalRequests = 0;
            String limitName = null;
            
            if (args.length > 0 && !args[0].startsWith("p:")) {
                try {
                    requestId = Integer.parseInt(args[0]);
                    page = 0;
                    
                } catch (NumberFormatException ex) {
                    sender.sendMessage(config.CHECK_INVALID_REQUEST_NUMBER);
                    return true;
                }
            }
            
            if (sender.hasPermission("modreq.check")) {
                
                if (args.length == 0) {
                    page = 1;
                }
                else if (args[0].startsWith("p:")) {
                    try {
                        page = Integer.parseInt(args[0].substring(2));
                        
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(config.CHECK_INVALID_PAGE_NUMBER);
                        return true;
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
                    requests.addAll(reqTable.getRequestPage(page - 1, 5, RequestStatus.OPEN, RequestStatus.CLAIMED));
                    totalRequests = reqTable.getTotalRequest(RequestStatus.OPEN, RequestStatus.CLAIMED);
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
                    if (requestId > 0) {
                        sender.sendMessage(config.CHECK_REQUEST_NOT_FOUND);
                    }
                    else {
                        sender.sendMessage(config.PLAYER_CHECK_EMPTY);
                    }
                }
                else {
                    sender.sendMessage(config.MOD_CHECK_EMPTY);
                }
            } else if (totalRequests == 1 && requestId > 0) {
                if (requests.get(0) != null) {
                    messageRequestToPlayer(sender, requests.get(0));
                } else {
                    sender.sendMessage(config.CHECK_REQUEST_NOT_FOUND);
                }
            } else if (totalRequests > 0) {
                if (page > 1 && requests.size() == 0) {
                    sender.sendMessage(config.CHECK_PAGE_EMPTY);
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
                    player.sendMessage(String.format(config.MOD_TELEPORTING, requestId));
                    Location loc = stringToLocation(req.getRequestLocation());
                    player.teleport(loc);
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(config.MOD_INVALID_REQUEST_NUMBER);
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
                        
                        messageMods(String.format(config.MOD_REQUEST_CLAIMED, senderName, requestId));
                    }
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(config.MOD_INVALID_REQUEST_NUMBER);
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
                    if (req.getAssignedMod() != null &&
                        req.getAssignedMod().equalsIgnoreCase(senderName) &&
                        req.getStatus() == RequestStatus.CLAIMED) {
                        req.setStatus(RequestStatus.OPEN);
                        req.setAssignedMod(null);
                        reqTable.save(req);
                        
                        messageMods(String.format(config.MOD_REQUEST_UNCLAIMED, senderName, requestId));
                    }
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(config.MOD_INVALID_REQUEST_NUMBER);
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
                    sender.sendMessage(config.MOD_REQUEST_ALREADY_CLOSED);
                }
                else {
                	if (sender.hasPermission("modreq.done") && req != null) {
	                    String msg = "";
	                    msg = String.format(config.MOD_REQUEST_COMPLETED, requestId, senderName);
	                    messageMods(msg);
	                    
	                    if (doneMessage != null && doneMessage.length() != 0) {
	                        msg = String.format(config.MOD_CLOSE_MESSAGE, doneMessage);
	                        messageMods(msg);
	                    }
	                }
	                else {
	                    if (!req.getPlayerName().equalsIgnoreCase(senderName)) {
	                        req = null;
	                        
	                        sender.sendMessage(config.MOD_REQUEST_NOT_CLAIMED);
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
	                                message = String.format(config.PLAYER_REQUEST_COMPLETED_WITH_MESSAGE, senderName, doneMessage);
	                            } else {
	                                message = String.format(config.PLAYER_REQUEST_COMPLETED, senderName);
	                            }
	                            requestCreator.sendMessage(message);
	                        }
	                        else {
	                            if (!sender.hasPermission("modreq.done")) {
	                                messageMods(String.format(config.MOD_ALERT_REQUEST_COMPLETED, requestId));
	                                sender.sendMessage(String.format(config.MOD_REQUEST_CLOSED, requestId));
	                            }
	                        }
	                        req.setCloseSeenByUser(true);
	                    }
	                    reqTable.save(req);
	                }
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(config.MOD_INVALID_REQUEST_NUMBER);
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
                    if ((req.getAssignedMod() != null &&
                         req.getAssignedMod().equalsIgnoreCase(senderName) &&
                         req.getStatus() == RequestStatus.CLAIMED) ||
                        req.getStatus() == RequestStatus.CLOSED) {
                        req.setStatus(RequestStatus.OPEN);
                        req.setAssignedMod(null);
                        req.setCloseSeenByUser(false);
                        reqTable.save(req);
                        
                        messageMods(String.format(config.MOD_ALERT_REQUEST_REOPENED, requestId));
                    }
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(config.MOD_INVALID_REQUEST_NUMBER);
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
                    messageMods(String.format(config.MOD_REQUEST_ELEVATED, req.getId()));
                    reqTable.save(req);
                } else {
                    sender.sendMessage(config.MOD_UNCLAIM_TO_ELEVATE);
                }
            }
            catch (NumberFormatException ex) {
                sender.sendMessage(config.MOD_INVALID_REQUEST_NUMBER);
            }
        } else if ( command.getName().equalsIgnoreCase("mr-reset")) {
            try {
                resetDatabase();
                sender.sendMessage(config.MOD_DATABASE_RESET);
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
        String onlineStatus = config.OFFLINE_COLOR;
        
        if (getServer().getPlayerExact(req.getPlayerName()) != null) {
            onlineStatus = config.ONLINE_COLOR;
        }
        Location loc = stringToLocation(req.getRequestLocation());
        String location = String.format("%s, %d, %d, %d", loc.getWorld().getName(), Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));
        messages.add(String.format(config.PLAYER_MOD_REQUEST_FROM,
                                   req.getId(),
                                   req.getStatus().toString() + ((req.getStatus() == RequestStatus.CLAIMED)?" by " + req.getAssignedMod():"")));
        String lastColor = ChatColor.getLastColors(config.PLAYER_MOD_REQUEST_FILED.split("%s")[0]);
        messages.add(String.format(config.PLAYER_MOD_REQUEST_FILED,
                                   onlineStatus + req.getPlayerName() + lastColor,
                                   timestampToDateString(req.getRequestTime()),
                                   location));
        messages.add(String.format(config.PLAYER_MOD_REQUEST_MESSAGE, req.getRequest()));
        
        sender.sendMessage(messages.toArray(new String[1]));
    }
    
    private void messageRequestListToPlayer(CommandSender sender, List<Request> reqs, int page, int totalRequests, boolean showPage) {
        List<String> messages = new ArrayList<String>();
        
        messages.add(String.format(config.MOD_REQUEST_LIST_HEADER, totalRequests));
        for (Request r : reqs) {
            String onlineStatus = config.OFFLINE_COLOR;
            String message = "";
            if (r.getRequest().length() > 20) {
                message = r.getRequest().substring(0, 17) + "...";
            } else {
                message = r.getRequest();
            }
            if (getServer().getPlayerExact(r.getPlayerName()) != null) {
                onlineStatus = config.ONLINE_COLOR;
            }
            String[] parts = config.MOD_REQUEST_LIST_ITEM.split("%s");
            String afterAdminColor = ChatColor.getLastColors(parts[0]);
            StringBuilder before = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++)
                before.append(parts[i]);
            String lastColor = ChatColor.getLastColors(before.toString());
            try {
                messages.add(String.format(config.MOD_REQUEST_LIST_ITEM, 
                                           r.getId(),
                                           ((r.isFlagForAdmin())?(config.ADMIN_COLOR + "[ADMIN] " + afterAdminColor):""),
                                           ((r.getStatus() == RequestStatus.CLAIMED)?r.getAssignedMod():r.getStatus().toString()),
                                           timestampToDateString(r.getRequestTime()),
                                           onlineStatus + r.getPlayerName() + lastColor,
                                           message));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        if (showPage) {
            messages.add(String.format(config.MOD_REQUEST_LIST_FOOTER, page, (int)Math.ceil(totalRequests / 5.0)));
        }
        
        sender.sendMessage(messages.toArray(new String[1]));
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
