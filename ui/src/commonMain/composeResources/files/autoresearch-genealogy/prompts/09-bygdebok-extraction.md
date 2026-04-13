# Local History Extraction

Extract genealogical data from digitized local history books (bygdeboker, county histories, parish records).

## Autoresearch Configuration

**Goal**: Search digitized local history sources for mentions of your ancestral families. Extract names, dates, relationships, farm/property information, and any biographical details.

**Metric**: Number of new facts (names, dates, relationships) extracted from local history sources

**Direction**: Maximize

**Verify**: Count new entries added to person files, Family_Tree.md, or Research_Log.md during this session.

**Guard**:
- Local histories are secondary sources. Cross-reference any extracted data with primary records before treating it as established.
- When extracting from non-English sources, note the original language text alongside the translation.
- Patronymic and farm name conventions vary by region and era. Do not assume modern surname conventions apply to historical records.

**Iterations**: 12

**Protocol**:

1. **Identify relevant local histories**: Based on your family's geographic origins, search for:
   - Digitized local history books (bygdeboker for Scandinavian families, county histories for American families, Ortsfamilienbucher for German families)
   - National library digital collections (nb.no for Norway, polona.pl for Poland, ANNO for Austria)
   - University and historical society publications
   - FamilySearch wiki pages for the relevant parish/county

2. **Search strategies by region**:

   **Scandinavian (bygdeboker)**:
   - Search the National Library (nb.no) for the parish name + "bygdebok" or "gard og slekt"
   - Search for farm names, not surnames (pre-1900 Scandinavian records use farm names and patronymics)
   - Check the Kvien database (kvien.net) for western Norway farm lineages

   **American (county histories)**:
   - Search for "[COUNTY] [STATE] history" on Google Books, HathiTrust, or Archive.org
   - Look for biographical sections (often at the back of 19th century county histories)
   - Check USGenWeb for the relevant county

   **Central European (Ortsfamilienbucher, parish records)**:
   - Search Matricula Online (data.matricula-online.eu) for Catholic parish records
   - Search GenTeam.at for indexed Austrian records
   - Search Archion.de for Protestant German records

3. **Extract data**: For each relevant passage found:
   a. Record the exact source (book title, author, year, page number or URL)
   b. Transcribe the relevant text
   c. Extract structured facts (names, dates, places, occupations, relationships)
   d. Note the language and any translation uncertainties

4. **Cross-reference**: Compare extracted data against existing vault files. Flag confirmations and contradictions.

5. **Update the vault**: Add new facts to person files, create new person files for newly discovered ancestors, update Family_Tree.md, log all searches in Research_Log.md.

## Naming Convention Notes

### Scandinavian Patronymics
- Before ~1900, most Scandinavians did not use fixed surnames
- "Rasmusen" means "son of Rasmus," not a fixed family name
- The same person may appear as "[Name] [Patronymic]" in one record and "[Name] [Farm Name]" in another
- Example: "Paul Zacharias Antonsen Lillehaug" = Paul, middle name Zacharias, son of Anton, from the Lillehaug farm

### Farm Names
- Farm names function as location identifiers, not family names
- A family moving to a new farm would take the new farm's name
- Sub-farms (husmannsplasser) may not appear in standard farm name indexes
- The same farm name may exist in multiple parishes

### Americanization
- Immigrants often simplified or anglicized their names upon arrival
- Patronymics became fixed surnames (Johannesen → Johnson)
- Farm names were sometimes kept, sometimes dropped
- Spelling varied widely (Lillehaug / Lillehaugen / Lillehauge)
