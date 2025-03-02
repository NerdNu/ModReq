package nu.nerd.modreq.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

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

	public Note(){}

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
