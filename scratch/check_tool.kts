import ai.koog.agents.core.tools.annotations.Tool
import kotlin.reflect.full.*

fun main() {
    println("Inspect Tool Annotation:")
    val constructors = Tool::class.constructors
    for (c in constructors) {
        println("  Constructor: $c")
    }
    val properties = Tool::class.memberProperties
    for (p in properties) {
        println("  Property: ${p.name} -> ${p.returnType}")
    }
}
main()
