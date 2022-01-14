package nu.nerd.modreq.database;

import io.ebean.ExpressionList;
import io.ebean.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request.RequestStatus;

public class RequestTable {

	ModReq plugin;

	public RequestTable(ModReq plugin) {
		this.plugin = plugin;
	}

	public List<Request> getUserRequests(UUID uuid) {
		List<Request> retVal = new ArrayList<>();

		Query<Request> query = plugin.getDatabase().find(Request.class).where()
				.ieq("playerUUID", uuid.toString())
				.in("status", RequestStatus.OPEN, RequestStatus.CLAIMED).query();

		if (query != null) {
			retVal.addAll(query.findList());
		}

		return retVal;
	}

	public List<Request> getMissedClosedRequests(UUID uuid) {
		List<Request> retVal = new ArrayList<>();

		Query<Request> query = plugin.getDatabase().find(Request.class).where().ieq("playerUUID", uuid.toString()).eq("status", RequestStatus.CLOSED).eq("closeSeenByUser", false).query();

		if (query != null) {
			retVal.addAll(query.findList());
		}

		return retVal;
	}

	public int getNumRequestFromUser(UUID uuid) {
		int retVal = 0;
		Query<Request> query = plugin.getDatabase().find(Request.class).where().ieq("playerUUID", uuid.toString()).in("status", RequestStatus.OPEN).query();

		if (query != null) {
			retVal = query.findCount();
		}

		return retVal;
	}

	public int getTotalRequest(boolean includeElevated, String searchTerm, RequestStatus... statuses) {
		int retVal = 0;

		ExpressionList<Request> expressions = plugin.getDatabase().find(Request.class).where().in("status", (Object[]) statuses);

		if (searchTerm != null) {
			expressions = expressions.where().ilike("request", "%" + searchTerm + "%");
		}

		if (!includeElevated) {
			expressions = expressions.where().eq("flagForAdmin", false);
		}

		Query<Request> query = expressions.query();

		if (query != null) {
			retVal = query.findCount();
		}

		return retVal;
	}

	public List<Request> getAllRequests() {
		Query<Request> query = plugin.getDatabase().find(Request.class);
		if (query != null) {
			return query.findList();
		}

		return null;
	}

	public List<Request> getRequestPage(int page, int perPage, boolean includeElevated, String searchTerm, RequestStatus... statuses) {
		List<Request> retVal = new ArrayList<>();

		ExpressionList<Request> expressions = plugin.getDatabase().find(Request.class).where().in("status", (Object[]) statuses);

		if (searchTerm != null) {
			expressions.where().ilike("request", "%" + searchTerm + "%");
		}

		if (!includeElevated) {
			expressions.where().eq("flagForAdmin", false);
		}

		Query<Request> query = expressions.query();

		if (query != null) {
			retVal.addAll(query.setMaxRows(perPage).setFirstRow(page * perPage).findPagedList().getList());
		}

		return retVal;
	}

	public Request getRequest(int id) {
		Request retVal = null;

		Query<Request> query = plugin.getDatabase().find(Request.class).where().eq("id", id).query();

		if (query != null) {
			retVal = query.findOne();
		}

		return retVal;
	}

	public void save(Request request) {
		plugin.getDatabase().save(request);
	}

}
