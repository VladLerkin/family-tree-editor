# GEDCOM Completeness

Ensure every person in your family tree exists in your GEDCOM file with all known data.

## Autoresearch Configuration

**Goal**: Every named person in `[VAULT_PATH]/Family_Tree.md` should exist in `[GEDCOM_PATH]` with correct names, dates, places, and relationships.

**Metric**: Number of persons in Family_Tree.md who are missing from or incomplete in the GEDCOM

**Direction**: Minimize (lower is better)

**Verify**: `grep -c "MISSING\|INCOMPLETE" [VAULT_PATH]/gedcom_audit.md`

**Guard**:
- Use GEDCOM 5.5.1 format (the most widely supported version)
- Do not include living persons' full birth dates in the GEDCOM (privacy). Use birth year only or mark as "Living."
- Preserve existing GEDCOM data; do not overwrite entries that already exist unless they are demonstrably wrong

**Iterations**: 10

**Protocol**:

1. **Build the master list**: Read `[VAULT_PATH]/Family_Tree.md`. For every named person, extract:
   - Full name
   - Birth date and place
   - Death date and place
   - Marriage(s) with date, place, and spouse
   - Parents
   - Children

2. **Read the GEDCOM** (or create one if it does not exist): Parse `[GEDCOM_PATH]` and build a parallel list of all INDI (individual) and FAM (family) records.

3. **Compare**: For each person in the master list:
   - Does an INDI record exist? If not, mark as MISSING.
   - Are all known facts present? If not, mark as INCOMPLETE and list missing fields.
   - Are relationships correct? Check FAM records for spouse links, FAMC/FAMS references.

4. **Create the audit file**: `[VAULT_PATH]/gedcom_audit.md` with:
   ```
   | Person | Status | Missing Fields | Notes |
   |---|---|---|---|
   | [ANCESTOR] | MISSING | all | Not in GEDCOM |
   | [ANCESTOR-2] | INCOMPLETE | death_date, burial | Has INDI but missing data |
   ```

5. **Fix missing persons**: For each MISSING person, add a new INDI record:
   ```
   0 @I[N]@ INDI
   1 NAME [Given] /[Surname]/
   1 BIRT
   2 DATE [DD MMM YYYY]
   2 PLAC [Place]
   1 DEAT
   2 DATE [DD MMM YYYY]
   2 PLAC [Place]
   ```

6. **Fix incomplete persons**: For each INCOMPLETE person, add the missing fields to the existing INDI record.

7. **Fix relationships**: Ensure every marriage has a FAM record linking husband, wife, and children:
   ```
   0 @F[N]@ FAM
   1 HUSB @I[husband]@
   1 WIFE @I[wife]@
   1 CHIL @I[child1]@
   1 MARR
   2 DATE [DD MMM YYYY]
   2 PLAC [Place]
   ```

8. **Validate**: After all changes, verify:
   - Every INDI has at least a NAME
   - Every FAM has at least HUSB or WIFE
   - No orphaned CHIL references (child ID exists as INDI)
   - No duplicate INDI records for the same person
   - The file ends with `0 TRLR`

## GEDCOM 5.5.1 Quick Reference

```
0 HEAD
1 SOUR autoresearch-genealogy
1 GEDC
2 VERS 5.5.1
2 FORM LINEAGE-LINKED
1 CHAR UTF-8
0 @I1@ INDI
1 NAME Given /Surname/
1 SEX M
1 BIRT
2 DATE 10 MAY 1866
2 PLAC Renville County, Minnesota, USA
1 DEAT
2 DATE 12 DEC 1950
2 PLAC Clinton, Minnesota, USA
1 FAMS @F1@
0 @F1@ FAM
1 HUSB @I1@
1 WIFE @I2@
1 CHIL @I3@
1 MARR
2 DATE 15 JUN 1911
2 PLAC Clinton, Minnesota, USA
0 TRLR
```
