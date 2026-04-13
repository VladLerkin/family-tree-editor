# Timeline Gap Analysis

Identify gaps in your family timeline where records should exist but have not been found.

## Autoresearch Configuration

**Goal**: For every person in Family_Tree.md with known birth and death dates, identify time periods where records should exist but are missing. Search for those records.

**Metric**: Number of "expected record gaps" remaining (life events where a record probably exists but has not been found)

**Direction**: Minimize (lower is better)

**Verify**: Count entries in `[VAULT_PATH]/timeline_gaps.md` with status OPEN.

**Guard**:
- Not every gap has a findable record. Some records were destroyed, never created, or are not yet digitized.
- Do not fill gaps with speculative data. Only add records actually found.

**Iterations**: 8

**Protocol**:

1. **Build expected events**: For every person in the tree, generate a list of life events that should have created records:
   - Birth (birth certificate, baptism record)
   - Marriage (marriage certificate, church record)
   - Census appearances (every 10 years they were alive during census years)
   - Military service (if applicable based on age and era)
   - Immigration/naturalization (if immigrant)
   - Death (death certificate, obituary, burial record)
   - Property ownership (deeds, tax records)

2. **Compare against what exists**: For each expected event, check whether a corresponding record exists in the vault (person file, transcription, or citation).

3. **Identify gaps**: Create `[VAULT_PATH]/timeline_gaps.md` listing:
   - Person, expected event, expected date range, expected jurisdiction, status (OPEN/FOUND/NOT_APPLICABLE)

4. **Prioritize gaps**: Rank by:
   - Records most likely to exist and be findable (census records, vital records in well-indexed jurisdictions)
   - Records that would resolve open questions
   - Records for ancestors with the fewest existing sources

5. **Search for missing records**: For each high-priority gap, run targeted web searches:
   - Census: `[ANCESTOR NAME] [CENSUS YEAR] census [STATE/COUNTY]`
   - Vital records: `[ANCESTOR NAME] [EVENT] [YEAR] [JURISDICTION]`
   - Immigration: `[ANCESTOR NAME] passenger [YEAR RANGE] [PORT]`

6. **Update**: When a record is found, update the person file, timeline, and gap tracker. When a search yields nothing, log the negative result.
