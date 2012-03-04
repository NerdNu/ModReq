package nu.nerd.modreq;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ModReq extends JavaPlugin {

    @Override
    public void onEnable() {
        // setup persistence stuff here
    }

    @Override
    public void onDisable() {
        // tear down
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("modreq")) {
            // submit a modreq here, no need for a permission check
            return true;
        }

        return false;
    }
}
