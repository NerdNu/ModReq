package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the in-game command /tpc.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class TPClaimCommand implements CommandHandler {

    private ClaimCommand claimCommand;
    private TPIdCommand tpIdCommand;
    private CheckCommand checkCommand;

    /**
     * Creates a new {@code TPClaimCommand} instance.
     */
    public TPClaimCommand(ClaimCommand claimCommand, TPIdCommand tpIdCommand, CheckCommand checkCommand) {
        this.claimCommand = claimCommand;
        this.tpIdCommand = tpIdCommand;
        this.checkCommand = checkCommand;
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {
        if(claimCommand.execute(player, name, args)) {
            tpIdCommand.execute(player, name, args);
            checkCommand.execute(player, name, args);
            return true;
        }
        return false;
    }
}
