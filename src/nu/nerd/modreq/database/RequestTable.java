package nu.nerd.modreq.database;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.Expr;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;

import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request.RequestStatus;

public class RequestTable {

	ModReq plugin;
	
	public RequestTable(ModReq plugin) {
		this.plugin = plugin;
	}
	
	public List<Request> getUserRequests(String username) {
		List<Request> retVal = new ArrayList<Request>();
		
		Query<Request> query = plugin.getDatabase().find(Request.class).where()
                        .ieq("playerName", username)
                        .in("status", RequestStatus.OPEN, RequestStatus.CLAIMED).query();
		
		if (query != null) {
			retVal.addAll(query.findList());
		}
		
		return retVal;
	}
	
	public List<Request> getMissedClosedRequests(String username) {
		List<Request> retVal = new ArrayList<Request>();
		
		Query<Request> query = plugin.getDatabase().find(Request.class).where().ieq("playerName", username).eq("status", RequestStatus.CLOSED).eq("closeSeenByUser", false).query();
		
		if (query != null) {
			retVal.addAll(query.findList());
		}
		
		return retVal;
	}
	
	public int getNumRequestFromUser(String username) {
		int retVal = 0;
		Query<Request> query = plugin.getDatabase().find(Request.class).where().ieq("playerName", username).in("status", RequestStatus.OPEN, RequestStatus.CLAIMED).query();
		
		if (query != null) {
			retVal = query.findRowCount();
		}
		
		return retVal;
	}
	
	public int getTotalRequest(boolean includeElevated, RequestStatus ... statuses) {
		int retVal = 0;
                
		ExpressionList<Request> expressions = plugin.getDatabase().find(Request.class).where().in("status", statuses);
                
                if (includeElevated)
                    expressions.where().eq("flagForAdmin", true);
                
                Query<Request> query = expressions.query();
		
		if (query != null) {
			retVal = query.findRowCount();
		}
		
		return retVal;
	}
	
	public List<Request> getRequestPage(int page, int perPage, boolean includeElevated, RequestStatus ... statuses) {
		List<Request> retVal = new ArrayList<Request>();

		ExpressionList<Request> expressions = plugin.getDatabase().find(Request.class).where().in("status", statuses);
                
                if (includeElevated)
                    expressions.where().eq("flagForAdmin", true);
                
                Query<Request> query = expressions.query();
		
		if (query != null) {
			retVal.addAll(query.findPagingList(perPage).getPage(page).getList());
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
