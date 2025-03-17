package nu.nerd.modreq.commands;

import nu.nerd.modreq.Configuration;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Note;
import nu.nerd.modreq.database.NoteTable;
import nu.nerd.modreq.database.RequestTable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nu.nerd.modreq.utils.MessageUtils.buildMessage;
import static nu.nerd.modreq.utils.MessageUtils.concatArgs;
import static nu.nerd.modreq.utils.RequestUtils.getRequest;

/**
 * Represents the in-game command /mr-note.
 * This class provides methods to process the data required by this command.
 * @version 1.0
 * @since 3.0
 */
public class MRNoteCommand implements CommandHandler {

    private final ModReq plugin;
    private RequestTable reqTable;
    private NoteTable noteTable;
    private Map<String, String> environment;
    private Configuration configuration;
    private CompletableFuture<Void> future;
    private Map<UUID, Integer> claimedIds;
    private BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    /**
     * Creates a new {@code MRNoteCommand} instance.
     *
     * @param plugin The main plugin instance, used for scheduling, environment variables, database access,
     *               and asynchronous operations.
     */
    public MRNoteCommand(ModReq plugin) {
        this.plugin = plugin;
        this.reqTable = plugin.getReqTable();
        this.noteTable = plugin.getNoteTable();
        this.environment = plugin.getEnvironment();
        this.configuration = plugin.getConfiguration();
        this.future = plugin.getCompleteableFuture();
        this.claimedIds = plugin.getClaimedIds();
    }

    @Override
    public boolean execute(Player player, String name, String[] args) {

        UUID playerUUID = player.getUniqueId();

        getRequest(player, playerUUID, args[1], true, claimedIds, reqTable, environment,
                configuration, plugin).thenAcceptAsync(request -> {
            if (request == null) {
                return;
            }
            bukkitScheduler.runTask(plugin, () -> {
                int requestId = request.getId();
                environment.put("request_id", Integer.toString(requestId));

                if (args[0].equalsIgnoreCase("add")) {
                    Note note = new Note();
                    note.setNoteBody(concatArgs(args, 2));
                    note.setPlayerUUID(playerUUID);
                    note.setPlayer(player.getName());
                    note.setRequestId(requestId);
                    noteTable.save(note);

                    player.sendMessage(buildMessage(configuration.MOD__NOTE_ADDED, environment, configuration));

                } else if (args[0].equalsIgnoreCase("remove")) {
                    int noteId;
                    try {
                        noteId = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        environment.put("note_id", args[2]);
                        player.sendMessage(buildMessage(configuration.MOD__NOTE_NUMBER, environment, configuration));
                        return;
                    }
                    environment.put("note_id", Integer.toString(noteId));

                    noteTable.getRequestNotes(request).thenAcceptAsync(notes -> {
                        bukkitScheduler.runTask(plugin, () -> {
                            int noteIndex = noteId - 1;
                            if (noteIndex < 0 || noteIndex >= notes.size()) {
                                player.sendMessage(buildMessage(configuration.MOD__NOTE_MISSING, environment, configuration));
                                return;
                            }

                            Note noteToRemove = notes.get(noteIndex);
                            noteTable.remove(noteToRemove);
                            player.sendMessage(buildMessage(configuration.MOD__NOTE_REMOVED, environment, configuration));
                        });
                    });
                }
            });
        });
        return true;
    }
}
