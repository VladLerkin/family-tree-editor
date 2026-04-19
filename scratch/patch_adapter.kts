import java.io.File
val f = File("core/src/commonMain/kotlin/com/family/tree/core/ai/koog/KoogModelAdapter.kt")
var text = f.readText()
text = text.replace(
    "aiMessages.add(AiMessage(",
    "println(\"[AI-DEBUG-PATCH] Adding AiMessage: role=\$role\"); aiMessages.add(AiMessage("
)
f.writeText(text)
