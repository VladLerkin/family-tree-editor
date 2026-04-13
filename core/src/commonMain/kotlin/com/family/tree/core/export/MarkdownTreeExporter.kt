package com.family.tree.core.export

import com.family.tree.core.ProjectData
import com.family.tree.core.model.IndividualId

/**
 * Exports the family tree to a Markdown format resembling an ASCII ancestor tree.
 */
class MarkdownTreeExporter {

    fun exportToString(data: ProjectData): String {
        val individualsCount = data.individuals.size
        val familiesCount = data.families.size
        
        // Geographic Profile
        val places = data.individuals.flatMap { it.events.mapNotNull { e -> e.place?.trim() }.filter { it.isNotBlank() } }
        val topPlaces = places.groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .joinToString(", ") { "${it.first} (${it.second} mentions)" }

        val sb = StringBuilder()
        sb.appendLine("## Family Tree Summary")
        sb.appendLine("- Total Individuals: $individualsCount")
        sb.appendLine("- Total Families: $familiesCount")
        if (topPlaces.isNotEmpty()) {
            sb.appendLine("- Geographic Scope: $topPlaces")
            
            val languages = mutableListOf<String>()
            if (places.any { it.contains("Russia", ignoreCase = true) || it.contains("Россия", ignoreCase = true) || 
                            it.contains("Ukraine", ignoreCase = true) || it.contains("Belarus", ignoreCase = true) }) {
                languages.add("Russian/Ukrainian (Cyrillic)")
            }
            if (places.any { it.contains("Germany", ignoreCase = true) || it.contains("Austria", ignoreCase = true) }) {
                languages.add("German")
            }
            if (languages.isNotEmpty()) {
                sb.appendLine("- Likely Search Languages: ${languages.joinToString(", ")}")
            }
        }
        sb.appendLine()
        sb.appendLine("## Ancestry View")
        sb.appendLine()

        // Find roots: individuals who do not have any children in the database
        // An individual has children if they are the husband or wife in a family that has childrenIds
        val parentIds = mutableSetOf<IndividualId>()
        for (fam in data.families) {
            if (fam.childrenIds.isNotEmpty()) {
                if (fam.husbandId != null) parentIds.add(fam.husbandId)
                if (fam.wifeId != null) parentIds.add(fam.wifeId)
            }
        }

        val roots = data.individuals.filter { it.id !in parentIds }

        if (roots.isEmpty() && data.individuals.isNotEmpty()) {
            // Fallback: if everyone is a parent (circular or some other weird edge case), just print the first one
            buildTree(data.individuals.first().id, "", "", data, sb)
        } else if (roots.isEmpty()) {
            sb.appendLine("Empty tree")
        } else {
            for (root in roots) {
                buildTree(root.id, "", "", data, sb)
                sb.appendLine()
            }
        }

        return sb.toString().trimEnd() + "\n"
    }

    private fun buildTree(
        personId: IndividualId?,
        prefix: String,
        childPrefix: String,
        data: ProjectData,
        sb: StringBuilder
    ) {
        if (personId == null) return
        val person = data.individuals.find { it.id == personId } ?: return

        // Extract basic details
        val birthString = person.events.find { it.type == "BIRT" }?.date?.takeIf { it.isNotBlank() }
            ?: person.birthYear?.toString()
        val deathString = person.events.find { it.type == "DEAT" }?.date?.takeIf { it.isNotBlank() }
            ?: person.deathYear?.toString()
        // Try to get a place from birth or death
        val place = person.events.find { it.type == "BIRT" }?.place?.takeIf { it.isNotBlank() }
            ?: person.events.find { it.type == "DEAT" }?.place?.takeIf { it.isNotBlank() }

        val details = mutableListOf<String>()
        if (birthString != null) details.add("b. $birthString")
        if (deathString != null) details.add("d. $deathString")
        if (place != null) details.add(place)

        val detailStr = if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
        sb.appendLine(prefix + person.displayName + detailStr)

        // Find parents
        // A person's parents are the husband and wife of the family where this person is a child
        val famC = data.families.find { it.childrenIds.contains(personId) }
        if (famC != null) {
            val hasFather = famC.husbandId != null
            val hasMother = famC.wifeId != null

            if (hasFather) {
                val fatherPrefix = if (hasMother) "├── " else "└── "
                val newChildPrefix = if (hasMother) "│   " else "    "
                buildTree(famC.husbandId, childPrefix + fatherPrefix, childPrefix + newChildPrefix, data, sb)
            }
            if (hasMother) {
                buildTree(famC.wifeId, childPrefix + "└── ", childPrefix + "    ", data, sb)
            }
        }
    }
}
