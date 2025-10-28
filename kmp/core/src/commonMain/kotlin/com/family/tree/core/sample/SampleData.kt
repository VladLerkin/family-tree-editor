package com.family.tree.core.sample

import com.family.tree.core.model.*

object SampleData {
    fun simpleThreeGen(): Pair<List<Individual>, List<Family>> {
        val gpa = Individual(IndividualId("I1"), firstName = "Ivan", lastName = "Petrov", birthYear = 1948)
        val gma = Individual(IndividualId("I2"), firstName = "Maria", lastName = "Petrova", birthYear = 1950)
        val dad = Individual(IndividualId("I3"), firstName = "Alex", lastName = "Petrov", birthYear = 1975)
        val mom = Individual(IndividualId("I4"), firstName = "Elena", lastName = "Petrova", birthYear = 1978)
        val kid1 = Individual(IndividualId("I5"), firstName = "Nikita", lastName = "Petrov", birthYear = 2005)
        val kid2 = Individual(IndividualId("I6"), firstName = "Anna", lastName = "Petrova", birthYear = 2008)

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
}