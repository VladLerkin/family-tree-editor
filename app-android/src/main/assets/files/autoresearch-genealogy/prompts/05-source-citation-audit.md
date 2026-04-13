# Source Citation Audit

Verify that every person file in your vault cites at least two independent sources.

## Autoresearch Configuration

**Goal**: For every person file in `[VAULT_PATH]/`, check that the `sources` frontmatter field lists at least two independent sources. For any person with fewer than two sources, attempt to find additional corroborating sources via web search.

**Metric**: Number of person files with fewer than two independent sources

**Direction**: Minimize (lower is better)

**Verify**: Count person files where the `sources` field has fewer than 2 entries, or where the Document Sources section has fewer than 2 rows.

**Guard**:
- Do not count two entries from the same underlying source as independent (e.g., two Ancestry trees that copied the same data)
- Do not add speculative sources. Only add sources that actually corroborate existing claims.
- If no second source can be found, mark the person's confidence as `low` or `stub` in the frontmatter.

**Iterations**: 8

**Protocol**:

1. **Inventory**: Read every `.md` file in `[VAULT_PATH]/` with `type: person` in the frontmatter. For each, count the number of independent sources listed.

2. **Categorize**:
   - 2+ sources: PASS (no action needed)
   - 1 source: NEEDS_CORROBORATION
   - 0 sources: UNSOURCED

3. **For each NEEDS_CORROBORATION person**:
   a. Read the existing person file to understand what is claimed
   b. Search for corroborating sources: Find a Grave, census records, newspaper archives, church records
   c. If found, add the source to the person file's Document Sources section and update the `sources` frontmatter
   d. If not found, add a note: "Single source only; corroboration not yet found"

4. **For each UNSOURCED person**:
   a. Determine where the person's data originally came from (family tree screenshot? oral history?)
   b. Search for ANY source that confirms the person's existence and basic facts
   c. If nothing is found, flag the person in Open_Questions.md

5. **Create an audit file**: `[VAULT_PATH]/source_citation_audit.md` tracking each person's source count and status.

6. **Update confidence levels**: Adjust `confidence` frontmatter based on source count:
   - 3+ independent sources → `high`
   - 2 independent sources → `moderate`
   - 1 source → `low`
   - 0 sources → `stub`
