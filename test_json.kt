import kotlinx.serialization.json.*

fun main() {
    try {
        val root = Json.parseToJsonElement("blank").jsonObject
    } catch(e: Exception) {
        println(e.message)
    }
}
