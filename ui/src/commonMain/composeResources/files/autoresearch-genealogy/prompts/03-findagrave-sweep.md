# Find a Grave Sweep

Locate a Find a Grave memorial for every deceased person in your family tree.

## Autoresearch Configuration

**Goal**: For every deceased person in `[VAULT_PATH]/Family_Tree.md`, search Find a Grave for their memorial. Extract all available data and update the vault.

**Metric**: Number of deceased persons WITHOUT a Find a Grave memorial link

**Direction**: Minimize (lower is better)

**Verify**: `grep -c "NO_MEMORIAL_FOUND\|NEEDS_FINDAGRAVE" [VAULT_PATH]/findagrave_audit.md`

**Guard**:
- Do not assume a Find a Grave result is the correct person without verifying name, dates, AND location.
- If multiple memorials match, document all candidates and flag for human review.
- Do not create Find a Grave memorials; only search for existing ones.

**Iterations**: 15

**Protocol**:

1. **Build the deceased list**: Read `[VAULT_PATH]/Family_Tree.md`. List every person with a death date or "deceased" notation. For each, note:
   - Full name (all known variants)
   - Birth and death dates (even approximate)
   - Expected burial location (city, state, or cemetery if known)
   - Whether a Find a Grave memorial is already linked in the vault

2. **Create the audit file**: `[VAULT_PATH]/findagrave_audit.md` with columns:
   - Person name, dates, expected burial location, Find a Grave status, memorial number, new data extracted

3. **Search priority order**:
   - Most recent deaths first (more likely to have memorials)
   - Then work backward chronologically
   - Skip anyone already linked to a memorial (add to audit as KNOWN)

4. **Search strategy per person**:
   a. `"Find a Grave" "[FULL NAME]" [DEATH YEAR]`
   b. `site:findagrave.com "[FULL NAME]" [LOCATION]`
   c. Try name variations: maiden name, married name, nickname, abbreviated first name
   d. If no results, try searching by cemetery name + surname only

5. **When a memorial IS found**:
   - Verify: do name, dates, and location all match?
   - Extract: full name as listed, birth/death dates, burial location (cemetery name and city/state), spouse(s), parents, children, linked records (Ancestry, FamilySearch)
   - Compare against vault data. Note any discrepancies.
   - Update the person file with the memorial link and any new data
   - Update Family_Tree.md if the memorial reveals new relationships

6. **When a memorial is NOT found**:
   - Log as NO_MEMORIAL_FOUND with all search terms tried
   - Note whether the cemetery itself has coverage on Find a Grave (some cemeteries are not yet transcribed)

7. **After each batch of 3 to 4 searches**, update the audit file with current counts.

## Find a Grave Data Value

Find a Grave memorials often contain data not available elsewhere:
- Exact cemetery and plot location
- Photos of headstones (which may show dates not in other records)
- Family links (spouse, parents, children buried nearby)
- Linked Ancestry and FamilySearch records
- User-contributed biographical information (treat as secondary source)
