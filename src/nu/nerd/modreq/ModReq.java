package nu.nerd.modreq;

import io.ebean.Database;
import io.ebean.SqlRow;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import nu.nerd.BukkitEbean.EbeanBuilder;
import nu.nerd.modreq.database.Note;
import nu.nerd.modreq.database.NoteTable;
import nu.nerd.modreq.database.Request;
import nu.nerd.modreq.database.Request.RequestStatus;
import nu.nerd.modreq.database.RequestTable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.gestern.bukkitmigration.UUIDFetcher;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ModReq extends JavaPlugin {

	ModReqListener listener = new ModReqListener(this);
	Configuration config = new Configuration(this);
	Map<String, String> environment = new HashMap<>();

	/**
	 * Map from UUID of staff member to integer ID of most recently claimed request.
	 *
	 * This acts as the default request ID when handling "/check -", "/tp-id -" and "/tpinfo -", and synonyms.
	 *
	 * This collection is saved in most-recent-claims.yml across restarts.
	 */
	Map<UUID, Integer> claimedIds = new HashMap<>();
	File claimsFile;

	Database db;
	RequestTable reqTable;
	NoteTable noteTable;

	@Override
	public void onEnable() {
		setupDatabase();
		File configFile = new File(this.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			getConfig().options().copyDefaults(true);
			saveConfig();
		}

		config.load();

		claimsFile = new File(getDataFolder(), "most-recent-claims.yml");
		loadClaimedIds();

		reqTable = new RequestTable(this);
		noteTable = new NoteTable(this);
		getServer().getPluginManager().registerEvents(listener, this);
	}

	@Override
	public void onDisable() {
		saveClaimedIds();
	}

	public boolean setupDatabase() {
		try {
			db = new EbeanBuilder(this).setClasses(getDatabaseClasses()).build();
            db.find(Request.class).findCount();
            db.find(Note.class).findCount();
		} catch (PersistenceException ex) {
			getLogger().log(Level.INFO, "First run, please initialize database manually. Run the following:");
			db = new EbeanBuilder(this).setDdlGenerate(true).setClasses(getDatabaseClasses()).build();
			getLogger().log(Level.INFO, """
                          CREATE TABLE modreq_notes (
							id                        integer primary key,
							player_uuid               varchar(40) not null,
							player                    varchar(255) not null,
							request_id                integer not null,
							note_body                 varchar(255)
                          );
                          """);
			getLogger().log(Level.INFO, """
                          CREATE TABLE modreq_requests (
							id                        integer primary key,
							player_uuid               varchar(40) not null,
							player_name               varchar(255) not null,
							assigned_mod_uuid         varchar(40),
							assigned_mod              varchar(255),
							request                   varchar(255),
							request_time              bigint not null,
							status                    integer not null,
							request_location          varchar(255) not null,
							close_message             varchar(255),
							close_time                bigint,
							close_seen_by_user        integer(1),
							flag_for_admin            integer(1),
							constraint ck_modreq_requests_status check (status in (0,1,2))
                          );
                          """);
			return true;
		}

		return false;
	}

	@SuppressWarnings("SleepWhileInLoop")
	public void resetDatabase() {
		getLogger().log(Level.INFO, "Resetting database");

		getLogger().log(Level.INFO, "Backup up existing data into memory");
		List<SqlRow> rowRequests = getDatabase().sqlQuery("SELECT id, player_name, assigned_mod, request, request_time, status, request_location, close_message, close_time, close_seen_by_user, flag_for_admin FROM modreq_requests").findList();
		List<SqlRow> rowNotes = getDatabase().sqlQuery("SELECT id, player, request_id, note_body FROM modreq_notes").findList();

		List<Request> reqs = new ArrayList<>();
		List<Note> notes = new ArrayList<>();
		Set<String> unknownNames = new HashSet<>();

//        getLogger().log(Level.INFO, "Executing remove ddl");
//        EbeanHelper.removeDDL(db);
		if (setupDatabase()) {
			getLogger().log(Level.INFO, "Schema created, converting {0} requests and {1} notes", new Object[]{rowRequests.size(), rowNotes.size()});
			for (SqlRow row : rowRequests) {
				Request req = new Request();
				req.setId(row.getInteger("id"));
				if (row.containsKey("player_uuid")) {
					req.setPlayerUUID(row.getUUID("player_uuid"));
				}
				req.setPlayerName(row.getString("player_name"));
				req.setRequest(row.getString("request"));
				req.setRequestTime(row.getInteger("request_time"));
				req.setRequestLocation(row.getString("request_location"));
				req.setStatus(RequestStatus.values()[row.getInteger("status")]);
				if (req.getStatus() == RequestStatus.CLAIMED) {
					if (row.containsKey("assigned_mod_uuid")) {
						req.setAssignedModUUID(row.getUUID("assigned_mod_uuid"));
					}
				}
				req.setAssignedMod(row.getString("assigned_mod"));
				req.setFlagForAdmin(row.getBoolean("flag_for_admin"));

				if (req.getPlayerUUID() == null && req.getPlayerName() != null) {
					unknownNames.add(req.getPlayerName());
				}
				if (req.getAssignedModUUID() == null && req.getAssignedMod() != null) {
					unknownNames.add(req.getAssignedMod());
				}

				reqs.add(req);
			}

			for (SqlRow row : rowNotes) {
				Note note = new Note();
				note.setId(row.getInteger("id"));
				note.setPlayer(row.getString("player"));
				note.setRequestId(row.getInteger("request_id"));
				note.setNoteBody(row.getString("note_body"));

				if (note.getPlayerUUID() == null && note.getPlayer() != null) {
					unknownNames.add(note.getPlayer());
				}

				notes.add(note);
			}

			if (!unknownNames.isEmpty()) {
				getLogger().log(Level.INFO, "Fetching {0} UUIDs", unknownNames.size());
				try {
					List<String> names = new ArrayList<>(unknownNames);
					UUIDFetcher fetcher = new UUIDFetcher(names);
					Map<String, UUID> responses = fetcher.call();

					List<String> namesChanged = new ArrayList<>();
					for (String name : names) {
						if (!responses.containsKey(name)) {
							namesChanged.add(name);
						}
					}
					getLogger().log(Level.INFO, "Failed to lookup {0} uuids, querying for history", namesChanged.size());
					final JSONParser jsonParser = new JSONParser();
					int i = 0;
					for (String name : namesChanged) {
						try {
							URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name + "?at=1422774069");
							HttpURLConnection connection = (HttpURLConnection) url.openConnection();
							connection.setRequestProperty("Content-Type", "application/json");
							connection.setUseCaches(false);
							connection.setDoInput(true);
							connection.setDoOutput(true);

							JSONObject profile = (JSONObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
							//String nameNew = (String) profile.get("name");
							String uuidStringNoDash = (String) profile.get("id");
							String uuidString = uuidStringNoDash.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
							UUID uuid = UUID.fromString(uuidString);
							responses.put(name, uuid);
							//getLogger().info("New Name = " + nameNew + " UUID = " + uuid);
						} catch (Exception e) {
							getLogger().log(Level.INFO, "Failed to fetch historical uuid for {0}", name);
						}

						i++;
						if (i > 600) {
							Thread.sleep(100L);
						}
					}

					for (Map.Entry<String, UUID> response : responses.entrySet()) {
						for (Request req : reqs) {
							if (req.getPlayerName() != null && req.getPlayerName().equalsIgnoreCase(response.getKey())) {
								req.setPlayerUUID(response.getValue());
							}
							if (req.getAssignedMod() != null && req.getAssignedMod().equalsIgnoreCase(response.getKey())) {
								req.setAssignedModUUID(response.getValue());
							}
						}

						for (Note note : notes) {
							if (note.getPlayer() != null && note.getPlayer().equalsIgnoreCase(response.getKey())) {
								note.setPlayerUUID(response.getValue());
							}
						}
					}
				} catch (Exception e) {
					getLogger().log(Level.SEVERE, "Failed to fetch uuids", e);
				}
			}

			getLogger().log(Level.INFO, "Saving {0} reqs", reqs.size());
			int i = 0;
			for (Request req : reqs) {
				try {
					i++;

					reqTable.save(req);

					if (i % 1000 == 0) {
						getLogger().log(Level.INFO, "Saved {0} of {1} reqs", new Object[]{i, reqs.size()});
						Thread.sleep(1000L);
					}
				} catch (Exception e) {
					getLogger().log(Level.SEVERE, "Failed to save ModReq id={0} player={1}", new Object[]{req.getId(), req.getPlayerName()});
					getLogger().log(Level.SEVERE, e.getMessage());
				}
			}
			getLogger().log(Level.INFO, "Saved {0} of {1} reqs", new Object[]{i, reqs.size()});

			getLogger().log(Level.INFO, "Saving {0} notes", notes.size());
			i = 0;
			for (Note note : notes) {
				try {
					i++;

					noteTable.save(note);

					if (i % 1000 == 0) {
						getLogger().log(Level.INFO, "Saved {0} of {1} notes", new Object[]{i, notes.size()});
						Thread.sleep(1000L);
					}
				} catch (Exception e) {
					getLogger().log(Level.SEVERE, "Failed to save note id={0} player={1}", new Object[]{note.getId(), note.getPlayer()});
					getLogger().log(Level.SEVERE, e.getMessage());
				}
			}
			getLogger().log(Level.INFO, "Saved {0} of {1} notes", new Object[]{i, notes.size()});
		}
		getLogger().log(Level.INFO, "Done");
	}

	public ArrayList<Class<?>> getDatabaseClasses() {
		ArrayList<Class<?>> list = new ArrayList<>();
		list.add(Request.class);
		list.add(Note.class);
		return list;
	}

	public Database getDatabase() {
		return db;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		String senderName = ChatColor.stripColor(sender.getName());
		UUID senderUUID = null;
		Player player = null;
		if (sender instanceof Player player1) {
			player = player1;
			senderUUID = player.getUniqueId();
		}
		environment.clear();
		if (sender instanceof ConsoleCommandSender) {
			senderName = "Console";
		}
		if (command.getName().equalsIgnoreCase("modreq")) {
			if (args.length == 0) {
				return false;
			}
			modreq(sender, senderName, player, args);
		} else if (command.getName().equalsIgnoreCase("check")) {
			check(sender, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("tp-id")) {
			if (args.length == 0) {
				return false;
			}
			tpId(sender, senderUUID, player, args);
		} else if (command.getName().equalsIgnoreCase("tpinfo")) {
			if (args.length == 0) {
				return false;
			}
			tpId(sender, senderUUID, player, args);
			check(sender, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("tpc")) {
			if (args.length == 0) {
				return false;
			}
			if (claim(sender, senderName, senderUUID, args)) {
				tpId(sender, senderUUID, player, args);
				check(sender, senderUUID, args);
			}
		} else if (command.getName().equalsIgnoreCase("claim")) {
			if (args.length == 0) {
				return false;
			}
			claim(sender, senderName, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("unclaim")) {
			if (args.length == 0) {
				return false;
			}
			unclaim(sender, senderName, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("done")) {
			if (args.length == 0) {
				return false;
			}
			done(sender, senderName, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("reopen")) {
			if (args.length == 0) {
				return false;
			}
			reopen(sender, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("elevate")) {
			if (args.length == 0) {
				return false;
			}
			elevate(sender, senderName, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("mr-reset")) {
			try {
				resetDatabase();
				sendMessage(sender, config.MOD__RESET);
			} catch (Exception ex) {
				getLogger().log(Level.WARNING, "Failed to reset database", ex);
			}
		} else if (command.getName().equalsIgnoreCase("mr-note")) {
			if (args.length < 3) {
				return false;
			}
			return mrNote(sender, senderName, senderUUID, args);
		} else if (command.getName().equalsIgnoreCase("mr-upgrade")) {
			mrUpgrade();
		}
		return true;
	}

	private void modreq(CommandSender sender, String senderName, Player player, String[] args) {
		if (!(sender instanceof Player)) {
			return;
		}

		if (reqTable.getNumRequestFromUser(player.getUniqueId()) < config.MAX_REQUESTS) {
			Request req = new Request();
			req.setPlayerUUID(player.getUniqueId());
			req.setPlayerName(senderName);
			String r = ChatColor.translateAlternateColorCodes('&', concatArgs(args, 0));
			r = ChatColor.stripColor(r);
			req.setRequest(r);
			req.setRequestTime(System.currentTimeMillis());
			String location = String.format("%s,%f,%f,%f,%f,%f", player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
			req.setRequestLocation(location);
			req.setStatus(RequestStatus.OPEN);

			reqTable.save(req);
			environment.put("request_id", String.valueOf(req.getId()));
			messageMods(config.MOD__NEW_REQUEST);
			sendMessage(sender, config.GENERAL__REQUEST_FILED);
		} else {
			environment.put("max_requests", Integer.toString(config.MAX_REQUESTS));
			sendMessage(sender, config.GENERAL__MAX_REQUESTS);
		}
	}

	private void check(CommandSender sender, UUID senderUUID, String[] args) {
		// Setting page > 0 triggers a page listing.
		int page = 1;
		int requestId = 0;
		int totalRequests = 0;
		String searchTerm = null;
		UUID limitUUID = null;
		boolean showNotes = true;
		boolean includeElevated = sender.hasPermission("modreq.cleardb");

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equalsIgnoreCase("--admin") || arg.equalsIgnoreCase("-a")) {
				includeElevated = true;
			} else if (arg.startsWith("p:")) {
				page = Integer.parseInt(arg.substring(2));
			} else if (arg.equalsIgnoreCase("--page") || arg.equalsIgnoreCase("-p")) {
				if (i + 1 > args.length) {
					sendMessage(sender, config.GENERAL__PAGE_ERROR);
					return;
				} else {
					try {
						page = Integer.parseInt(args[i + 1]);
						i++;
					} catch (NumberFormatException ex) {
						sendMessage(sender, config.GENERAL__PAGE_ERROR);
						return;
					}
				}
			} else if (arg.equalsIgnoreCase("--search") || arg.equalsIgnoreCase("-s")) {
				if (i + 1 < args.length) {
					searchTerm = args[i + 1];
					i++;
				} else {
					sendMessage(sender, config.GENERAL__SEARCH_ERROR);
					return;
				}
			} else if (arg.equals("-")) {
				Integer claimedId = claimedIds.get(senderUUID);
				if (claimedId != null) {
					requestId = claimedId;
					page = 0;
				}
			} else {
				try {
					requestId = Integer.parseInt(arg);
					page = 0;
				} catch (NumberFormatException ex) {
					sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
					return;
				}
			}
		}

		if (!sender.hasPermission("modreq.check")) {
			if (sender instanceof Player) {
				limitUUID = senderUUID;
			}
			showNotes = false;
		}

		List<Request> requests = new ArrayList<>();

		if (page > 0) {
			if (limitUUID != null) {
				requests.addAll(reqTable.getUserRequests(limitUUID));
				totalRequests = requests.size();
			} else {
				requests.addAll(reqTable.getRequestPage(page - 1, config.PAGE_SIZE, includeElevated, searchTerm, RequestStatus.OPEN, RequestStatus.CLAIMED));
				totalRequests = reqTable.getTotalRequest(includeElevated, searchTerm, RequestStatus.OPEN, RequestStatus.CLAIMED);
			}
		} else if (requestId > 0) {
			Request req = reqTable.getRequest(requestId);
			if (req == null) {
				sendMessage(sender, config.GENERAL__REQUEST_ERROR);
				return;
			} else {
				totalRequests = 1;
				if (limitUUID != null && req.getPlayerUUID().equals(limitUUID)) {
					requests.add(req);
				} else if (limitUUID == null) {
					requests.add(req);
				} else {
					totalRequests = 0;
				}
			}
		}

		if (totalRequests == 0) {
			if (limitUUID != null) {
				if (requestId > 0) {
					sendMessage(sender, config.GENERAL__REQUEST_ERROR);
				} else {
					sendMessage(sender, config.GENERAL__NO_REQUESTS);
				}
			} else {
				sendMessage(sender, config.MOD__NO_REQUESTS);
			}
		} else if (totalRequests == 1 && requestId > 0) {
			messageRequestToPlayer(sender, requests.get(0), showNotes);
		} else if (totalRequests > 0) {
			if (page > 1 && requests.isEmpty()) {
				sendMessage(sender, config.MOD__EMPTY_PAGE);
			} else {
				boolean showPage = true;
				if (limitUUID != null) {
					showPage = false;
				}
				messageRequestListToPlayer(sender, requests, page, totalRequests, showPage);
			}
		} else {
			// there was an error.
		}
	}

	private void tpId(CommandSender sender, UUID senderUUID, Player player, String[] args) {
		if (!(sender instanceof Player)) {
			return;
		}

		Request req = getRequest(sender, senderUUID, args[0], true);
		if (req == null) {
			return;
		}

		environment.put("request_id", String.valueOf(req.getId()));
		sendMessage(player, config.MOD__TELEPORT);
		Location loc = stringToLocation(req.getRequestLocation());
		player.teleport(loc);
	}

	/**
	 * Do the work of the /claim command.
	 *
	 * @return true if the request was successfully claimed by the sender now or previously; false if closed or already
	 * claimed by some other player.
	 */
	private boolean claim(CommandSender sender, String senderName, UUID senderUUID, String[] args) {
		Request req = getRequest(sender, senderUUID, args[0], false);
		if (req == null) {
			return false;
		}
		int requestId = req.getId();

		if (req.getStatus() == RequestStatus.OPEN) {
			req.setStatus(RequestStatus.CLAIMED);
			req.setAssignedModUUID(senderUUID);
			req.setAssignedMod(senderName);
			reqTable.save(req);

			environment.put("mod", senderName);
			environment.put("request_id", String.valueOf(requestId));
			messageMods(config.MOD__REQUEST_TAKEN);
			environment.remove("mod");
			environment.remove("request_id");
			claimedIds.put(senderUUID, requestId);
			return true;

		} else if (req.getStatus() == RequestStatus.CLOSED) {
			sendMessage(sender, config.MOD__ALREADY_CLOSED);

		} else if (req.getStatus() == RequestStatus.CLAIMED) {
			if (req.getAssignedModUUID().equals(senderUUID)) {
				// Already claimed by command sender. Update most recent claim.
				claimedIds.put(senderUUID, requestId);
				return true;
			} else {
				sendMessage(sender, config.MOD__ALREADY_CLAIMED);
			}
		}
		return false;
	}

	private void unclaim(CommandSender sender, String senderName, UUID senderUUID, String[] args) {
		Request req = getRequest(sender, senderUUID, args[0], true);
		if (req == null) {
			return;
		}

		if (req.getStatus() == RequestStatus.CLAIMED && req.getAssignedModUUID().equals(senderUUID)) {

			req.setStatus(RequestStatus.OPEN);
			req.setAssignedModUUID(null);
			req.setAssignedMod(null);
			reqTable.save(req);

			environment.put("mod", senderName);
			environment.put("request_id", String.valueOf(req.getId()));
			messageMods(config.MOD__UNCLAIM);
			environment.remove("mod");
			environment.remove("request_id");
		}
	}

	private void done(CommandSender sender, String senderName, UUID senderUUID, String[] args) {
		Request req = getRequest(sender, senderUUID, args[0], true);
		if (req == null) {
			return;
		}
		int requestId = req.getId();

		String doneMessage = concatArgs(args, 1);
		boolean successfullyClosed = true;

		if (req.getStatus() == RequestStatus.CLOSED) {
			sendMessage(sender, config.MOD__ALREADY_CLOSED);
			successfullyClosed = false;
		} else {
			// Moderator doing /done.
			if (sender.hasPermission("modreq.done")) {
				environment.put("mod", senderName);
				environment.put("request_id", String.valueOf(requestId));
				messageMods(config.MOD__COMPLETED);
				environment.remove("request_id");
				environment.remove("mod");

				if (doneMessage.length() != 0) {
					environment.put("close_message", doneMessage);
					messageMods(config.MOD__COMPLETED_MESSAGE);
					environment.remove("close_message");
				}
			} else {
				// Player doing /done.
				if (req.getPlayerUUID() != null && !req.getPlayerUUID().equals(senderUUID)) {
					sendMessage(sender, config.GENERAL__CLOSE_ERROR);
					successfullyClosed = false;
				}
			}
		}

		if (successfullyClosed) {
			req.setStatus(RequestStatus.CLOSED);
			req.setCloseTime(System.currentTimeMillis());
			req.setCloseMessage(doneMessage);
			req.setAssignedModUUID(senderUUID);
			req.setAssignedMod(senderName);

			Player requestCreator = getServer().getPlayerExact(req.getPlayerName());
			if (requestCreator != null) {
				// Message request creator immediately if online.
				req.setCloseSeenByUser(true);

				if (requestCreator.getUniqueId().equals(senderUUID)) {
					// Request closed by player.
					if (!sender.hasPermission("modreq.done")) {
						environment.put("request_id", String.valueOf(requestId));
						messageMods(config.MOD__DELETED);
						sendMessage(sender, config.GENERAL__DELETED);
						environment.remove("request_id");
					}
				} else {
					// Request closed by moderator.
					environment.put("close_message", doneMessage);
					environment.put("mod", senderName);
					environment.put("request_id", String.valueOf(requestId));
					if (doneMessage.length() != 0) {
						sendMessage(requestCreator, config.GENERAL__COMPLETED_MESSAGE);
					} else {
						sendMessage(requestCreator, config.GENERAL__COMPLETED);
					}
					environment.remove("close_message");
					environment.remove("mod");
					environment.remove("request_id");
				}
			}
			reqTable.save(req);
		}
	}

	private void reopen(CommandSender sender, UUID senderUUID, String[] args) {
		Request req = getRequest(sender, senderUUID, args[0], true);
		if (req == null) {
			return;
		}

		if (req.getStatus() == RequestStatus.CLOSED
				|| (req.getStatus() == RequestStatus.CLAIMED && req.getAssignedModUUID().equals(senderUUID))) {
			req.setStatus(RequestStatus.OPEN);
			req.setAssignedModUUID(null);
			req.setAssignedMod(null);
			req.setCloseSeenByUser(false);
			reqTable.save(req);

			environment.put("mod", sender.getName());
			environment.put("request_id", String.valueOf(req.getId()));
			messageMods(config.MOD__REOPENED);
			environment.remove("mod");
			environment.remove("request_id");
		}
	}

	private void elevate(CommandSender sender, String senderName, UUID senderUUID, String[] args) {
		unclaim(sender, senderName, senderUUID, args);

		Request req = getRequest(sender, senderUUID, args[0], true);
		if (req == null) {
			return;
		}
		if (req.getStatus() == RequestStatus.OPEN) {
			req.setFlagForAdmin(true);
			environment.put("request_id", String.valueOf(req.getId()));
			messageMods(config.MOD__FLAGGED);
			environment.remove("request_id");
			reqTable.save(req);
		} else if (req.getStatus() == RequestStatus.CLAIMED) {
			// When someone else has already claimed, unclaim() won't work.
			sendMessage(sender, config.MOD__ALREADY_CLAIMED);
		} else if (req.getStatus() == RequestStatus.CLOSED) {
			sendMessage(sender, config.MOD__ALREADY_CLOSED);
		}
	}

	/**
	 * Handle "/mr-note add req text" and "/mr-note remove req noteid".
	 *
	 * @return true if the command has the correct syntax, so usage should not be shown.
	 */
	private boolean mrNote(CommandSender sender, String senderName, UUID senderUUID, String[] args) {
		Request request = getRequest(sender, senderUUID, args[1], true);
		if (request == null) {
			return true;
		}
		int requestId = request.getId();
		environment.put("request_id", Integer.toString(requestId));

		if (args[0].equalsIgnoreCase("add")) {
			Note note = new Note();
			note.setNoteBody(concatArgs(args, 2));
			note.setPlayerUUID(senderUUID);
			note.setPlayer(senderName);
			note.setRequestId(requestId);
			noteTable.save(note);

			sender.sendMessage(buildMessage(config.MOD__NOTE_ADDED));
			return true;

		} else if (args[0].equalsIgnoreCase("remove")) {
			int noteId;
			try {
				noteId = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				environment.put("note_id", args[2]);
				sender.sendMessage(buildMessage(config.MOD__NOTE_NUMBER));
				return true;
			}
			environment.put("note_id", Integer.toString(noteId));

			List<Note> notes = noteTable.getRequestNotes(request);
			int noteIndex = noteId - 1;
			if (noteIndex < 0 || noteIndex >= notes.size()) {
				sender.sendMessage(buildMessage(config.MOD__NOTE_MISSING));
				return true;
			}

			Note noteToRemove = notes.get(noteIndex);
			noteTable.remove(noteToRemove);
			sender.sendMessage(buildMessage(config.MOD__NOTE_REMOVED));
			return true;

		} else {
			return false;
		}
	}

	private void mrUpgrade() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this, this::resetDatabase, 0L);
	}

	/**
	 * Parse the specified command argument (usually args[0] in onCommand()) as an integer request ID and return the
	 * corresponding Request.
	 *
	 * If useLastClaimedId is true, an ID of "-" is accepted as a reference to the most recently claimed request
	 *
	 * Errors are messaged to the command sender in the case of a malformed request ID, or if the ID does not correspond
	 * to a database entry.
	 *
	 * @param arg the command argument to parse.
	 * @param senderUUID UUID of the CommandSender.
	 * @param useLastClaimedId if true, then an arg of "-" is considered to be a synonym for the ID of the most recently
	 * claimed request.
	 * @return the corresponding Request, or null if not found.
	 */
	private Request getRequest(CommandSender sender, UUID senderUUID, String arg, boolean useLastClaimedId) {
		int requestId = 0;

		if (arg.equals("-")) {
			if (useLastClaimedId) {
				Integer claimedId = claimedIds.get(senderUUID);
				if (claimedId != null) {
					requestId = claimedId;
				}
			}
		} else {
			try {
				requestId = Integer.parseInt(arg);
			} catch (NumberFormatException ex) {
			}
		}

		if (requestId == 0) {
			sendMessage(sender, config.GENERAL__REQUEST_NUMBER);
			return null;
		}

		Request req = reqTable.getRequest(requestId);
		if (req == null) {
			sendMessage(sender, config.GENERAL__REQUEST_ERROR);
		}
		return req;
	}

	private void loadClaimedIds() {
		YamlConfiguration ymlConfiguration = new YamlConfiguration();
		try {
			if (claimsFile.isFile()) {
				ymlConfiguration.load(claimsFile);
				for (String uuidString : ymlConfiguration.getKeys(false)) {
					try {
						claimedIds.put(UUID.fromString(uuidString), ymlConfiguration.getInt(uuidString));
					} catch (Exception ex) {
					}
				}
			}
		} catch (FileNotFoundException ex) {
			getLogger().log(Level.WARNING, "cannot read {0}", claimsFile.getPath());
		} catch (IOException ex) {
			getLogger().log(Level.WARNING, "error reading {0}", claimsFile.getPath());
		} catch (InvalidConfigurationException ex) {
			getLogger().log(Level.WARNING, "cannot parse {0}", claimsFile.getPath());
		}
	}

	private void saveClaimedIds() {
		YamlConfiguration ymlConfiguration = new YamlConfiguration();
		for (Entry<UUID, Integer> claim : claimedIds.entrySet()) {
			ymlConfiguration.set(claim.getKey().toString(), claim.getValue());
		}

		try {
			ymlConfiguration.save(claimsFile);
		} catch (IOException ex) {
			getLogger().log(Level.WARNING, "most recently claimed requests could not be saved in {0}", claimsFile.getPath());
		}
	}

	private String concatArgs(String[] args, int first) {
		StringBuilder builder = new StringBuilder();
		String sep = "";
		for (int i = first; i < args.length; ++i) {
			builder.append(sep).append(args[i]);
			sep = " ";
		}
		return builder.toString();
	}

	private Location stringToLocation(String requestLocation) {
		String[] split = requestLocation.split(",");
		String world = split[0];
		double x = Double.parseDouble(split[1]);
		double y = Double.parseDouble(split[2]);
		double z = Double.parseDouble(split[3]);

		if (split.length > 4) {
			float yaw = Float.parseFloat(split[4]);
			float pitch = Float.parseFloat(split[5]);
			return new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
		} else {
			return new Location(getServer().getWorld(world), x, y, z);
		}
	}

	private String timestampToDateString(long timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		SimpleDateFormat format = new SimpleDateFormat(config.DATE_FORMAT);
		return format.format(cal.getTime());
	}

	public String buildMessage(String inputMessage) {
		String message = inputMessage;

		for (Map.Entry<String, String> entry : environment.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key.equalsIgnoreCase("player")) {
				if (getServer().getPlayerExact(value) != null) {
					value = config.COLOUR_ONLINE + value;
				} else {
					value = config.COLOUR_OFFLINE + value;
				}
			}

			message = message.replace("{" + key + "}", value);
		}

		message = ChatColor.translateAlternateColorCodes('&', message);
		return message;
	}

	private void messageRequestToPlayer(CommandSender sender, Request req, boolean showNotes) {
		List<String> messages = new ArrayList<>();
		Location loc = stringToLocation(req.getRequestLocation());
		String location = String.format("%s, %d, %d, %d", loc.getWorld().getName(), Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));

		environment.put("status", req.getStatus().toString());
		environment.put("request_id", String.valueOf(req.getId()));
		if (req.getStatus() == RequestStatus.CLAIMED) {
			environment.put("mod", req.getAssignedMod());
			messages.add(buildMessage(config.GENERAL__ITEM__HEADER_CLAIMED));
			environment.remove("mod");
		} else {
			messages.add(buildMessage(config.GENERAL__ITEM__HEADER_UNCLAIMED));
		}
		environment.remove("status");
		environment.remove("request_id");
		environment.put("player", req.getPlayerName());
		environment.put("time", timestampToDateString(req.getRequestTime()));
		environment.put("location", location);
		messages.add(buildMessage(config.GENERAL__ITEM__DETAILS));
		environment.remove("player");
		environment.remove("time");
		environment.remove("location");
		environment.put("request_message", req.getRequest());
		messages.add(buildMessage(config.GENERAL__ITEM__REQUEST));
		environment.remove("request_message");

		if (showNotes) {
			List<Note> notes = noteTable.getRequestNotes(req);

			int i = 1;
			for (Note note : notes) {
				environment.put("id", Integer.toString(i));
				environment.put("user", note.getPlayer());
				environment.put("message", note.getNoteBody());
				messages.add(buildMessage(config.GENERAL__ITEM__NOTE));

				i++;
			}
			environment.remove("id");
			environment.remove("player");
			environment.remove("message");
		}

		sender.sendMessage(messages.toArray(new String[1]));
	}

	private void messageRequestListToPlayer(CommandSender sender, List<Request> reqs, int page, int totalRequests, boolean showPage) {
		List<String> messages = new ArrayList<>();

		environment.put("num_requests", String.valueOf(totalRequests));
		messages.add(buildMessage(config.GENERAL__LIST__HEADER));
		environment.remove("num_requests");
		for (Request r : reqs) {
			int noteCount = noteTable.getNoteCount(r);
			try {
				environment.put("request_id", String.valueOf(r.getId()));
				environment.put("note_count", noteCount > 0 ? ChatColor.RED + " [" + Integer.toString(noteCount) + "]" : "");
				environment.put("admin", (r.isFlagForAdmin() ? (ChatColor.AQUA + " [ADMIN]") : ""));
				environment.put("mod", (r.getStatus() == RequestStatus.CLAIMED ? (r.getAssignedMod()) : ""));
				environment.put("status", (r.getStatus() != RequestStatus.CLAIMED ? (r.getStatus().toString()) : ""));
				environment.put("time", timestampToDateString(r.getRequestTime()));
				environment.put("player", r.getPlayerName());
				environment.put("request_message", r.getRequest());
				messages.add(buildMessage(config.GENERAL__LIST__ITEM));
				environment.remove("request_id");
				environment.remove("note_count");
				environment.remove("admin");
				environment.remove("mod");
				environment.remove("status");
				environment.remove("time");
				environment.remove("player");
				environment.remove("request_message");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		if (showPage) {
			int numpages = (int) Math.ceil(totalRequests / (float) config.PAGE_SIZE);
			environment.put("page", String.valueOf(page));
			environment.put("num_pages", String.valueOf(numpages));
			messages.add(buildMessage(config.GENERAL__LIST__FOOTER));
			environment.remove("page");
			environment.remove("num_pages");
		}

		sender.sendMessage(messages.toArray(new String[1]));
	}

	public void sendMessage(CommandSender sender, String message) {
		message = buildMessage(message);
		sender.sendMessage(message);
	}

	public void messageMods(String message) {
		String permission = "modreq.notice";
		message = buildMessage(message);
		this.getServer().broadcast(message, permission);

		Set<Permissible> subs = getServer().getPluginManager().getPermissionSubscriptions(permission);
		for (Player player : getServer().getOnlinePlayers()) {
			if (player.hasPermission(permission) && !subs.contains(player)) {
				player.sendMessage(message);
			}
		}
	}
}
