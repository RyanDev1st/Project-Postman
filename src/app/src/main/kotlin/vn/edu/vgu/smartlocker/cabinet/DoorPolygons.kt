package vn.edu.vgu.smartlocker.cabinet

/**
 * Door corner projections, ported from scripts/cabinet-sim/hero/cabinet.json.
 * Normalised 0..1, origin top left, over the cabinet render.
 * Regenerate with hero.py whenever the camera or the drawing changes.
 * Corner order: top-left, top-right, bottom-right, bottom-left.
 */
val DOOR_POLYGONS: Map<String, List<Pair<Float, Float>>> = mapOf(
    "1" to listOf(0.30011f to 0.15805f, 0.15748f to 0.16627f, 0.15753f to 0.30908f, 0.30014f to 0.30447f),
    "2" to listOf(0.45339f to 0.14921f, 0.30319f to 0.15787f, 0.30322f to 0.30437f, 0.4534f to 0.29952f),
    "3" to listOf(0.61503f to 0.1399f, 0.45665f to 0.14903f, 0.45665f to 0.29941f, 0.61501f to 0.2943f),
    "4" to listOf(0.78572f to 0.13006f, 0.61846f to 0.1397f, 0.61844f to 0.29419f, 0.78567f to 0.28879f),
    "5" to listOf(0.30014f to 0.30802f, 0.15753f to 0.31254f, 0.15758f to 0.4553f, 0.30017f to 0.4544f),
    "7" to listOf(0.61501f to 0.29805f, 0.45665f to 0.30306f, 0.45666f to 0.4534f, 0.61499f to 0.45239f),
    "8" to listOf(0.78567f to 0.29264f, 0.61844f to 0.29794f, 0.61842f to 0.45237f, 0.78562f to 0.45131f),
    "9" to listOf(0.30017f to 0.45795f, 0.15758f to 0.45877f, 0.15763f to 0.60149f, 0.3002f to 0.60428f),
    "10" to listOf(0.45341f to 0.55715f, 0.30328f to 0.55548f, 0.30329f to 0.60434f, 0.45341f to 0.60728f),
    "11" to listOf(0.61499f to 0.45614f, 0.45666f to 0.45705f, 0.45667f to 0.60734f, 0.61497f to 0.61044f),
    "12" to listOf(0.78562f to 0.45516f, 0.61842f to 0.45612f, 0.6184f to 0.61051f, 0.78557f to 0.61378f),
    "13" to listOf(0.3002f to 0.60783f, 0.15763f to 0.60495f, 0.15768f to 0.74763f, 0.30023f to 0.75411f),
    "14" to listOf(0.45341f to 0.61092f, 0.30329f to 0.60789f, 0.30332f to 0.75425f, 0.45342f to 0.76108f),
    "15" to listOf(0.61497f to 0.61418f, 0.45667f to 0.61099f, 0.45667f to 0.76123f, 0.61495f to 0.76843f),
    "16" to listOf(0.78557f to 0.61763f, 0.6184f to 0.61425f, 0.61839f to 0.76859f, 0.78553f to 0.77619f),
    "17" to listOf(0.30023f to 0.75766f, 0.15768f to 0.75109f, 0.15773f to 0.89372f, 0.30026f to 0.9039f),
    "18" to listOf(0.45342f to 0.76473f, 0.30332f to 0.75781f, 0.30335f to 0.90412f, 0.45343f to 0.91484f),
    "19" to listOf(0.61495f to 0.77217f, 0.45667f to 0.76488f, 0.45668f to 0.91507f, 0.61493f to 0.92637f),
    "20" to listOf(0.78553f to 0.78004f, 0.61838f to 0.77233f, 0.61837f to 0.92662f, 0.78548f to 0.93855f),
)