import com.family.tree.core.ai.AiMessage
import com.family.tree.core.ai.AiToolCall
import com.family.tree.core.ai.AiFunctionCall

// Let's just create a list of strings mimicking the roles.
fun runSim() {
    val input = listOf("system", "user", "tool.call A", "tool.result A", "tool.call B", "tool.call C", "tool.result B")
    
    val aiMessages = mutableListOf<String>()
    
    input.forEach { message ->
        when {
            message == "system" -> aiMessages.add("system")
            message == "user" -> aiMessages.add("user")
            message.startsWith("tool.result") -> aiMessages.add("tool (" + message.split(" ")[1] + ")")
            message.startsWith("tool.call") -> {
                val call = message.split(" ")[1]
                val last = aiMessages.lastOrNull()
                if (last != null && last.startsWith("assistant")) {
                    aiMessages[aiMessages.size - 1] = last + ", " + call
                } else {
                    aiMessages.add("assistant (" + call + ")")
                }
            }
        }
    }
    
    println(aiMessages)
}
runSim()
