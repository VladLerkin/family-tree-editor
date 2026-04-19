import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ContentPart

fun check(m: Message) {
    if (m is Message.Tool.Call) {
        println("Message.Tool.Call is a Message")
    }
}
fun check2(c: ContentPart) {
    if (c is Message.Tool.Call) {
        println("Message.Tool.Call is a ContentPart")
    }
}
