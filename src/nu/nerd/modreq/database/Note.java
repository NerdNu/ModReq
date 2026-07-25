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
	@DatabaseField(canBeNull = false, columnName = "player_uuid")
	private UUID player_uuid;

	/**
	 * The name of the player who created the note.
	 */
	@DatabaseField(canBeNull = false)
	private String player;

	/**
	 * The ID of the request that the note corresponds to.
	 */
	@DatabaseField(canBeNull = false, columnName = "request_id")
	private int request_id;

	/**
	 * The content of the note.
	 */
	@DatabaseField(columnName = "note_body")
	private String note_body;

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
	 * @param player_uuid The player's UUID
	 */
	public void setPlayer_uuid(UUID player_uuid) {
		this.player_uuid = player_uuid;
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
	 * Sets the request ID
	 * @param request_id the request ID to set
	 */
	public void setRequest_id(int request_id) {
		this.request_id = request_id;
	}

	/**
	 * @return the noteBody
	 */
	public String getNote_body() {
		return note_body;
	}

	/**
	 * @param note_body the noteBody to set
	 */
	public void setNote_body(String note_body) {
		this.note_body = note_body;
	}
}
