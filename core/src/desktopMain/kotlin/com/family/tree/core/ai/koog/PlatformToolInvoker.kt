package com.family.tree.core.ai.koog

import com.family.tree.core.ai.koog.KoogAgent.ToolCall
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.instanceParameter

/**
 * JVM (Desktop) implementation using reflection to discover and invoke tools.
 */
actual class PlatformToolInvoker actual constructor() {
    actual fun getToolDocumentation(tools: List<Any>): String {
        return tools.joinToString("\n\n") { tool ->
            val className = tool::class.simpleName ?: "Unknown"
            val methods = tool::class.memberFunctions
                .filter { method -> method.annotations.any { it is Tool } }
                .joinToString("\n") { method ->
                    val toolAnnotation = method.annotations.find { it is Tool } as Tool
                    val params = method.parameters.drop(1).joinToString(", ") { p ->
                        val optional = if (p.isOptional) "?" else ""
                        "${p.name}$optional: ${p.type}"
                    }
                    "- ${method.name}($params): ${toolAnnotation.description}"
                }
            "Tool Class: $className\nMethods:\n$methods"
        }
    }

    actual suspend fun invokeTool(tools: List<Any>, call: ToolCall): String {
        val tool = if (call.className != null) {
            tools.find { it::class.simpleName == call.className }
                ?: return "Error: Tool class ${call.className} not found."
        } else {
            tools.find { t ->
                t::class.memberFunctions.any { it.name == call.methodName && it.annotations.any { a -> a is Tool } }
            } ?: return "Error: Method ${call.methodName} not found in any available tools."
        }

        return try {
            val method = tool::class.memberFunctions.find { it.name == call.methodName }
                ?: return "Error: Method ${call.methodName} not found in ${call.className}."

            // Build a KParameter -> value map (callBy handles optional params automatically)
            val argsMap = mutableMapOf<kotlin.reflect.KParameter, Any?>()
            argsMap[method.instanceParameter!!] = tool

            for (p in method.parameters.drop(1)) { // skip 'this'
                val strVal = call.args[p.name]
                if (strVal != null) {
                    argsMap[p] = strVal
                }
                // optional params not in argsMap are skipped automatically by callBy
            }

            val result = if (method.isSuspend) {
                // callSuspend is positional — must include ALL parameters in declaration order.
                // For params not provided by the AI, pass null (works for String?/optional params).
                val orderedArgs = mutableListOf<Any?>()
                for (p in method.parameters) {
                    when {
                        p == method.instanceParameter -> orderedArgs.add(tool)
                        argsMap.containsKey(p) -> orderedArgs.add(argsMap[p])
                        else -> orderedArgs.add(null) // null for all missing params
                    }
                }
                method.callSuspend(*orderedArgs.toTypedArray())
            } else {
                method.callBy(argsMap)
            }
            result?.toString() ?: "null"
        } catch (e: Exception) {
            "❌ Error in ${call.methodName}: [${e::class.simpleName}] ${e.message ?: e.toString()}"
        }
    }
}
