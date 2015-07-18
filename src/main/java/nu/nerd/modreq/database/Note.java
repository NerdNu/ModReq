package nu.nerd.modreq.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.UUID;

@Entity
@Table(name = "modreq_notes")
public class Note {
    
    /**
     * The id of the note in the table.
     */
    @Id
    private int id;
	
    /**
     * The uuid of the player who created the note.
     */
    @NotNull
    private UUID playerUUID;
	
    /**
     * The name of the player who created the note.
     */
    @NotNull
    private String player;
    
    /**
     * The ID of the request that the note corresponds to.
     */
    @NotNull
    private int requestId;
    
    /**
     * The content of the note.
     */
    @NotEmpty
    private String noteBody;

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

	public void setPlayerUUID(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}
	
	public UUID getPlayerUUID() {
		return this.playerUUID;
	}

    /**
     * @return the player
     */
    public String getPlayer() {
        return player;
    }

    /**
     * @param player the player to set
     */
    public void setPlayer(String player) {
        this.player = player;
    }

    /**
     * @return the requestId
     */
    public int getRequestId() {
        return requestId;
    }

    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    /**
     * @return the noteBody
     */
    public String getNoteBody() {
        return noteBody;
    }

    /**
     * @param noteBody the noteBody to set
     */
    public void setNoteBody(String noteBody) {
        this.noteBody = noteBody;
    }
}
