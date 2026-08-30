package vn.edu.vgu.smartlocker.net

import org.json.JSONObject

/**
 * What comes back from the server, as things the app can hold.
 *
 * Parsing lives beside each type rather than in the screens. A screen that
 * reads `json.getString("box_number")` is a screen that breaks when the
 * Server team renames a field, and there is no one place to fix it.
 *
 * Every reader here is **lenient**: a missing field becomes empty or zero, not
 * an exception. The contract in `api-contract.md` is our proposal and has not
 * been agreed yet - see ADR 0005 - so the first real answers may not match it.
 * An app that crashes on a surprise field cannot report what the surprise was.
 */

/** A pass, and when it stops working. */
data class Session(val token: String, val expiresAt: String) {
    companion object {
        fun from(json: JSONObject) = Session(
            token = json.optString("token"),
            expiresAt = json.optString("expires_at"),
        )
    }
}

/**
 * One parcel waiting in one box.
 *
 * `boxNumber` is a **string**, not an int. The doors are labelled `01` to `20`
 * and that leading zero is on the cabinet, on the screen, and in the app. An
 * int would drop it and print `4` on a door that says `04`.
 */
data class Parcel(
    val id: String,
    val cabinetName: String,
    val boxNumber: String,
    val arrivedAt: String,
) {
    companion object {
        fun from(json: JSONObject) = Parcel(
            id = json.optString("id"),
            cabinetName = json.optString("cabinet_name"),
            boxNumber = json.optString("box_number"),
            arrivedAt = json.optString("arrived_at"),
        )
    }
}

/** One line of what happened, for the history screen. */
data class Event(
    val at: String,
    val boxNumber: String,
    val action: String,
    val cabinetName: String,
) {
    companion object {
        fun from(json: JSONObject) = Event(
            at = json.optString("at"),
            boxNumber = json.optString("box_number"),
            action = json.optString("action"),
            cabinetName = json.optString("cabinet_name"),
        )
    }
}

/**
 * The answer to "a door opened".
 *
 * This is the one the whole product turns on, so it is its own type rather
 * than a bare string. If the server ever needs to say more about an open -
 * which shelf, how long it will stay open - it is added here and the screens
 * follow.
 */
data class Opened(val boxNumber: String, val cabinetName: String) {
    companion object {
        fun from(json: JSONObject) = Opened(
            boxNumber = json.optString("box_number"),
            cabinetName = json.optString("cabinet_name"),
        )
    }
}

/**
 * The signed-in account, from endpoint 24.
 *
 * Both fields are read leniently: an older server answers 404 and the caller
 * keeps whatever it had, and a server that answers without a name gives an
 * empty one rather than a wrong one. A blank name means the app says nothing
 * about who you are, which is the honest state - it never invents one.
 */
data class Account(val name: String, val phone: String) {
    companion object {
        fun from(json: JSONObject) = Account(
            name = json.optString("full_name"),
            phone = json.optString("phone_number"),
        )
    }
}
