package com.family.tree.core.gedcom

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.model.*

/**
 * Manual test utilities for GEDCOM import/export.
 * This file can be used to verify functionality manually.
 */
object GedcomTest {
    
    /**
     * Creates a simple test ProjectData with sample individuals and families.
     */
    fun createTestData(): ProjectData {
        val ind1 = Individual(
            id = IndividualId("I1"),
            firstName = "John",
            lastName = "Doe",
            gender = Gender.MALE,
            birthYear = 1950,
            deathYear = 2020,
            events = listOf(
                GedcomEvent(
                    type = "BIRT",
                    date = "1 JAN 1950",
                    place = "New York, USA"
                ),
                GedcomEvent(
                    type = "DEAT",
                    date = "15 MAR 2020",
                    place = "Los Angeles, USA"
                )
            ),
            notes = listOf(
                Note(
                    id = NoteId("N1"),
                    text = "This is a test note for John Doe."
                )
            ),
            tags = listOf(
                Tag(
                    id = TagId("T1"),
                    name = "Founder"
                )
            )
        )
        
        val ind2 = Individual(
            id = IndividualId("I2"),
            firstName = "Jane",
            lastName = "Doe",
            gender = Gender.FEMALE,
            birthYear = 1952,
            events = listOf(
                GedcomEvent(
                    type = "BIRT",
                    date = "10 FEB 1952",
                    place = "Boston, USA"
                )
            )
        )
        
        val ind3 = Individual(
            id = IndividualId("I3"),
            firstName = "Bob",
            lastName = "Doe",
            gender = Gender.MALE,
            birthYear = 1975,
            events = listOf(
                GedcomEvent(
                    type = "BIRT",
                    date = "5 MAY 1975",
                    place = "Chicago, USA"
                )
            )
        )
        
        val fam1 = Family(
            id = FamilyId("F1"),
            husbandId = IndividualId("I1"),
            wifeId = IndividualId("I2"),
            childrenIds = listOf(IndividualId("I3")),
            events = listOf(
                GedcomEvent(
                    type = "MARR",
                    date = "20 JUN 1974",
                    place = "Las Vegas, USA"
                )
            ),
            tags = listOf(
                Tag(
                    id = TagId("T2"),
                    name = "MainFamily"
                )
            )
        )
        
        return ProjectData(
            individuals = listOf(ind1, ind2, ind3),
            families = listOf(fam1),
            metadata = ProjectMetadata(name = "Test Family Tree")
        )
    }
    
    /**
     * Tests export by creating test data and exporting to GEDCOM string.
     */
    fun testExport(): String? {
        val data = createTestData()
        return GedcomIO.exportToString(data)
    }
    
    /**
     * Tests import by parsing a GEDCOM string.
     */
    fun testImport(gedcomContent: String): ProjectData? {
        return GedcomIO.importFromString(gedcomContent)
    }
    
    /**
     * Tests round-trip: export then import and compare counts.
     */
    fun testRoundTrip(): Boolean {
        val original = createTestData()
        
        // Export
        val exported = GedcomIO.exportToString(original)
        if (exported == null) {
            println("Export failed")
            return false
        }
        
        println("Exported GEDCOM:")
        println(exported)
        println("\n---\n")
        
        // Import
        val imported = GedcomIO.importFromString(exported)
        if (imported == null) {
            println("Import failed")
            return false
        }
        
        println("Import results:")
        println("Individuals: ${imported.individuals.size} (expected: ${original.individuals.size})")
        println("Families: ${imported.families.size} (expected: ${original.families.size})")
        
        for (ind in imported.individuals) {
            println("  Individual: ${ind.firstName} ${ind.lastName}, " +
                    "gender=${ind.gender}, birth=${ind.birthYear}, death=${ind.deathYear}")
            println("    Events: ${ind.events.size}, Notes: ${ind.notes.size}, Tags: ${ind.tags.size}")
        }
        
        for (fam in imported.families) {
            println("  Family: husband=${fam.husbandId?.value}, wife=${fam.wifeId?.value}, " +
                    "children=${fam.childrenIds.size}")
        }
        
        val success = imported.individuals.size == original.individuals.size &&
                      imported.families.size == original.families.size
        
        if (success) {
            println("\n✓ Round-trip test PASSED")
        } else {
            println("\n✗ Round-trip test FAILED")
        }
        
        return success
    }
}
