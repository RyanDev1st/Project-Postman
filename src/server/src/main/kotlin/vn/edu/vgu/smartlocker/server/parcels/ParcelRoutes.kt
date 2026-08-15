package vn.edu.vgu.smartlocker.server.parcels

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vn.edu.vgu.smartlocker.server.Db
import vn.edu.vgu.smartlocker.server.Ids
import vn.edu.vgu.smartlocker.server.Refusal
import vn.edu.vgu.smartlocker.server.asIso
import vn.edu.vgu.smartlocker.server.auth.Empty
import vn.edu.vgu.smartlocker.server.auth.Tokens
import vn.edu.vgu.smartlocker.server.auth.receiverId
import vn.edu.vgu.smartlocker.server.now
import vn.edu.vgu.smartlocker.server.num
import vn.edu.vgu.smartlocker.server.refuse
import vn.edu.vgu.smartlocker.server.str

/**
 * Endpoints 5 to 8 - what the receiver's phone may ask.
 *
 * Every route here starts by turning a token into a receiver id, and every
 * query below is filtered by that id. There is no route that takes a receiver
 * id as a parameter, which is what stops one person reading another's parcels:
 * the identity comes from the credential, never from the request body.
 *
 * Field names are the app's, from `net/Models.kt`. `box_number` is a string,
 * because the doors are labelled `01` to `20` and an int would print `4` on a
 * door that says `04`.
 */
fun Route.parcelRoutes(db: Db, tokens: Tokens, collect: Collect) {

    /** 5. What is waiting for me, and where. */
    get("/parcels") {
        val me = call.receiverId(tokens)
        val parcels = db.rows(
            """SELECT p.id AS id, c.name AS cabinet_name, p.box_number AS box_number,
                      p.arrived_at AS arrived_at
                 FROM parcels p JOIN cabinets c ON c.id = p.cabinet_id
                WHERE p.receiver_id = ? AND p.state IN ('waiting', 'opening')
                ORDER BY p.arrived_at DESC""",
            me,
        ) {
            ParcelJson(
                id = it.str("id"),
                cabinetName = it.str("cabinet_name"),
                boxNumber = it.str("box_number"),
                arrivedAt = it.num("arrived_at").asIso(),
            )
        }
        call.respond(ParcelsResponse(parcels))
    }

    /**
     * 6. **The main path.** Scanned the cabinet's QR; open my box.
     *
     * The whole decision is [Collect.byScan]. This route's only jobs are to
     * find out who is asking and to turn the answer into the contract's words.
     */
    post("/parcels/collect") {
        val me = call.receiverId(tokens)
        val asked = call.receive<CollectRequest>()

        when (val outcome = collect.byScan(me, asked.sessionCode)) {
            is Collect.Result.Opening ->
                call.respond(OpenedResponse(outcome.boxNumber, outcome.cabinetName))
            Collect.Result.SessionExpired -> call.refuse(Refusal.SESSION_EXPIRED)
            Collect.Result.NoParcelHere -> call.refuse(Refusal.NO_PARCEL_HERE)
            Collect.Result.BoxFaulty -> call.refuse(Refusal.BOX_FAULTY)
        }
    }

    /** 7. What happened before. Read from the log, which is never edited. */
    get("/parcels/history") {
        val me = call.receiverId(tokens)
        val events = db.rows(
            """SELECT e.at AS at, e.box_number AS box_number, e.action AS action,
                      c.name AS cabinet_name
                 FROM events e
                 LEFT JOIN cabinets c ON c.id = e.cabinet_id
                WHERE e.receiver_id = ? ORDER BY e.at DESC LIMIT 200""",
            me,
        ) {
            EventJson(
                at = it.num("at").asIso(),
                boxNumber = it.str("box_number"),
                action = it.str("action"),
                // Joined, not stored on the event. A cabinet that is renamed
                // should read by its new name everywhere, including in what
                // already happened - the row records where, not what it was
                // called that day. LEFT, so a deleted cabinet still lists.
                cabinetName = it.str("cabinet_name"),
            )
        }
        call.respond(EventsResponse(events))
    }

    /** 8. Where to send this person's notifications. */
    post("/devices") {
        val me = call.receiverId(tokens)
        val asked = call.receive<DeviceRequest>()
        if (asked.deviceId.isNotBlank()) {
            db.exec(
                """INSERT INTO devices (receiver_id, device_id, registered_at) VALUES (?, ?, ?)
                   ON CONFLICT(receiver_id, device_id) DO UPDATE SET registered_at = excluded.registered_at""",
                me, asked.deviceId, now(),
            )
        }
        call.respond(Empty())
    }
}

@Serializable
data class CollectRequest(@SerialName("session_code") val sessionCode: String = "")

@Serializable
data class DeviceRequest(@SerialName("device_id") val deviceId: String = "")

@Serializable
data class ParcelJson(
    val id: String,
    @SerialName("cabinet_name") val cabinetName: String,
    @SerialName("box_number") val boxNumber: String,
    @SerialName("arrived_at") val arrivedAt: String,
)

@Serializable
data class ParcelsResponse(val parcels: List<ParcelJson>)

@Serializable
data class EventJson(
    val at: String,
    @SerialName("box_number") val boxNumber: String,
    val action: String,
    @SerialName("cabinet_name") val cabinetName: String = "",
)

@Serializable
data class EventsResponse(val events: List<EventJson>)

@Serializable
data class OpenedResponse(
    @SerialName("box_number") val boxNumber: String,
    @SerialName("cabinet_name") val cabinetName: String,
)
