ModReq
======
Overview
--------
ModReq (**Mod**erator **Req**uest) is a help ticket management system for Minecraft.  Players make requests with the /modreq command, which saves the coordinates of the requesting player and the request text.  Staff can see all requests in a work queue and can select which request to handle next.

Requests can be in one of three states:
 * OPEN - The request has been made but is not yet being handled by staff.
 * CLAIMED - A staff member has started work on the request. A request can only be claimed by a single staff member at a time.
 * CLOSED - The request was completed (or denied).  Players can also close their own requests, even if not a staff member.

ModReq supports two tiers of staff permissions: Moderator and Admin.  Admins are assumed to have some superset of Moderator permissions.  By default, new requests are visible to all staff members (Moderators and Admins).  However, requests can be marked as requiring an Admin to handle them using the /elevate command.


Commands
--------
Note: In the discussion that follows, `#` signifies an number.

### `/modreq`

`/modreq <request>`
 * Make a request.  The coordinates of the player making the request are saved by the command.

### `/check`

`/check #`
 * Retrieve a full description of the request with the specified number.

`/check [--page # | -p # | p:#] [--admin | -a] [--search term | -s term]`
 * List all open requests matching specified criteria.  Requests are grouped into pages of (by default) 5 requests and `/check` only shows one page of requests at a time.
 * `/check` on its own lists all requests visible to the player. Non-staff players will only see open requests that they have made themselves. Moderators will see requests by other players, but not those that have been elevated. Admins will see all requests.
 * `--page`, `-p` or `p:` select a different page to show.  Pages are numbered starting at 1.
 * `--admin` or `-a` allow Admin requests to be visible to Moderators. Without one of these options, moderators will only see open requests that have not been elevated.
 * `--search` or `-s` allow a search term to be specified. Only requests whose text include the term will be listed.

### `/tp-id` or `/tpid`

`/tp-id #`
 * A staff-only command to teleport to the location of a request.

### `/claim`

`/claim #`
 * A staff-only command to claim exclusive access to a request.
 * The request is moved to the CLAIMED state and associated with a staff member.

### `/unclaim`

`/unclaim #`
 * Remove your claim on a request that you have previously claimed with `/claim`.
 * The request is returned to the OPEN state and not associated with any staff member.

### `/done`

`/done # [message]`
 * Mark a request as completed, optionally specifying a message that will be sent to the requesting player.
 * The request is moved to the CLOSED state.
 * Typical setups will grant non-staff players the `modreq.done` permission so that they can cancel their own requests using this command.

### `/reopen`

`/reopen #`
 * Reopen a closed request.
 * The request changes to the OPEN state.

### `/elevate`

`/elevate #`
 * Mark the request as requiring handling by an Admin.

### `/tpinfo` or `/tpi`

`/tpi #`
`/tpinfo #`
 * A staff command to teleport to and check the details of a request without claiming it.
 * This command is shorthand for `/tp-id #` followed by `/check #`.

### `/tpc`

`/tpc #`
 * A staff command that tries to claim a request and, _only if successful_, teleport to and check the details of the request.
 * An error message will be shown if another staff member has already claimed or closed the request.
 * If you have already claimed the request, the command acts as `/tpi`.
 * The command executes `/claim #`, and if successful `/tpi #`.

Permissions
-----------
 * `modreq.request` - Permission to submit a request (defaults to true).
 * `modreq.mod` - All Moderator permissions. Certain administrative commands are excluded.
   * `modreq.check` - Staff permission to check requests. Note that players lacking this permission can still check the status of requests they made themselves.
   * `modreq.claim` - Staff permission to claim a request.
   * `modreq.done` - Staff permission to close a request.
   * `modreq.teleport` - Staff permission to teleport to the location the request was made.
   * `modreq.notice` - Staff permission to receive broadcast messages about requests, e.g. new requests made.
 * `modreq.cleardb` - Admin permission to clear the request database.
 * `modreq.upgrade` - Admin permission to upgrade the request database from an older schema.
 * `modreq` - All permissions, including Moderator and Admin permssions (use with caution).
