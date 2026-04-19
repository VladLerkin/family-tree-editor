import java.io.File

val f = File("core/src/commonMain/kotlin/com/family/tree/core/ai/koog/KoogModelAdapter.kt")
var text = f.readText()
text = text.replace(
    """onLog("[AI-DEBUG] Building prompt from ${'$'}{prompt.messages.size} messages...")""",
    """onLog("[AI-DEBUG] Building prompt from ${'$'}{prompt.messages.size} messages...")
        prompt.messages.forEachIndexed { i, m -> onLog("  Msg ${'$'}i: ${'$'}{m::class.simpleName}") }"""
)
f.writeText(text)
