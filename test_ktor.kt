import java.net.HttpURLConnection
import java.net.URL

fun main() {
    val url = URL("https://pamyat-naroda.ru/heroes/")
    val conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
    try {
        val html = conn.inputStream.bufferedReader().use { it.readText() }
        println("HTML LENGTH: ${html.length}")
        val csrfPattern = Regex("""name="csrf"[^>]*?value="([^"]+)"""")
        val csrfPattern2 = Regex("""value="([^"]+)"[^>]*?name="csrf"""")
        val match1 = csrfPattern.find(html)
        val match2 = csrfPattern2.find(html)
        println("Match 1: ${match1?.groupValues?.get(1)}")
        println("Match 2: ${match2?.groupValues?.get(1)}")
        
        java.io.File("pamyat_naroda_test.html").writeText(html)
    } catch(e: Exception) {
        println("Error: ${e.message}")
        val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
        if (err != null) {
            java.io.File("pamyat_naroda_err.html").writeText(err)
            println("Saved error HTML.")
        }
    }
}
