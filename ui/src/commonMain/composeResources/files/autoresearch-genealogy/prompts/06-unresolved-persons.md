# Unresolved Persons

Identify and attempt to resolve every unnamed or ambiguous person mentioned in your vault.

## Autoresearch Configuration

**Goal**: Find every person mentioned in vault documents (transcriptions, certificates, witness lists, photo captions) who does NOT have a person file or clear identification. Attempt to identify them and determine their relationship to the family.

**Metric**: Number of unresolved persons remaining

**Direction**: Minimize (lower is better)

**Verify**: `grep -c "UNRESOLVED\|UNKNOWN\|UNIDENTIFIED" [VAULT_PATH]/unresolved_persons.md`

**Guard**:
- Do not force identifications. If a person cannot be identified with reasonable confidence, leave them as unresolved.
- Do not create person files for tangentially connected individuals (e.g., a random census neighbor) unless they appear in multiple family documents.

**Iterations**: 10

**Protocol**:

1. **Extract all named individuals**: Read every file in `[VAULT_PATH]/`. For each file, extract every named person mentioned (witnesses, sponsors, godparents, neighbors, co-signers, gift-givers, attendees).

2. **Cross-reference against known persons**: For each extracted name, check whether:
   - A person file exists
   - They appear in Family_Tree.md
   - They are mentioned in other vault documents

3. **Categorize**:
   - **Known**: Has a person file or is in Family_Tree.md → skip
   - **Likely family**: Appears in multiple family documents, shares surname, or holds a family role (witness, sponsor) → prioritize
   - **Community connection**: Appears once in a document (neighbor on census, co-worker) → lower priority
   - **Cannot identify**: Name is too common or context is insufficient → log and move on

4. **For each "Likely family" person**:
   a. Search for them in Find a Grave, census records, and other vault documents
   b. Determine their relationship to the family (sibling? cousin? in-law? family friend?)
   c. If identified, create a person file or add them to Family_Tree.md
   d. If partially identified, log in `[VAULT_PATH]/unresolved_persons.md` with what is known

5. **Create or update the tracking file**: `[VAULT_PATH]/unresolved_persons.md` with columns: Name, Context (where mentioned), Category, Resolution, Notes.

6. **Special attention to**:
   - Wedding witnesses and certificate signers (often close family members)
   - Baptism sponsors/godparents (often aunts, uncles, or close friends)
   - People who appear at multiple family events across different years
   - People with the same surname who are not yet in the tree
