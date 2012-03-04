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
            if (args.length == 0) {
                return false;
            }

            StringBuilder request = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; i++) {
                request.append(" ").append(args[i]);
            }
            // submit a modreq here, no need for a permission check
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
            // do the teleport
        }
        else if (command.getName().equalsIgnoreCase("claim")) {
            if (args.length == 0) {
                return false;
            }
            // claim it
        }
        else if (command.getName().equalsIgnoreCase("unclaim")) {
            if (args.length == 0) {
                return false;
            }
            // unclaim it
        }
        else if (command.getName().equalsIgnoreCase("done")) {
            if (args.length == 0) {
                return false;
            }

            if (sender.hasPermission("modreq.done")) {
                // close it no matter what
            }
            else {
                // only close their own
            }
        }
        else if (command.getName().equalsIgnoreCase("reopen")) {
            if (args.length == 0) {
                return false;
            }
            // reopen it
        }

        return true;
    }
}
