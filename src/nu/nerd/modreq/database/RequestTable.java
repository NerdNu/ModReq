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

/**
 * Represents the interaction between the requests table in the database and the plugin.
 *
 * @version 1.0
 * @since 3.0
 */
public class RequestTable {

	private final Dao<Request, Integer> requestDao;

	/**
	 * Creates a new {@code RequestTable} instance.
	 *
	 * @param plugin The main plugin instance, used for fetching the Request DAO.
	 */
	public RequestTable(ModReq plugin) {
		this.requestDao = plugin.getRequestDao();
	}

	/**
	 * Gets a list of requests a player has open or claimed
	 * @param uuid The UUID of the player being checked
	 * @return The list of requests of the player
	 */
	public CompletableFuture<List<Request>> getUserRequests(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try{
				List<Request> requests = requestDao.queryBuilder()
						.where()
						.eq("player_uuid", uuid)
						.and()
						.in("status", RequestStatus.OPEN, RequestStatus.CLAIMED)
						.query();
				return requests;
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch user requests", exception);
			}
		}).exceptionally(e -> {
			System.out.println("Unable to fetch user requests!\n" + e.getMessage());
			return new ArrayList<>();
		});
	}

	/**
	 * Gets a list of requests a player had completed for them while offline
	 * @param uuid The UUID of the player logging in
	 * @return The list of requests closed while a user was offline
	 */
	public CompletableFuture<List<Request>> getMissedClosedRequests(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return requestDao.queryBuilder()
						.where()
						.eq("player_uuid", uuid)
						.and()
						.eq("status", RequestStatus.CLOSED)
						.and()
						.eq("close_seen_by_user", false)
						.query();
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch missed closed requests", exception);
			}
		}).exceptionally(e -> {
			System.out.println("Unable to fetch missed closed requests!\n" + e.getMessage());
			return new ArrayList<>();
		});
	}

	/**
	 * Gets the number of open requests a user has
	 * @param uuid The UUID of the user being checked
	 * @return The number of open requests of the specified user
	 */
	public CompletableFuture<Integer> getNumRequestFromUser(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return (int) requestDao.queryBuilder()
						.where()
						.eq("player_uuid", uuid)
						.and()
						.eq("status", RequestStatus.OPEN)
						.countOf();
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch user " + uuid.toString() + "'s open modreq count", exception);
			}
		}).exceptionally(e -> {
			System.out.println("Unable to fetch user " + uuid.toString() + "'s open modreq count!\n" + e.getMessage());
			return 0;
		});
	}

	/**
	 * Gets the number of requests that match the specified criteria
	 * @param includeElevated If admin requests should be included
	 * @param searchTerm A term to search for in the requests
	 * @param statuses The statuses of the modreqs to check
	 * @return The number of requests that match
	 */
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
					where.and().eq("flag_for_admin", false);
				}

				return (int) queryBuilder.countOf();

			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch total request number", exception);
			}
		}).exceptionally(e -> {
			System.out.println("Unable to fetch total request number!\n" + e.getMessage());
			return 0;
		});
	}

	/**
	 * Gets the requests that are to be displayed on the page specified by the player
	 * @param page The page of requests that should be shown
	 * @param perPage How many requests should be shown per page
	 * @param includeElevated If admin requests should be included
	 * @param searchTerm A term to search for in the requests
	 * @param statuses The statuses of the modreqs to check
	 * @return A list of requests
	 */
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
					where.and().eq("flag_for_admin", false);
				}

				queryBuilder.offset((long) page * perPage).limit((long) perPage);
				return queryBuilder.query();

			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch requests for page " + page, exception);
			}
		}).exceptionally(e -> {
			System.out.println("Unable to fetch requests for page " + page + "!\n" + e.getMessage());
			return new ArrayList<>();
		});
	}

	/**
	 * Retrieves a specific request from the database
	 * @param id The ID of the request to get
	 * @return The request
	 */
	public CompletableFuture<Request> getRequest(int id) {
		return CompletableFuture.supplyAsync(() -> {
			try{
				return requestDao.queryForId(id);
			} catch(SQLException exception) {
				throw new RuntimeException("Unable to fetch request" + id, exception);
			}
		}).exceptionally(e -> {
			System.out.println("Unable to fetch request" + id + "!\n" + e.getMessage());
			return null;
		});
	}

	/**
	 * Saves the request to the database
	 * @param request The request being saved
	 */
	public void save(Request request) {
		CompletableFuture.runAsync(() -> {
			try {
				requestDao.createOrUpdate(request);
			} catch (SQLException exception) {
				if (request.getId() != 0) {
					throw new RuntimeException("Unable to save/update request" + request.getId(), exception);
				} else {
					throw new RuntimeException("Unable to save/update request made by " + request.getPlayer_name(), exception);
				}
			}
		}).exceptionally(e -> {
			if (request.getId() != 0) {
				System.out.println("Unable to save/update request!\n" + e.getMessage());
			} else {
				System.out.println("Unable to save/update request made by " + request.getPlayer_name() + "!\n" + e.getMessage());
			}
			return null;
		});
	}

	/**
	 * Saves the request to the database and returns the request for when the ID is needed
	 * @param request The request being saved
	 * @return The request after being saved
	 */
	public CompletableFuture<Request> saveAndGetId(Request request) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				requestDao.createOrUpdate(request);
				return request;
			} catch (SQLException exception) {
				if (request.getId() != 0) {
					throw new RuntimeException("Unable to save/update request" + request.getId(), exception);
				} else {
					throw new RuntimeException("Unable to save/update request made by " + request.getPlayer_name(), exception);
				}
			}
		}).exceptionally(e -> {
			if (request.getId() != 0) {
				System.out.println("Unable to save/update request!\n" + e.getMessage());
			} else {
				System.out.println("Unable to save/update request made by " + request.getPlayer_name() + "!\n" + e.getMessage());
			}
			return null;
		});
	}

}
