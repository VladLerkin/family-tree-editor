# Colonial Records Search

Search for colonial American ancestors in pre-1800 records.

## Autoresearch Configuration

**Goal**: For every ancestor in `[VAULT_PATH]/Family_Tree.md` who lived in colonial America (pre-1800), search for records that would extend the line further back or fill biographical details.

**Metric**: Number of colonial-era ancestors with at least one primary source citation

**Direction**: Maximize

**Verify**: Count persons in Family_Tree.md with dates before 1800 who have entries in their Document Sources section.

**Guard**:
- Colonial records are fragmentary. Many have been lost to fire, war, or neglect.
- Do not confuse similarly named individuals. Colonial naming conventions often reused names across generations within a family.
- Be cautious with compiled genealogies and lineage society applications; they sometimes contain errors that have been perpetuated for decades.

**Iterations**: 10

**Protocol**:

1. **Identify colonial ancestors**: Read Family_Tree.md and list every person with dates before 1800. For each, note:
   - Name, dates, location (colony/county)
   - What records already exist in the vault
   - What records might exist (see Record Types below)

2. **Search by record type**:

   **Land records**: Colonial land patents, grants, and surveys are among the best-preserved colonial records.
   - Search: `[ANCESTOR] land [COUNTY] [COLONY] colonial`
   - Check county deed books (many digitized by FamilySearch)

   **Probate records**: Wills, inventories, and administrations name family members.
   - Search: `[ANCESTOR] will probate [COUNTY] [YEAR RANGE]`
   - Often the best source for identifying a wife, children, and extended family

   **Military records**: Militia rolls, muster lists, oaths of allegiance.
   - Search: `[ANCESTOR] militia [COUNTY] [WAR/ERA]`
   - Revolutionary War records: check the DAR Patriot Index

   **Church records**: Baptism, marriage, and burial registers.
   - Search by parish and denomination
   - Many colonial churches have published their registers

   **Tax records**: Tithable lists, tax assessments.
   - Show who lived in a county and approximately when

   **Court records**: Civil suits, criminal proceedings, guardianships.
   - Often reveal family relationships not found elsewhere

3. **Source evaluation**: Colonial records require extra care:
   - Names are spelled inconsistently (Thomas / Tomas / Thom.)
   - Dates may use Old Style (Julian) or New Style (Gregorian) calendars
   - "Junior" and "Senior" indicate relative age in a community, not necessarily father and son
   - Women are frequently identified only as "wife of [HUSBAND]" or by their first name

4. **Update vault**: For each record found, create a transcription note, update the person file, and log the search.

## Colonial Record Sources

| Source | Coverage | Access |
|---|---|---|
| FamilySearch | All colonies; indexed and image-only collections | Free (account required) |
| Fold3 | Military records, pensions | Subscription |
| Archive.org / Google Books | Published county histories, genealogies | Free |
| State archives | Varies by state | Some digitized, some in-person only |
| DAR Patriot Index | Revolutionary War era | Free at dar.org |
| USGenWeb | County-level transcriptions | Free, volunteer-maintained |
