package nu.nerd.modreq;

import com.avaje.ebean.CallableSql;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlUpdate;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
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
    Configuration config = new Configuration(this);
    Dictionary<String, String> environment = new Hashtable<String, String>();

    RequestTable reqTable;
    
    @Override
    public void onEnable() {
        setupDatabase();
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        
        config.load();
        
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
        List<Request> reqs = reqTable.getRequestPage(0, 1000, true, null, RequestStatus.OPEN, RequestStatus.CLAIMED);
        
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
                
                if (reqTable.getNumRequestFromUser(senderName) < config.MAX_REQUESTS) {
                    Request req = new Request();
                    req.setPlayerName(senderName);
                    req.setRequest(request.toString());
                    req.setRequestTime(System.currentTimeMillis());
                    String location = String.format("%s,%f,%f,%f,%f,%f", player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
                    req.setRequestLocation(location);
                    req.setStatus(RequestStatus.OPEN);

                    reqTable.save(req);
                    messageMods(config.MOD__NEW_REQUEST);
                    sendMessage(sender, config.GENERAL__REQUEST_FILED);
                } else {
                    environment.put("max_requests", config.MAX_REQUESTS.toString());
                    sendMessage(sender, config.GENERAL__MAX_REQUESTS);
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("check")) {
            // Setting page > 0 triggers a page listing.
            int page = 1;
            int requestId = 0;
            int totalRequests = 0;
            String searchTerm = null;
            String limitName = null;
            
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equalsIgnoreCase("--admin") || arg.equalsIgnoreCase("-a")) {
                    includeElevated = true;
                }
                else if (arg.startsWith("p:")) {
                    page = Integer.parseInt(arg.substring(2));
                }
                else if (arg.equalsIgnoreCase("--page") || arg.equalsIgnoreCase("-p")) {
                    if (i+1 > args.length) {
                        sendMessage(sender, config.GENERAL__PAGE_ERROR);
                        return true;
                    }
                    else {
                        try {
                            page = Integer.parseInt(args[i+1]);
                            i++;
                        }
                        catch (NumberFormatException ex) {
                            sendMessage(sender, config.GENERAL__PAGE_ERROR);
                            return true;
                        }
                    }
                }
                else if (arg.equalsIgnoreCase("--search") || arg.equalsIgnoreCase("-s")) {
                    if (i+1 <= args.length) {
                        searchTerm = args[i+1];
                        i++;
                    }
                    else {
                        sendMessage(sender, config.GENERAL__SEARCH_ERROR);
                        return true;
                    }
                }
                else {
                    try {
                        requestId = Integer.parseInt(arg);
                        page = 0;
                    }
                    catch (NumberFormatException ex) {
                        sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
                        return true;
                    }
                }
            }
            
            if (!sender.hasPermission("modreq.check")) {
                limitName = senderName;
            }
            
            List<Request> requests = new ArrayList<Request>();
            
            if (page > 0) {
                if (limitName != null) {
                    requests.addAll(reqTable.getUserRequests(limitName));
                    totalRequests = requests.size();
                } else {
                    requests.addAll(reqTable.getRequestPage(page - 1, 5, includeElevated, searchTerm, RequestStatus.OPEN, RequestStatus.CLAIMED));
                    totalRequests = reqTable.getTotalRequest(includeElevated, searchTerm, RequestStatus.OPEN, RequestStatus.CLAIMED);
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
                        sendMessage(sender, config.GENERAL__REQUEST_ERROR);
                    }
                    else {
                        sendMessage(sender, config.GENERAL__NO_REQUESTS);
                    }
                }
                else {
                    sendMessage(sender, config.MOD__NO_REQUESTS);
                }
            } else if (totalRequests == 1 && requestId > 0) {
                messageRequestToPlayer(sender, requests.get(0));
            } else if (totalRequests > 0) {
                if (page > 1 && requests.size() == 0) {
                    sendMessage(sender, config.MOD__EMPTY_PAGE);
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
                    if (req != null) {
                        sendMessage(player, config.MOD__TELEPORT);
                        Location loc = stringToLocation(req.getRequestLocation());
                        player.teleport(loc);
                    }
                    else {
                        sendMessage(sender, config.GENERAL__REQUEST_ERROR);
                    } 
                }
            }
            catch (NumberFormatException ex) {
                sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
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
                        
                        environment.put("mod", senderName);
                        environment.put("request_id", String.valueOf(requestId));
                        messageMods(config.MOD__REQUEST_TAKEN);
                        environment.remove("mod");
                        environment.remove("request_id");
                    }
                }
            }
            catch (NumberFormatException ex) {
                sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
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
                        
                        environment.put("mod", senderName);
                        environment.put("request_id", String.valueOf(requestId));
                        messageMods(config.MOD__UNCLAIM);
                        environment.remove("mod");
                        environment.remove("request_id");
                    }
                }
            }
            catch (NumberFormatException ex) {
                sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
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
                    sendMessage(sender, config.MOD__ALREADY_CLOSED);
                }
                else {
                    if (sender.hasPermission("modreq.done") && req != null) {
                        String msg = "";
                        environment.put("mod", senderName);
                        environment.put("request_id", String.valueOf(requestId));
                        msg = String.format("%sRequest #%d has been completed by %s", ChatColor.GREEN, requestId, senderName);
                        messageMods(config.MOD__COMPLETED);
                        environment.remove("request_id");
                        environment.remove("mod");

                        if (doneMessage != null && doneMessage.length() != 0) {
                            msg = String.format("Close Message - %s%s", ChatColor.GRAY, doneMessage);
                            environment.put("close_message", doneMessage);
                            messageMods(config.MOD__COMPLETED_MESSAGE);
                            environment.remove("close_message");
                        }
                    }
                    else {
                        if (!req.getPlayerName().equalsIgnoreCase(senderName)) {
                            req = null;
                            sendMessage(sender, config.GENERAL__CLOSE_ERROR);
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
                                environment.put("close_message", doneMessage);
                                environment.put("mod", senderName);
                                if (doneMessage != null && doneMessage.length() != 0) {
                                    sendMessage(requestCreator, config.GENERAL__COMPLETED_MESSAGE);
                                } else {
                                    sendMessage(requestCreator, config.GENERAL__COMPLETED);
                                }
                                requestCreator.sendMessage(ChatColor.GREEN + message);
                                environment.remove("close_message");
                                environment.remove("mod");
                            }
                            else {
                                if (!sender.hasPermission("modreq.done")) {
                                    environment.put("request_id", String.valueOf(requestId));
                                    messageMods(config.MOD__DELETED);
                                    sendMessage(sender, config.GENERAL__DELETED);
                                    environment.remove("request_id");
                                }
                            }
                            req.setCloseSeenByUser(true);
                        }
                        reqTable.save(req);
                    }
                }
            }
            catch (NumberFormatException ex) {
                sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
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
                        
                        environment.put("mod", sender.getName());
                        environment.put("request_id", String.valueOf(requestId));
                        messageMods(config.MOD__REOPENED);
                        environment.remove("mod");
                        environment.remove("request_id");
                    }
                }
            }
            catch (NumberFormatException ex) {
                sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
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
                    environment.put("request_id", String.valueOf(req.getId()));
                    messageMods(config.MOD__FLAGGED);
                    environment.remove("request_id");
                    reqTable.save(req);
                }
            }
            catch (NumberFormatException ex) {
                sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
            }
        } else if ( command.getName().equalsIgnoreCase("mr-reset")) {
            try {
                resetDatabase();
                sendMessage(sender, config.MOD__RESET);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Failed to reset database", ex);
            }
        }

        return true;
    }
    
    private Location stringToLocation(String requestLocation) {
        Location loc;
        double x, y, z;
        float pitch, yaw;
        String world;
        String[] split = requestLocation.split(",");
        world = split[0];
        x = Double.parseDouble(split[1]);
        y = Double.parseDouble(split[2]);
        z = Double.parseDouble(split[3]);
	if (split.length > 4) {
           yaw = Float.parseFloat(split[4]);
           pitch = Float.parseFloat(split[5]);
            loc = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
	} else {
           loc = new Location(getServer().getWorld(world), x, y, z);
        }
        return loc;
    }
    
    private String timestampToDateString(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("MMM.d@k.m.s");
        return format.format(cal.getTime());
    }
    
    public String buildMessage(String message) {
        while (environment.keys().hasMoreElements()) {
            String key = environment.keys().nextElement();
            String value = environment.get(key);
            environment.remove(key);
            if (key.equalsIgnoreCase("player")) {
                if (getServer().getPlayerExact(value).isOnline()) {
                    value = config.COLOUR__ONLINE + value;
                }
                else {
                    value = config.COLOUR__OFFLINE + value;
                }
            }
            message = message.replace("{" + key + "}", value);
        }
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    private void messageRequestToPlayer(CommandSender sender, Request req) {
        List<String> messages = new ArrayList<String>();
        Location loc = stringToLocation(req.getRequestLocation());
        String location = String.format("%s, %d, %d, %d", loc.getWorld().getName(), Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));
        
        environment.put("status", req.getStatus().toString());
        environment.put("request_id", String.valueOf(req.getId()));
        if (req.getStatus() == RequestStatus.CLAIMED) {
            environment.put("mod", req.getAssignedMod());
            messages.add(buildMessage(config.GENERAL__ITEM__HEADER_CLAIMED));
            environment.remove("mod");
        }
        else {
            messages.add(buildMessage(config.GENERAL__ITEM__HEADER_UNCLAIMED));
        }
        environment.remove("status");
        environment.remove("request_id");
        environment.put("player", req.getPlayerName());
        environment.put("time", timestampToDateString(req.getRequestTime()));
        environment.put("location", location);
        messages.add(buildMessage(config.GENERAL__ITEM__DETAILS));
        environment.remove("player");
        environment.remove("time");
        environment.remove("location");
        environment.put("request_message", req.getRequest());
        messages.add(buildMessage(config.GENERAL__ITEM__REQUEST));
        environment.remove("request_message");
        
        sender.sendMessage(messages.toArray(new String[1]));
    }
    
    private void messageRequestListToPlayer(CommandSender sender, List<Request> reqs, int page, int totalRequests, boolean showPage) {
        List<String> messages = new ArrayList<String>();
        
        environment.put("num_requests", String.valueOf(totalRequests));
        messages.add(buildMessage(config.GENERAL__LIST__HEADER));
        environment.remove("num_requests");
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
                environment.put("request_id", String.valueOf(r.getId()));
                environment.put("admin", (r.isFlagForAdmin()?(ChatColor.AQUA + " [ADMIN]"):""));
                environment.put("mod", (r.getStatus()==RequestStatus.CLAIMED?(r.getAssignedMod()):""));
                environment.put("status", (r.getStatus()!=RequestStatus.CLAIMED?(r.getStatus().toString()):""));
                environment.put("time", timestampToDateString(r.getRequestTime()));
                environment.put("player", r.getPlayerName());
                environment.put("request_message", r.getRequest());
                messages.add(buildMessage(config.GENERAL__LIST__ITEM));
                environment.remove("request_id");
                environment.remove("admin");
                environment.remove("mod");
                environment.remove("status");
                environment.remove("time");
                environment.remove("player");
                environment.remove("request_message");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        if (showPage) {
            int numpages = (int)Math.ceil(totalRequests / config.MAX_REQUESTS.floatValue());
            environment.put("page", String.valueOf(page));
            environment.put("num_pages", String.valueOf(numpages));
            messages.add(buildMessage(config.GENERAL__LIST__FOOTER));
            environment.remove("page");
            environment.remove("num_pages");
        }
        
        sender.sendMessage(messages.toArray(new String[1]));
    }
    
    public void sendMessage(CommandSender sender, String message) {
        message = buildMessage(message);
        sender.sendMessage(message);
    }
    
    public void messageMods(String message) {
        String permission = "modreq.notice";
        message = buildMessage(message);
        this.getServer().broadcast(message, permission);

        Set<Permissible> subs = getServer().getPluginManager().getPermissionSubscriptions(permission);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission(permission) && !subs.contains(player)) {
                player.sendMessage(message);
            }
        }
    }
}
