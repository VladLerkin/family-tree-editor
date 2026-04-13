# Cross-Reference Audit

Find and fix every date, name, and place discrepancy between your family tree and your source documents.

## Autoresearch Configuration

**Goal**: For every named person in `[VAULT_PATH]/Family_Tree.md`, compare their dates, names, and places against the corresponding person files and transcription notes. For each mismatch, determine which source is correct and fix the wrong file.

**Metric**: Number of discrepancies remaining (mismatches between Family_Tree.md and person/transcription files)

**Direction**: Minimize (lower is better)

**Verify**: `grep -c "DISCREPANCY\|MISMATCH\|CONFLICT" [VAULT_PATH]/cross_reference_audit.md`

**Guard**:
- When sources conflict, use this hierarchy: primary documents (certificates, vital records) > secondary sources (newspapers, published genealogies) > tertiary sources (family trees, oral history, photo captions)
- Do not silently choose one version. Document every discrepancy in the audit file.
- Do not change primary source transcriptions to match the family tree. If they disagree, the family tree is more likely wrong.

**Iterations**: 12

**Protocol**:

1. **Build the master list**: Read `[VAULT_PATH]/Family_Tree.md` completely. For every named person, extract:
   - Full name (all variants)
   - Birth date and place
   - Death date and place
   - Marriage date, place, and spouse
   - Parents' names
   - Any other dated facts

2. **Compare against source files**: For each person, read their person file (if one exists) and any transcription notes that mention them. Compare every fact.

3. **When a mismatch is found**:
   a. Record it in `[VAULT_PATH]/cross_reference_audit.md` with: person name, field (date/name/place), value in Family_Tree.md, value in person file, authoritative source
   b. Determine which source is correct using the hierarchy above
   c. Fix the incorrect file using Edit
   d. Add a `## Data Discrepancies` section to the person file if one does not exist, documenting the conflict and resolution

4. **High-risk areas to check specifically**:
   - Names that appear in multiple forms (patronymics, maiden names, nicknames, Americanized versions)
   - Dates that were corrected during earlier research
   - Places that have been renamed or reorganized (county boundaries change, countries change names)
   - Death dates listed as approximate
   - Maiden names vs married names vs widowed names

5. **After each family line is audited**, update the count of remaining discrepancies.

6. **Final pass**: Re-read Family_Tree.md and compare against the audit file. Every discrepancy should be marked as RESOLVED in the audit file.

## Output

The audit file (`cross_reference_audit.md`) should contain:

```
| Person | Field | Family_Tree Value | Person File Value | Correct Value | Source | Status |
|---|---|---|---|---|---|---|
| [ANCESTOR] | birth_date | 1866 | May 10, 1866 | May 10, 1866 | Baptism certificate | RESOLVED |
```
