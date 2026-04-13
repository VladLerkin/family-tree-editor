# Immigration Search

Search for immigration, naturalization, and passenger records for immigrant ancestors.

## Autoresearch Configuration

**Goal**: For every immigrant ancestor in `[VAULT_PATH]/Family_Tree.md`, locate their immigration record (passenger manifest), naturalization record, or both.

**Metric**: Number of immigrant ancestors without an immigration or naturalization record

**Direction**: Minimize (lower is better)

**Verify**: Count immigrant ancestors in Family_Tree.md who lack citation to a passenger list or naturalization document.

**Guard**:
- Name spelling on passenger manifests can be extremely different from what the family used in America. Search all plausible variants.
- Not all immigrants were naturalized. Some remained resident aliens their entire lives.
- Pre-1820 US passenger lists are sparse to nonexistent.

**Iterations**: 10

**Protocol**:

1. **Identify immigrant ancestors**: From Family_Tree.md, list every person born outside the US (or with "emigrated" / "immigrated" notation). For each, note:
   - Name (all known variants, including original language spelling)
   - Approximate arrival year or range
   - Country of origin (as specific as possible: country, region, city/village)
   - Port of entry (if known)
   - Final destination in the US

2. **Search passenger manifests**:
   a. `site:ellisisland.org "[ANCESTOR NAME]"` (covers 1892 to 1957 for New York)
   b. `"[ANCESTOR NAME]" passenger manifest [YEAR RANGE]`
   c. FamilySearch: search the relevant port's passenger list collection
   d. Try name variants: original spelling, phonetic spellings, abbreviated names, maiden name (for women)
   e. If name searches fail, try searching by ship name (if known from family records) + approximate date

3. **Search naturalization records**:
   a. FamilySearch: "[STATE] naturalization" collections
   b. `"[ANCESTOR NAME]" naturalization [CITY/COUNTY] [YEAR RANGE]`
   c. County clerk offices (many post-1906 records are indexed online)
   d. Post-1906 naturalization records are the richest: they contain exact birthplace, birth date, arrival date, ship name, occupation, physical description

4. **Data extraction**: When a record is found:
   - Passenger manifest fields: full name, age, occupation, last residence, destination, ship name, departure port, arrival port, arrival date, traveling companions, literacy, physical description, amount of money
   - Naturalization fields: full name, birth date, birthplace (town and country), arrival date, port, ship name, occupation, current address, witnesses

5. **Cross-reference**: Compare extracted data against existing vault files. Does the birth date match? Does the birthplace narrow down a region? Are traveling companions family members?

6. **Update vault**: Create transcription notes for found records, update person files, update Family_Tree.md with new data, log searches in Research_Log.md.

## Key Immigration Databases

| Database | Coverage | Access |
|---|---|---|
| Ellis Island (libertyellisfoundation.org) | New York arrivals, 1892 to 1957 | Free |
| Castle Garden (castlegarden.org) | New York arrivals, 1820 to 1892 | Free |
| FamilySearch | Multiple ports, multiple countries | Free |
| Ancestry.com | Largest indexed collection | Subscription |
| Emigration records by country | Varies (Digitalarkivet for Norway, Emihamn for Sweden, etc.) | Varies |
| USCIS Genealogy Program | Naturalization certificates, 1906 to 1956 | $65 per request |

## Name Variation Strategies

When searching for an immigrant ancestor, try:
- Original language spelling (e.g., Lillehauge, not Lillehaug)
- Phonetic variants (Kowalski / Kowalsky / Kovalski)
- Shortened forms (Chas. for Charles, Wm. for William)
- Maiden name for married women
- Patronymic (for Scandinavians: Antonsen, Rasmusen)
- Farm name or place name (for Scandinavians)
- Both "old country" and Americanized versions
