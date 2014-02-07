/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nu.nerd.modreq;

public class Configuration {
    private ModReq plugin;
    
    public String GENERAL__PAGE_ERROR;
    public String GENERAL__REQUEST_NUMBER;
    public String GENERAL__REQUEST_FILED;
    public String GENERAL__MAX_REQUESTS;
    public String GENERAL__REQUEST_ERROR;
    public String GENERAL__NO_REQUESTS;
    public String GENERAL__CLOSE_ERROR;
    public String GENERAL__COMPLETED;
    public String GENERAL__COMPLETED_MESSAGE;
    public String GENERAL__DELETED;
    public String GENERAL__LIST__HEADER;
    public String GENERAL__LIST__ITEM;
    public String GENERAL__LIST__FOOTER;
    public String GENERAL__ITEM__HEADER_CLAIMED;
    public String GENERAL__ITEM__HEADER_UNCLAIMED;
    public String GENERAL__ITEM__DETAILS;
    public String GENERAL__ITEM__REQUEST;
    public String MOD__NEW_REQUEST;
    public String MOD__NO_REQUESTS;
    public String MOD__EMPTY_PAGE;
    public String MOD__TELEPORT;
    public String MOD__UNCLAIM;
    public String MOD__REQUEST_TAKEN;
    public String MOD__COMPLETED;
    public String MOD__COMPLETED_MESSAGE;
    public String MOD__DELETED;
    public String MOD__REOPENED;
    public String MOD__FLAGGED;
    public String MOD__RESET;
    public String MOD__ALREADY_CLOSED;
    public String COLOUR__ONLINE;
    public String COLOUR__OFFLINE;
    public Integer MAX_REQUESTS;
    
    public Configuration(ModReq plugin) {
        this.plugin = plugin;
    }
    
    public void save() {
        plugin.saveConfig();
    }
    
    public void load() {
        plugin.reloadConfig();
        
        GENERAL__REQUEST_NUMBER = plugin.getConfig().getString("messages.general.request-number");
        GENERAL__REQUEST_FILED = plugin.getConfig().getString("messages.general.request-filed");
        GENERAL__MAX_REQUESTS = plugin.getConfig().getString("messages.general.max-requests");
        GENERAL__REQUEST_ERROR = plugin.getConfig().getString("messages.general.request-error");
        GENERAL__NO_REQUESTS = plugin.getConfig().getString("messages.general.no-requests");
        GENERAL__CLOSE_ERROR = plugin.getConfig().getString("messages.general.page-error");
        GENERAL__COMPLETED = plugin.getConfig().getString("messages.general.completed");
        GENERAL__COMPLETED_MESSAGE = plugin.getConfig().getString("messages.general.completed-message");
        GENERAL__DELETED = plugin.getConfig().getString("messages.general.deleted");
        GENERAL__PAGE_ERROR = plugin.getConfig().getString("messages.page-error");
        GENERAL__ITEM__DETAILS = plugin.getConfig().getString("messages.general.item.details");
        GENERAL__ITEM__HEADER_CLAIMED = plugin.getConfig().getString("messages.general.item.header-claimed");
        GENERAL__ITEM__HEADER_UNCLAIMED = plugin.getConfig().getString("messages.general.item.header-unclaimed");
        GENERAL__ITEM__REQUEST = plugin.getConfig().getString("messages.general.item.request");
        GENERAL__LIST__FOOTER = plugin.getConfig().getString("messages.list.footer");
        GENERAL__LIST__HEADER = plugin.getConfig().getString("messages.list.header");
        GENERAL__LIST__ITEM = plugin.getConfig().getString("messages.list.item");
        MOD__COMPLETED = plugin.getConfig().getString("messages.mod.completed");
        MOD__COMPLETED_MESSAGE = plugin.getConfig().getString("messages.mod.completed-message");
        MOD__DELETED = plugin.getConfig().getString("messages.mod.deleted");
        MOD__EMPTY_PAGE = plugin.getConfig().getString("messages.mod.empty-page");
        MOD__FLAGGED = plugin.getConfig().getString("messages.mod.flagged");
        MOD__NEW_REQUEST = plugin.getConfig().getString("messages.mod.new-request");
        MOD__NO_REQUESTS = plugin.getConfig().getString("messages.mod.no-requests");
        MOD__REOPENED = plugin.getConfig().getString("messages.mod.reopened");
        MOD__RESET = plugin.getConfig().getString("messages.mod.reset");
        MOD__TELEPORT = plugin.getConfig().getString("messages.mod.teleport");
        MOD__UNCLAIM = plugin.getConfig().getString("messages.mod.unclaim");
        MOD__REQUEST_TAKEN = plugin.getConfig().getString("messages.mod.request-taken");
        MOD__ALREADY_CLOSED = plugin.getConfig().getString("messages.mod.already-closed");
        COLOUR__OFFLINE = plugin.getConfig().getString("colour.offline");
        COLOUR__ONLINE = plugin.getConfig().getString("colour.online");
        MAX_REQUESTS = plugin.getConfig().getInt("max-requests", 5);
    }
}