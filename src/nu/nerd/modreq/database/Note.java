package nu.nerd.modreq.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

/**
 * Represents the note table in the database.
 *
 * @version 1.0
 * @since 3.0
 */
@DatabaseTable(tableName = "modreq_notes")
public class Note {

	/**
	 * The id of the note in the table.
	 */
	@DatabaseField(generatedId = true)
	private int id;

	/**
	 * The uuid of the player who created the note.
	 */
	@DatabaseField(canBeNull = false)
	private UUID playerUUID;

	/**
	 * The name of the player who created the note.
	 */
	@DatabaseField(canBeNull = false)
	private String player;

	/**
	 * The ID of the request that the note corresponds to.
	 */
	@DatabaseField(canBeNull = false)
	private int requestId;

	/**
	 * The content of the note.
	 */
	@DatabaseField
	private String noteBody;

	// ----------------------------------------------------------------------------

	public Note(){ /* Empty as ORMLite requires an empty constructor to initialize this ORM class. */ }

	// ----------------------------------------------------------------------------

	/**
	 * Returns the modeq's ID
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the modreq's ID
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Sets the player's UUID
	 * @param playerUUID The player's UUID
	 */
	public void setPlayerUUID(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	/**
	 * Returns the player's UUID
	 * @return the player's UUID
	 */
	public UUID getPlayerUUID() {
		return this.playerUUID;
	}

	/**
	 * Returns the player
	 * @return the player
	 */
	public String getPlayer() {
		return player;
	}

	/**
	 * Sets the player
	 * @param player the player to set
	 */
	public void setPlayer(String player) {
		this.player = player;
	}

	/**
	 * Returns the request ID
	 * @return the request ID
	 */
	public int getRequestId() {
		return requestId;
	}

	/**
	 * Sets the request ID
	 * @param requestId the request ID to set
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
