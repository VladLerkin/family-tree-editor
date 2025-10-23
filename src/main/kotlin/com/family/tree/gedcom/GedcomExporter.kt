package com.family.tree.gedcom

import com.family.tree.model.*
import com.family.tree.storage.ProjectRepository
import java.io.BufferedWriter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Kotlin port of GedcomExporter (1:1 with Java implementation).
 */
class GedcomExporter {
    @Throws(IOException::class)
    fun exportToFile(data: ProjectRepository.ProjectData?, path: Path?) {
        requireNotNull(data) { "data is null" }
        requireNotNull(path) { "path is null" }

        val inds: List<Individual> = data.individuals
        val fams: List<Family> = data.families
        val sources: List<Source> = data.sources
        val repos: List<Repository> = data.repositories
        val submitters: List<Submitter> = data.submitters

        val indXref = GedcomMapper.buildIndividualXrefs(inds)
        val famXref = GedcomMapper.buildFamilyXrefs(fams)

        // Collect all notes and media from individuals and families
        val allNotes = LinkedHashMap<String, Note>()
        val allMedia = LinkedHashMap<String, MediaAttachment>()

        for (ind in inds) {
            for (note in ind.notes) {
                val id = note.id
                if (id != null) allNotes[id] = note
            }
            for (media in ind.media) {
                val id = media.id
                if (id != null) allMedia[id] = media
            }
            for (event in ind.events) {
                for (note in event.notes) {
                    val id = note.id
                    if (id != null) allNotes[id] = note
                }
            }
        }
        for (fam in fams) {
            for (note in fam.notes) {
                val id = note.id
                if (id != null) allNotes[id] = note
            }
            for (media in fam.media) {
                val id = media.id
                if (id != null) allMedia[id] = media
            }
            for (event in fam.events) {
                for (note in event.notes) {
                    val id = note.id
                    if (id != null) allNotes[id] = note
                }
            }
        }

        // Build xrefs for notes, media, sources, repositories, submitters
        val noteXref = LinkedHashMap<String, String>()
        var noteIdx = 1
        for (noteId in allNotes.keys) {
            noteXref[noteId] = "@N${noteIdx++}@"
        }

        val mediaXref = LinkedHashMap<String, String>()
        var mediaIdx = 1
        for (mediaId in allMedia.keys) {
            mediaXref[mediaId] = "@M${mediaIdx++}@"
        }

        val sourceXref = LinkedHashMap<String, String>()
        var sourceIdx = 1
        for (src in sources) {
            sourceXref[src.id] = "@S${sourceIdx++}@"
        }

        val repoXref = LinkedHashMap<String, String>()
        var repoIdx = 1
        for (repo in repos) {
            repoXref[repo.id] = "@R${repoIdx++}@"
        }

        val submXref = LinkedHashMap<String, String>()
        var submIdx = 1
        for (subm in submitters) {
            submXref[subm.id] = "@U${submIdx++}@"
        }

        // Ensure parent directory exists
        val dir = path.toAbsolutePath().parent
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir)

        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { w ->
            // Header (GEDCOM 5.5.5 structure)
            writeln(w, "0 HEAD")
            writeln(w, "1 GEDC")
            writeln(w, "2 VERS 5.5.5")
            writeln(w, "2 FORM LINEAGE-LINKED")
            writeln(w, "3 VERS 5.5.5")
            writeln(w, "1 CHAR UTF-8")
            writeln(w, "1 SOUR family.treeChartEditor")
            writeln(w, "2 NAME family.tree Chart Editor")
            writeln(w, "2 VERS 1.0")
            writeln(w, "1 DATE ${formatHeaderDate(LocalDateTime.now())}")
            writeln(w, "2 TIME ${formatHeaderTime(LocalDateTime.now())}")

            // Add first submitter reference if available
            if (submitters.isNotEmpty()) {
                val firstSubmXref = submXref[submitters[0].id]
                if (firstSubmXref != null) {
                    writeln(w, "1 SUBM $firstSubmXref")
                }
            }

            // Submitters
            for (subm in submitters) {
                val sx = submXref[subm.id]
                if (sx != null) {
                    writeln(w, "0 $sx SUBM")
                    if (!subm.name.isNullOrBlank()) {
                        writeln(w, "1 NAME ${escapeValue(subm.name!!)}")
                    }
                    if (subm.address != null) {
                        writeAddress(w, 1, subm.address!!)
                    }
                    if (!subm.phone.isNullOrBlank()) {
                        writeln(w, "1 PHON ${escapeValue(subm.phone!!)}")
                    }
                    if (!subm.email.isNullOrBlank()) {
                        writeln(w, "1 EMAIL ${escapeValue(subm.email!!)}")
                    }
                }
            }

            // Individuals
            for (ind in inds) {
                val xref = indXref[ind.id]
                writeln(w, "0 $xref INDI")
                writeln(w, "1 NAME ${GedcomMapper.buildName(ind)}")

                val surname = ind.lastName
                val givenName = ind.firstName
                if (!surname.isNullOrBlank()) {
                    writeln(w, "2 SURN ${escapeValue(surname!!)}")
                }
                if (!givenName.isNullOrBlank()) {
                    writeln(w, "2 GIVN ${escapeValue(givenName!!)}")
                }

                writeln(w, "1 SEX ${GedcomMapper.sexCode(ind.gender)}")

                // Export events
                for (event in ind.events) {
                    if (event.type != null) {
                        writeln(w, "1 ${event.type}")
                        if (!event.date.isNullOrBlank()) {
                            writeln(w, "2 DATE ${event.date}")
                        }
                        if (!event.place.isNullOrBlank()) {
                            writeln(w, "2 PLAC ${event.place}")
                        }

                        // Export source citations
                        for (sc in event.sources) {
                            val srcXref = sourceXref[sc.sourceId]
                            if (srcXref != null) {
                                writeln(w, "2 SOUR $srcXref")
                                if (!sc.page.isNullOrBlank()) {
                                    writeln(w, "3 PAGE ${escapeValue(sc.page!!)}")
                                }
                            }
                        }

                        // Export event attributes
                        for (attr in event.attributes) {
                            if (attr.tag != null && attr.value != null) {
                                writeln(w, "2 ${attr.tag} ${escapeValue(attr.value!!)}")
                            }
                        }
                    }
                }

                // Family child/spouse links
                for (f in fams) {
                    if (f.childrenIds.contains(ind.id)) {
                        val fx = famXref[f.id]
                        if (fx != null) writeln(w, "1 FAMC $fx")
                    }
                    if ((f.husbandId != null && f.husbandId == ind.id) || (f.wifeId != null && f.wifeId == ind.id)) {
                        val fx = famXref[f.id]
                        if (fx != null) writeln(w, "1 FAMS $fx")
                    }
                }

                // Notes
                for (note in ind.notes) {
                    val nx = noteXref[note.id]
                    if (nx != null) writeln(w, "1 NOTE $nx")
                }

                // Media
                for (media in ind.media) {
                    val mx = mediaXref[media.id]
                    if (mx != null) writeln(w, "1 OBJE $mx")
                }

                // Tags as custom fields
                for (tag in ind.tags) {
                    if (!tag.name.isNullOrBlank()) {
                        writeln(w, "1 _TAG ${escapeValue(tag.name!!)}")
                    }
                }
            }

            // Families
            for (fam in fams) {
                val xref = famXref[fam.id]
                writeln(w, "0 $xref FAM")

                if (fam.husbandId != null) {
                    val ix = indXref[fam.husbandId]
                    if (ix != null) writeln(w, "1 HUSB $ix")
                }

                if (fam.wifeId != null) {
                    val ix = indXref[fam.wifeId]
                    if (ix != null) writeln(w, "1 WIFE $ix")
                }

                for (cid in fam.childrenIds) {
                    val ix = indXref[cid]
                    if (ix != null) writeln(w, "1 CHIL $ix")
                }

                // Export events
                for (event in fam.events) {
                    if (event.type != null) {
                        writeln(w, "1 ${event.type}")
                        if (!event.date.isNullOrBlank()) {
                            writeln(w, "2 DATE ${event.date}")
                        }
                        if (!event.place.isNullOrBlank()) {
                            writeln(w, "2 PLAC ${event.place}")
                        }

                        // Export source citations
                        for (sc in event.sources) {
                            val srcXref = sourceXref[sc.sourceId]
                            if (srcXref != null) {
                                writeln(w, "2 SOUR $srcXref")
                                if (!sc.page.isNullOrBlank()) {
                                    writeln(w, "3 PAGE ${escapeValue(sc.page!!)}")
                                }
                            }
                        }
                    }
                }

                // Notes
                for (note in fam.notes) {
                    val nx = noteXref[note.id]
                    if (nx != null) writeln(w, "1 NOTE $nx")
                }

                // Media
                for (media in fam.media) {
                    val mx = mediaXref[media.id]
                    if (mx != null) writeln(w, "1 OBJE $mx")
                }

                // Tags as custom fields
                for (tag in fam.tags) {
                    if (!tag.name.isNullOrBlank()) {
                        writeln(w, "1 _TAG ${escapeValue(tag.name!!)}")
                    }
                }
            }

            // Source records
            for (src in sources) {
                val sx = sourceXref[src.id]
                if (sx != null) {
                    writeln(w, "0 $sx SOUR")
                    if (!src.agency.isNullOrBlank()) {
                        writeln(w, "1 DATA")
                        writeln(w, "2 AGNC ${escapeValue(src.agency!!)}")
                    }
                    if (!src.title.isNullOrBlank()) {
                        writeln(w, "1 TITL ${escapeValue(src.title!!)}")
                    }
                    if (!src.abbreviation.isNullOrBlank()) {
                        writeln(w, "1 ABBR ${escapeValue(src.abbreviation!!)}")
                    }
                    if (src.repositoryId != null) {
                        val rx = repoXref[src.repositoryId]
                        if (rx != null) {
                            writeln(w, "1 REPO $rx")
                            if (!src.callNumber.isNullOrBlank()) {
                                writeln(w, "2 CALN ${escapeValue(src.callNumber!!)}")
                            }
                        }
                    }
                }
            }

            // Repository records
            for (repo in repos) {
                val rx = repoXref[repo.id]
                if (rx != null) {
                    writeln(w, "0 $rx REPO")
                    if (!repo.name.isNullOrBlank()) {
                        writeln(w, "1 NAME ${escapeValue(repo.name!!)}")
                    }
                    if (repo.address != null) {
                        writeAddress(w, 1, repo.address!!)
                    }
                    if (!repo.phone.isNullOrBlank()) {
                        writeln(w, "1 PHON ${escapeValue(repo.phone!!)}")
                    }
                    if (!repo.email.isNullOrBlank()) {
                        writeln(w, "1 EMAIL ${escapeValue(repo.email!!)}")
                    }
                    if (!repo.website.isNullOrBlank()) {
                        writeln(w, "1 WWW ${escapeValue(repo.website!!)}")
                    }
                }
            }

            // Note records
            for ((key, note) in allNotes) {
                val nx = noteXref[key]
                if (nx != null) {
                    writeln(w, "0 $nx NOTE")
                    if (!note.text.isNullOrBlank()) {
                        writeMultilineValue(w, 1, note.text!!)
                    }
                    // Export sources for the note
                    if (note.sources != null) {
                        for (sc in note.sources) {
                            val sx = sourceXref[sc.sourceId]
                            if (sx != null) {
                                writeln(w, "1 SOUR $sx")
                                if (!sc.page.isNullOrBlank()) {
                                    writeln(w, "2 PAGE ${escapeValue(sc.page!!)}")
                                }
                            }
                        }
                    }
                }
            }

            // Media (OBJE) records
            for ((key, media) in allMedia) {
                val mx = mediaXref[key]
                if (mx != null) {
                    writeln(w, "0 $mx OBJE")
                    if (!media.fileName.isNullOrBlank()) {
                        writeln(w, "1 FILE ${escapeValue(media.fileName!!)}")
                    }
                    if (!media.relativePath.isNullOrBlank()) {
                        writeln(w, "1 _PATH ${escapeValue(media.relativePath!!)}")
                    }
                }
            }

            // Trailer
            writeln(w, "0 TRLR")
        }
    }

    @Throws(IOException::class)
    private fun writeAddress(w: BufferedWriter, level: Int, addr: Address) {
        writeln(w, "$level ADDR")
        if (!addr.line1.isNullOrBlank()) {
            writeln(w, "${level + 1} ADR1 ${escapeValue(addr.line1!!)}")
        }
        if (!addr.line2.isNullOrBlank()) {
            writeln(w, "${level + 1} ADR2 ${escapeValue(addr.line2!!)}")
        }
        if (!addr.line3.isNullOrBlank()) {
            writeln(w, "${level + 1} ADR3 ${escapeValue(addr.line3!!)}")
        }
        if (!addr.city.isNullOrBlank()) {
            writeln(w, "${level + 1} CITY ${escapeValue(addr.city!!)}")
        }
        if (!addr.state.isNullOrBlank()) {
            writeln(w, "${level + 1} STAE ${escapeValue(addr.state!!)}")
        }
        if (!addr.postalCode.isNullOrBlank()) {
            writeln(w, "${level + 1} POST ${escapeValue(addr.postalCode!!)}")
        }
        if (!addr.country.isNullOrBlank()) {
            writeln(w, "${level + 1} CTRY ${escapeValue(addr.country!!)}")
        }
    }

    private fun formatHeaderDate(dt: LocalDateTime): String {
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        return dt.format(fmt).uppercase(Locale.ROOT)
    }

    private fun formatHeaderTime(dt: LocalDateTime): String {
        val fmt = DateTimeFormatter.ofPattern("HH:mm:ss")
        return dt.format(fmt)
    }

    private fun escapeValue(value: String?): String {
        if (value == null) return ""
        return value.replace("\r", " ").replace("\n", " ")
    }

    @Throws(IOException::class)
    private fun writeMultilineValue(w: BufferedWriter, level: Int, text: String) {
        if (text.isEmpty()) return

        val lines = text.split("\r?\n".toRegex()).toTypedArray()
        var first = true
        for (line in lines) {
            if (line.length > 248) {
                var pos = 0
                var firstChunk = first
                while (pos < line.length) {
                    val end = kotlin.math.min(pos + 248, line.length)
                    val chunk = line.substring(pos, end)
                    if (firstChunk) {
                        writeln(w, "$level CONC $chunk")
                        firstChunk = false
                        first = false
                    } else {
                        writeln(w, "$level CONC $chunk")
                    }
                    pos = end
                }
            } else {
                if (first) {
                    writeln(w, "$level CONC $line")
                    first = false
                } else {
                    writeln(w, "$level CONT $line")
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun writeln(w: BufferedWriter, line: String) {
        w.write(line)
        w.write("\r\n")
    }
}