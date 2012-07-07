package nu.nerd.modreq;

import java.util.List;
import java.util.HashMap;
import org.bukkit.ChatColor;

public class Configuration
{

    private final ModReq plugin;

    public String MOD_ALERT_NEW_REQUEST;
    public String PLAYER_MADE_NEW_REQUEST;
    public String PLAYER_MAX_REQUESTS;
    public String CHECK_INVALID_REQUEST_NUMBER;
    public String CHECK_INVALID_PAGE_NUMBER;
    public String CHECK_REQUEST_NOT_FOUND;
    public String PLAYER_CHECK_EMPTY;
    public String MOD_CHECK_EMPTY;
    public String CHECK_PAGE_EMPTY;
    public String MOD_TELEPORTING;
    public String MOD_INVALID_REQUEST_NUMBER;
    public String MOD_REQUEST_CLAIMED;
    public String MOD_REQUEST_UNCLAIMED;
    public String MOD_REQUEST_ALREADY_CLOSED;
    public String MOD_REQUEST_COMPLETED;
    public String MOD_CLOSE_MESSAGE;
    public String MOD_REQUEST_NOT_CLAIMED;
    public String PLAYER_MOD_REQUEST_FROM;
    public String PLAYER_MOD_REQUEST_FILED;
    public String PLAYER_MOD_REQUEST_MESSAGE;
    public String PLAYER_REQUEST_COMPLETED;
    public String PLAYER_REQUEST_COMPLETED_WITH_MESSAGE;
    public String MOD_ALERT_REQUEST_COMPLETED;
    public String MOD_REQUEST_CLOSED;
    public String MOD_ALERT_REQUEST_REOPENED;
    public String MOD_REQUEST_ELEVATED;
    public String MOD_UNCLAIM_TO_ELEVATE;
    public String MOD_DATABASE_RESET;
    public String MOD_REQUEST_LIST_HEADER;
    public String MOD_REQUEST_LIST_ITEM;
    public String MOD_REQUEST_LIST_FOOTER;
    public String MOD_ALERT_OPEN_REQUESTS;
    public String ADMIN_COLOR;
    public String OFFLINE_COLOR;
    public String ONLINE_COLOR;

    public Configuration(ModReq instance)
    {
        plugin = instance;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void save()
    {
        plugin.saveConfig();
    }

    public void load()
    {
        plugin.reloadConfig();
        org.bukkit.configuration.ConfigurationSection config = plugin.getConfig();
        // Load messages from config
        MOD_ALERT_NEW_REQUEST = colorize(config.getString("mod-alert-new-request"));
        MOD_ALERT_OPEN_REQUESTS = colorize(config.getString("mod-alert-open-requests"));
        PLAYER_MADE_NEW_REQUEST = colorize(config.getString("player-made-new-request"));
        PLAYER_MAX_REQUESTS = colorize(config.getString("player-max-requests"));
        CHECK_INVALID_REQUEST_NUMBER = colorize(config.getString("check-invalid-request-number"));
        CHECK_INVALID_PAGE_NUMBER = colorize(config.getString("check-invalid-page-number"));
        CHECK_REQUEST_NOT_FOUND = colorize(config.getString("check-request-not-found"));
        PLAYER_CHECK_EMPTY = colorize(config.getString("player-check-empty"));
        MOD_CHECK_EMPTY = colorize(config.getString("mod-check-empty"));
        CHECK_PAGE_EMPTY = colorize(config.getString("check-page-empty"));
        MOD_TELEPORTING = colorize(config.getString("mod-teleporting"));
        MOD_INVALID_REQUEST_NUMBER = colorize(config.getString("mod-invalid-request-number"));
        MOD_REQUEST_CLAIMED = colorize(config.getString("mod-request-claimed"));
        MOD_REQUEST_UNCLAIMED = colorize(config.getString("mod-request-unclaimed"));
        MOD_REQUEST_ALREADY_CLOSED = colorize(config.getString("mod-request-already-closed"));
        MOD_REQUEST_COMPLETED = colorize(config.getString("mod-request-completed"));
        MOD_CLOSE_MESSAGE = colorize(config.getString("mod-close-message"));
        MOD_REQUEST_NOT_CLAIMED = colorize(config.getString("mod-request-not-claimed"));
        PLAYER_REQUEST_COMPLETED = colorize(config.getString("player-request-completed"));
        PLAYER_REQUEST_COMPLETED_WITH_MESSAGE = colorize(config.getString("player-request-completed-with-message"));
        MOD_ALERT_REQUEST_COMPLETED = colorize(config.getString("mod-alert-request-completed"));
        MOD_REQUEST_CLOSED = colorize(config.getString("mod-request-closed"));
        MOD_ALERT_REQUEST_REOPENED = colorize(config.getString("mod-alert-request-reopened"));
        MOD_REQUEST_ELEVATED = colorize(config.getString("mod-request-elevated"));
        MOD_UNCLAIM_TO_ELEVATE = colorize(config.getString("mod-unclaim-to-elevate"));
        MOD_DATABASE_RESET = colorize(config.getString("mod-database-reset"));
        PLAYER_MOD_REQUEST_FROM = colorize(config.getString("player-mod-request-from"));
        PLAYER_MOD_REQUEST_FILED = colorize(config.getString("player-mod-request-filed"));
        PLAYER_MOD_REQUEST_MESSAGE = colorize(config.getString("player-mod-request-message"));
        MOD_REQUEST_LIST_HEADER = colorize(config.getString("mod-request-list-header"));
        MOD_REQUEST_LIST_ITEM = colorize(config.getString("mod-request-list-item"));
        MOD_REQUEST_LIST_FOOTER = colorize(config.getString("mod-request-list-footer"));
        ADMIN_COLOR = colorize(config.getString("admin-color"));
        ONLINE_COLOR = colorize(config.getString("online-color"));
        OFFLINE_COLOR = colorize(config.getString("offline-color"));
    }
}