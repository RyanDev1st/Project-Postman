package vn.edu.vgu.smartlocker.server

import vn.edu.vgu.smartlocker.server.cabinet.Cabinets

/**
 * The two cabinets the front-ends already name, created on a fresh database.
 *
 * Run with `LOCKER_SEED=1` once. It is **not** a fake server and puts no
 * parcels in: a parcel appears when a shipper drops one at endpoint 11, the
 * same way it will on the day. Seeding parcels would mean the app showed
 * something no drop had ever created, and the first real drop would be the
 * first time that path had ever run.
 *
 * The ids are the ones already written into `src/cabinet/config.js` and into
 * the app's scan tests, so the three sides agree without anybody typing an id
 * twice.
 */
object Demo {

    private val CABINETS = listOf(
        Triple("vgu-back-gate", "VGU Back Gate", 20),
        Triple("vgu-library", "VGU Library", 12),
    )

    fun seed(db: Db) {
        CABINETS.forEach { (id, name, boxes) ->
            if (Cabinets.find(db, id) != null) {
                println("$id is already here - left alone")
                return@forEach
            }
            val key = Cabinets.create(db, id, name, boxes)
            println()
            println("  $id  ($name, $boxes boxes)")
            println("  CABINET KEY: $key")
        }
        println()
        println("Put the key for the cabinet this screen is, into src/cabinet/config.js.")
        println("It is stored hashed and cannot be printed again - use `cabinet rotate`.")
        println()
    }
}
