package com.family.tree.core.sample

import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.layout.NodePos
import com.family.tree.core.layout.ProjectLayout
import com.family.tree.core.model.*

object SampleData {
    fun simpleThreeGen(): Pair<List<Individual>, List<Family>> {
        val gpa = Individual(
            id = IndividualId("I1"),
            firstName = "William",
            lastName = "Smith",
            gender = Gender.MALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "15 MAR 1945", place = "Boston, Massachusetts, USA"),
                GedcomEvent(type = "DEAT", date = "22 NOV 2020", place = "Boston, Massachusetts, USA")
            )
        )
        val gma = Individual(
            id = IndividualId("I2"),
            firstName = "Margaret",
            lastName = "Smith",
            gender = Gender.FEMALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "22 JUN 1948", place = "London, England, UK")
            )
        )
        val dad = Individual(
            id = IndividualId("I3"),
            firstName = "James",
            lastName = "Smith",
            gender = Gender.MALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "10 JAN 1970", place = "New York, New York, USA")
            )
        )
        val mom = Individual(
            id = IndividualId("I4"),
            firstName = "Emily",
            lastName = "Smith",
            gender = Gender.FEMALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "5 MAY 1973", place = "Chicago, Illinois, USA")
            )
        )
        val kid1 = Individual(
            id = IndividualId("I5"),
            firstName = "Michael",
            lastName = "Smith",
            gender = Gender.MALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "12 SEP 2000", place = "Los Angeles, California, USA")
            )
        )
        val kid2 = Individual(
            id = IndividualId("I6"),
            firstName = "Sarah",
            lastName = "Smith",
            gender = Gender.FEMALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "8 DEC 2003", place = "Los Angeles, California, USA")
            )
        )

        val fam1 = Family(
            id = FamilyId("F1"),
            husbandId = gpa.id,
            wifeId = gma.id,
            childrenIds = listOf(dad.id)
        )
        val fam2 = Family(
            id = FamilyId("F2"),
            husbandId = dad.id,
            wifeId = mom.id,
            childrenIds = listOf(kid1.id, kid2.id)
        )
        return Pair(listOf(gpa, gma, dad, mom, kid1, kid2), listOf(fam1, fam2))
    }

    fun simpleThreeGenWithLayout(): LoadedProject {
        val gpa = Individual(
            id = IndividualId("I1"),
            firstName = "William",
            lastName = "Smith",
            gender = Gender.MALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "15 MAR 1945", place = "Boston, Massachusetts, USA"),
                GedcomEvent(type = "DEAT", date = "22 NOV 2020", place = "Boston, Massachusetts, USA")
            )
        )
        val gma = Individual(
            id = IndividualId("I2"),
            firstName = "Margaret",
            lastName = "Smith",
            gender = Gender.FEMALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "22 JUN 1948", place = "London, England, UK")
            )
        )
        val dad = Individual(
            id = IndividualId("I3"),
            firstName = "James",
            lastName = "Smith",
            gender = Gender.MALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "10 JAN 1970", place = "New York, New York, USA")
            )
        )
        val mom = Individual(
            id = IndividualId("I4"),
            firstName = "Emily",
            lastName = "Smith",
            gender = Gender.FEMALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "5 MAY 1973", place = "Chicago, Illinois, USA")
            )
        )
        val kid1 = Individual(
            id = IndividualId("I5"),
            firstName = "Michael",
            lastName = "Smith",
            gender = Gender.MALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "12 SEP 2000", place = "Los Angeles, California, USA")
            )
        )
        val kid2 = Individual(
            id = IndividualId("I6"),
            firstName = "Sarah",
            lastName = "Smith",
            gender = Gender.FEMALE,
            events = listOf(
                GedcomEvent(type = "BIRT", date = "8 DEC 2003", place = "Los Angeles, California, USA")
            )
        )

        val fam1 = Family(
            id = FamilyId("F1"),
            husbandId = gpa.id,
            wifeId = gma.id,
            childrenIds = listOf(dad.id)
        )
        val fam2 = Family(
            id = FamilyId("F2"),
            husbandId = dad.id,
            wifeId = mom.id,
            childrenIds = listOf(kid1.id, kid2.id)
        )

        // Координаты: родители (поколение 0) выше, дети ниже
        // y=0 - дедушка и бабушка (I1, I2)
        // y=140 - папа и мама (I3, I4)
        // y=280 - дети (I5, I6)
        val layout = ProjectLayout(
            nodePositions = mapOf(
                "I1" to NodePos(x = 10.0, y = 10.0),      // Дедушка
                "I2" to NodePos(x = 180.0, y = 10.0),    // Бабушка
                "I3" to NodePos(x = 90.0, y = 140.0),    // Папа
                "I4" to NodePos(x = 270.0, y = 140.0),  // Мама
                "I5" to NodePos(x = 10.0, y = 280.0),    // Сын
                "I6" to NodePos(x = 180.0, y = 280.0)   // Дочь
            )
        )

        val projectData = ProjectData(
            individuals = listOf(gpa, gma, dad, mom, kid1, kid2),
            families = listOf(fam1, fam2)
        )

        return LoadedProject(
            data = projectData,
            layout = layout,
            meta = null
        )
    }
}