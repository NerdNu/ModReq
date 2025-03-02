package nu.nerd.modreq.database;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import nu.nerd.modreq.ModReq;

public class NoteTable {

	private final Dao<Note, Integer> noteDao;

	public NoteTable(ModReq plugin) {
		this.noteDao = plugin.getNoteDao();
	}

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

	public CompletableFuture<Integer> getNoteCount(Request request) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return (int) noteDao.queryBuilder().where().eq("requestId", request.getId()).countOf();
			} catch(SQLException exception) {
				throw new RuntimeException("Failed to get note count", exception);
			}
		});
	}

	public void remove(Note note) {
		CompletableFuture.runAsync(() -> {
			try{
				noteDao.delete(note);
			} catch(SQLException exception) {
				throw new RuntimeException("Failed to delete note", exception);
			}
		});
	}

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
