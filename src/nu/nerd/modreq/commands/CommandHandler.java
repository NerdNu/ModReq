package nu.nerd.modreq.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Defines the structure for handling commands.
 * Any implementations of this interface should execute commands.
 *
 * @version 1.0
 * @since 3.0
 */
public interface CommandHandler {

    /**
     * Executes a command.
     *
     * @param player The player executing the command.
     * @param name The name of the command.
     * @param args The arguments of the command.
     * @return {@code true} if the command was successful, {@code false} if not.
     */
    boolean execute(Player player, String name, String[] args);
}
