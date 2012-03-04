package nu.nerd.modreq.database;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;

import nu.nerd.modreq.ModReq;

public class RequestTable {

	ModReq plugin;
	
	public RequestTable(ModReq plugin) {
		this.plugin = plugin;
	}
	
	public List<Request> getUserRequests(String username) {
		List<Request> retVal = new ArrayList<Request>();
		
		Query<Request> query = plugin.getDatabase().find(Request.class).where().ieq("playerName", username).eq("status", 1).query();
		
		if (query != null) {
			retVal.addAll(query.findList());
		}
		
		return retVal;
	}
	
	public int getNumRequestFromUser(String username) {
		int retVal = 0;
		Query<Request> query = plugin.getDatabase().find(Request.class).where().ieq("playerName", username).query();
		
		if (query != null) {
			retVal = query.findRowCount();
		}
		
		return retVal;
	}
	
	public PagingList<Request> getRequestPager(int perPage, int status) {
		PagingList<Request> retVal = null;
		
		Query<Request> query = plugin.getDatabase().find(Request.class).where().eq("status", Request.RequestStatus.OPEN).query();
		
		if (query != null) {
			retVal = query.findPagingList(perPage);
		}
		
		return retVal;
	}
	
	public Request getRequest(int id) {
		Request retVal = null;
		
		Query<Request> query = plugin.getDatabase().find(Request.class).where().eq("id", id).query();
		
		if (query != null) {
			retVal = query.findUnique();
		}
		
		return retVal;
	}
	
	public void save(Request request) {
		plugin.getDatabase().save(request);
	}
	
}
