package nu.nerd.modreq.database;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import nu.nerd.modreq.ModReq;
import nu.nerd.modreq.database.Request.RequestStatus;

public class RequestTable {

	private final Dao<Request, Integer> requestDao;

	public RequestTable(ModReq plugin) {
		this.requestDao = plugin.getRequestDao();
	}

	public CompletableFuture<List<Request>> getUserRequests(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try{
				List<Request> requests = requestDao.queryBuilder()
						.where()
						.eq("playerUUID", uuid)
						.and()
						.in("status", RequestStatus.OPEN, RequestStatus.CLAIMED)
						.query();
				return requests;
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch user requests", exception);
			}
		});
	}

	public CompletableFuture<List<Request>> getMissedClosedRequests(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return requestDao.queryBuilder()
						.where()
						.eq("playerUUID", uuid)
						.and()
						.eq("status", RequestStatus.CLOSED)
						.and()
						.eq("closeSeenByUser", false)
						.query();
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch missed closed requests", exception);
			}
		});
	}

	public CompletableFuture<Integer> getNumRequestFromUser(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return (int) requestDao.queryBuilder()
						.where()
						.eq("playerUUID", uuid)
						.and()
						.eq("status", RequestStatus.OPEN)
						.countOf();
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch user " + uuid.toString() + "'s open modreq count", exception);
			}
		});
	}

	public CompletableFuture<Integer> getTotalRequest(boolean includeElevated, String searchTerm, RequestStatus... statuses) {
		return CompletableFuture.supplyAsync(() -> {
			QueryBuilder<Request, Integer> queryBuilder = requestDao.queryBuilder();
			Where<Request, Integer> where = queryBuilder.where();
			try {
				List<RequestStatus> statusList = new ArrayList<>(Arrays.asList(statuses));

				if(searchTerm != null) {
					statusList.add(RequestStatus.CLOSED);
				}

				where.in("status", statusList.toArray(new RequestStatus[0]));

				if(searchTerm != null) {
					where.and().like("request", "%" + searchTerm + "%");
				}

				if(!includeElevated) {
					where.and().eq("flagForAdmin", false);
				}

				System.out.println("There should be a number below this :thonk:");
				System.out.println((int) queryBuilder.countOf());
				return (int) queryBuilder.countOf();

			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch total request number", exception);
			}
		});
	}

	public CompletableFuture<List<Request>> getRequestPage(int page, int perPage, boolean includeElevated, String searchTerm, RequestStatus... statuses) {
		return CompletableFuture.supplyAsync(() -> {
			QueryBuilder<Request, Integer> queryBuilder = requestDao.queryBuilder();
			Where<Request, Integer> where = queryBuilder.where();
			try {
				List<RequestStatus> statusList = new ArrayList<>(Arrays.asList(statuses));

				if(searchTerm != null) {
					statusList.add(RequestStatus.CLOSED);
				}

				where.in("status", statusList.toArray(new RequestStatus[0]));

				if(searchTerm != null) {
					where.and().like("request", "%" + searchTerm + "%");
				}

				if(!includeElevated) {
					where.and().eq("flagForAdmin", false);
				}

				queryBuilder.offset((long) page * perPage).limit((long) perPage);
				return queryBuilder.query();

			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch requests for page " + page, exception);
			}
		});
	}

	public CompletableFuture<Request> getRequest(int id) {
		return CompletableFuture.supplyAsync(() -> {
			try{
				return requestDao.queryForId(id);
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch request" + id, exception);
			}
		});
	}

	public void save(Request request) {
		CompletableFuture.runAsync(() -> {
			try {
				requestDao.createOrUpdate(request);
			} catch (SQLException exception) {
				if (request.getId() != 0) {
					throw new RuntimeException("Unable to save/update request" + request.getId(), exception);
				} else {
					throw new RuntimeException("Unable to save/update request made by " + request.getPlayerName(), exception);
				}
			}
		});
	}

}
