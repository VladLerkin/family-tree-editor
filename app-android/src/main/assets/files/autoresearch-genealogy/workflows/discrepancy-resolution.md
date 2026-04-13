# Discrepancy Resolution

A framework for resolving conflicts between sources.

## When to Use This Workflow

Use this when two or more sources disagree on a fact (date, name, place, relationship) about the same person.

## Step 1: Document the Discrepancy

In the person file's `## Data Discrepancies` section:

| Source A | Source B | Field | Value A | Value B |
|---|---|---|---|---|
| [Source name and type] | [Source name and type] | [birth_date / name / place] | [value] | [value] |

## Step 2: Classify the Sources

For each conflicting source, determine:
- **Source tier**: Primary (Tier 1), Secondary (Tier 2), or Tertiary (Tier 3)
- **Proximity to event**: Was the record created at the time of the event, or years/decades later?
- **Informant**: Who provided the information? How likely are they to know the correct answer?
- **Record purpose**: Was accuracy important for the record's purpose? (Legal documents vs casual correspondence)

## Step 3: Apply the Hierarchy

**If sources are different tiers**: The higher-tier source is presumed correct.

**If sources are the same tier**: Consider:
1. Which was created closer to the event?
2. Which informant had better knowledge?
3. Is there any third source to break the tie?
4. Is either source internally consistent while the other has other errors?

## Step 4: Resolve or Defer

### If the answer is clear:
- Update the person file with the correct value
- Note the resolution in the Data Discrepancies section
- Mark as RESOLVED with the reasoning

### If the answer is unclear:
- Keep both values in the person file
- Add to Open_Questions.md with the search strategy that might resolve it
- Mark as UNRESOLVED
- Set confidence to `low` for the disputed field

## Step 5: Propagate the Resolution

If the correct value differs from what is in Family_Tree.md or other files:
- Update Family_Tree.md
- Update Timeline.md
- Update any affected person files
- Log the correction in Research_Log.md

## Common Resolution Patterns

| Scenario | Typical Resolution |
|---|---|
| Birth certificate vs family tree screenshot | Birth certificate wins (Tier 1 vs Tier 3) |
| Obituary date vs gravestone date | Need a third source; both are Tier 2 |
| Census age vs birth certificate | Birth certificate wins; census ages are often rounded |
| Family oral history vs vital record | Vital record wins, but note the oral history version |
| Two primary sources disagree | Investigate which informant had better knowledge |
| Name spelling varies across records | Adopt the most formal/official spelling; note all variants |
