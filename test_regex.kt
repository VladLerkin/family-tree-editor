fun main() {
    val htmls = listOf(
        "<input type=\"hidden\" name=\"csrf\" value=\"token123\">",
        "<input value=\"token456\" type=\"hidden\" name=\"csrf\">",
        "<input type=\"hidden\" value=\"token789\" name=\"csrf\">",
        "<input name=\"csrf\" type=\"hidden\" value=\"tokenabc\">",
        "<input id=\"csrf\" name=\"csrf\" value=\"tokendef\" type=\"hidden\">"
    )

    fun extractByRegex(name: String, html: String): String? {
        val patterns = listOf(
            Regex("""name="$name"[^>]*?value="([^"]+)""""),
            Regex("""value="([^"]+)"[^>]*?name="$name"""")
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) }
    }

    htmls.forEach { html ->
        println("Testing: $html -> ${extractByRegex("csrf", html)}")
    }
}
