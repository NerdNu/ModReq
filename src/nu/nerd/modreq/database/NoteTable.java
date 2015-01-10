package nu.nerd.modreq.database;

import com.avaje.ebean.Query;
import java.util.ArrayList;
import java.util.List;

import nu.nerd.modreq.ModReq;

public class NoteTable {
    
    private ModReq parent;
    
    public NoteTable(ModReq parent) {
        this.parent = parent;
    }
    
    public List<Note> getRequestNotes(Request request) {
        List<Note> requestNotes = new ArrayList<Note>();
        
        Query<Note> query = parent.getDatabase().find(Note.class).where()
                .eq("requestId", request.getId())
                .query();
        
        if(query != null) {
            requestNotes.addAll(query.findList());
        }
        
        return requestNotes;
    }

    public int getNoteCount(Request request) {

        return parent.getDatabase().find(Note.class).where()
                .eq("requestId", request.getId()).findRowCount();
    }
    
    public void remove(Note note) {
        parent.getDatabase().delete(note);
    }
    
    public void save(Note note) {
        parent.getDatabase().save(note);
    }
    
}
