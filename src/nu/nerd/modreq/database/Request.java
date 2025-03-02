package nu.nerd.modreq.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "modreq_requests")
public class Request {

	public enum RequestStatus {
		/**
		 * The request has been resolved and is closed.
		 */
		CLOSED,
		/**
		 * The request has been claimed by a moderator.
		 */
		CLAIMED,
		/**
		 * The request has yet to be handled and can be claimed.
		 */
		OPEN
	}

	@DatabaseField(generatedId = true)
	private int id;

	@DatabaseField(canBeNull = false)
	private UUID playerUUID;
	@DatabaseField(canBeNull = false)
	private String playerName;

	@DatabaseField
	private UUID assignedModUUID;
	@DatabaseField
	private String assignedMod;

	@DatabaseField
	private String request;

	@DatabaseField(canBeNull = false)
	private long requestTime;

	@DatabaseField(canBeNull = false)
	private RequestStatus status;

	@DatabaseField(canBeNull = false)
	private String requestLocation;
	@DatabaseField
	private String closeMessage;
	@DatabaseField
	private long closeTime;
	@DatabaseField
	private boolean closeSeenByUser;
	@DatabaseField
	private boolean flagForAdmin;

	public Request() {}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public void setPlayerUUID(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	public UUID getPlayerUUID() {
		return this.playerUUID;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getPlayerName() {
		return this.playerName;
	}

	public void setAssignedModUUID(UUID assignedModUUID) {
		this.assignedModUUID = assignedModUUID;
	}

	public UUID getAssignedModUUID() {
		return this.assignedModUUID;
	}

	public void setAssignedMod(String assignedMod) {
		this.assignedMod = assignedMod;
	}

	public String getAssignedMod() {
		return this.assignedMod;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getRequest() {
		return this.request;
	}

	public void setRequestTime(long requestTime) {
		this.requestTime = requestTime;
	}

	public long getRequestTime() {
		return this.requestTime;
	}

	public void setStatus(RequestStatus status) {
		this.status = status;
	}

	public RequestStatus getStatus() {
		return this.status;
	}

	public void setRequestLocation(String requestLocation) {
		this.requestLocation = requestLocation;
	}

	public String getRequestLocation() {
		return this.requestLocation;
	}

	public void setCloseMessage(String closeMessage) {
		this.closeMessage = closeMessage;
	}

	public String getCloseMessage() {
		return this.closeMessage;
	}

	public void setCloseTime(long closeTime) {
		this.closeTime = closeTime;
	}

	public long getCloseTime() {
		return this.closeTime;
	}

	public void setCloseSeenByUser(boolean closeSeenByUser) {
		this.closeSeenByUser = closeSeenByUser;
	}

	public boolean isCloseSeenByUser() {
		return this.closeSeenByUser;
	}

	public void setFlagForAdmin(boolean flagForAdmin) {
		this.flagForAdmin = flagForAdmin;
	}

	public boolean isFlagForAdmin() {
		return this.flagForAdmin;
	}
}
