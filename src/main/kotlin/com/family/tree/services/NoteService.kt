package com.family.tree.services

import com.family.tree.model.Family
import com.family.tree.model.Individual
import com.family.tree.model.Note
import com.family.tree.storage.ProjectRepository
import com.family.tree.util.DirtyFlag
import java.util.*

class NoteService(private val data: ProjectRepository.ProjectData) {
    init {
        Objects.requireNonNull(data, "data")
    }

    fun addNoteToIndividual(individualId: String, note: Note): Note {
        Objects.requireNonNull(individualId, "individualId")
        Objects.requireNonNull(note, "note")
        val i: Individual = data.individuals.stream()
            .filter { x -> x.id == individualId }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Individual not found: $individualId") }
        i.notes.add(note)
        DirtyFlag.setModified()
        return note
    }

    fun addNoteToFamily(familyId: String, note: Note): Note {
        Objects.requireNonNull(familyId, "familyId")
        Objects.requireNonNull(note, "note")
        val f: Family = data.families.stream()
            .filter { x -> x.id == familyId }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Family not found: $familyId") }
        f.notes.add(note)
        DirtyFlag.setModified()
        return note
    }

    fun removeNoteFromIndividual(individualId: String, noteId: String) {
        Objects.requireNonNull(individualId, "individualId")
        Objects.requireNonNull(noteId, "noteId")
        val i: Individual = data.individuals.stream()
            .filter { x -> x.id == individualId }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Individual not found: $individualId") }
        if (i.notes.removeIf { n -> n.id == noteId }) {
            DirtyFlag.setModified()
        }
    }

    fun removeNoteFromFamily(familyId: String, noteId: String) {
        Objects.requireNonNull(familyId, "familyId")
        Objects.requireNonNull(noteId, "noteId")
        val f: Family = data.families.stream()
            .filter { x -> x.id == familyId }
            .findFirst()
            .orElseThrow { IllegalArgumentException("Family not found: $familyId") }
        if (f.notes.removeIf { n -> n.id == noteId }) {
            DirtyFlag.setModified()
        }
    }
}
