package com.family.tree.core.ai.koog

import com.family.tree.core.ai.koog.KoogAgent.ToolCall
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.callSuspend

/**
 * JVM implementation using reflection to discover and invoke tools.
 */
actual class PlatformToolInvoker actual constructor() {
    actual fun getToolDocumentation(tools: List<Any>): String {
        return tools.joinToString("\n\n") { tool ->
            val className = tool::class.simpleName ?: "Unknown"
            val methods = tool::class.memberFunctions
                .filter { method -> method.annotations.any { it is Tool } }
                .joinToString("\n") { method ->
                    val toolAnnotation = method.annotations.find { it is Tool } as Tool
                    val params = method.parameters.drop(1).joinToString(", ") { "${it.name}: ${it.type}" }
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
            // If class name is not specified, find the first tool that has this method
            tools.find { tool ->
                tool::class.memberFunctions.any { it.name == call.methodName && it.annotations.any { a -> a is Tool } }
            } ?: return "Error: Method ${call.methodName} not found in any available tools."
        }

        return try {
            val method = tool::class.memberFunctions.find { it.name == call.methodName }
                ?: return "Error: Method ${call.methodName} not found in ${call.className}."

            val params = method.parameters.drop(1)
            val argsMap = mutableMapOf<kotlin.reflect.KParameter, Any?>()
            argsMap[method.parameters[0]] = tool // 'this' argument

            for (p in params) {
                if (call.args.containsKey(p.name)) {
                    argsMap[p] = call.args[p.name]
                }
            }

            val result = if (method.isSuspend) {
                method.callSuspend(tool, *argsMap.values.toTypedArray().drop(1).toTypedArray())
            } else {
                method.call(tool, *argsMap.values.toTypedArray().drop(1).toTypedArray())
            }
            result.toString()
        } catch (e: Exception) {
            "Error executing tool: ${e.message}"
        }
    }
}
