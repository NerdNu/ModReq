package nu.nerd.modreq.database;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import nu.nerd.modreq.ModReq;

/**
 * Represents the interaction between the notes table in the database and the plugin.
 *
 * @version 1.0
 * @since 3.0
 */
public class NoteTable {

	/**
	 * The data access object for the notes table.
	 */
	private final Dao<Note, Integer> noteDao;

	/**
	 * Creates a new {@code NoteTable} instance.
	 *
	 * @param plugin The main plugin instance, used for fetching the Note DAO.
	 */
	public NoteTable(ModReq plugin) {
		this.noteDao = plugin.getNoteDao();
	}

	/**
	 * Gets the notes attached to a request.
	 * @param request The request being queried.
	 * @return The notes the specified request has attached to it.
	 */
	public CompletableFuture<List<Note>> getRequestNotes(Request request) {
		return CompletableFuture.supplyAsync(() -> {
			try{
				QueryBuilder<Note, Integer> queryBuilder = noteDao.queryBuilder();
				queryBuilder.where().eq("requestId", request.getId());
				return noteDao.query(queryBuilder.prepare());
			} catch(SQLException exception) {
				throw new RuntimeException("Failed  to fetch request notes!", exception);
			}
		});
	}

	/**
	 * Gets the amount of notes a request has.
	 * @param request The request being queried.
	 * @return The amount of notes the specified request has.
	 */
	public CompletableFuture<Integer> getNoteCount(Request request) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return (int) noteDao.queryBuilder().where().eq("requestId", request.getId()).countOf();
			} catch(SQLException exception) {
				throw new RuntimeException("Failed to get note count", exception);
			}
		});
	}

	/**
	 * Removes the note from the database.
	 * @param note The note being removed.
	 */
	public void remove(Note note) {
		CompletableFuture.runAsync(() -> {
			try{
				noteDao.delete(note);
			} catch(SQLException exception) {
				throw new RuntimeException("Failed to delete note", exception);
			}
		});
	}

	/**
	 * Saves the note to the database.
	 * @param note The note being saved.
	 */
	public void save(Note note) {
		CompletableFuture.runAsync(() -> {
			try{
				noteDao.createOrUpdate(note);
			} catch(SQLException exception) {
				throw new RuntimeException("Failed to save note", exception);
			}
		});
	}

}
