# Tree Expansion

Push every branch of your family tree as far back as possible using web research.

## Autoresearch Configuration

**Goal**: For every ancestor currently in `[VAULT_PATH]/Family_Tree.md`, search for their parents, siblings, spouses' families, and any additional generations. Update the vault after each discovery. Keep iterating until no more ancestors can be found through free web sources.

**Metric**: Number of new ancestors/individuals added to Family_Tree.md

**Direction**: Maximize

**Verify**: Count the number of named individuals in Family_Tree.md before and after each iteration. Log the delta.

**Guard**:
- Do not fabricate ancestors. Every addition must cite a source.
- Do not trust user-contributed trees (Geni, Ancestry hints) without corroboration from at least one independent source.
- Do not modify existing dates or names during expansion; that is the cross-reference audit's job.
- Mark any unverified additions with `(unverified)` in the tree.

**Iterations**: 8

**Protocol**:

1. **Baseline**: Read `[VAULT_PATH]/Family_Tree.md` completely. Count every named individual. Record this as the baseline in `[VAULT_PATH]/Research_Log.md`.

2. **Identify expansion targets**: For each leaf node (an ancestor with no listed parents), note:
   - Name, dates, and location
   - Which web sources might have their parents (Find a Grave, Geni, WikiTree, FamilySearch wiki, published county histories)

3. **Search strategy per ancestor**: For each target, run web searches in this order:
   a. `"Find a Grave" "[ANCESTOR NAME]" [DEATH YEAR] [BURIAL LOCATION]`
   b. `"[ANCESTOR NAME]" parents born [BIRTH YEAR RANGE] [LOCATION]`
   c. `site:geni.com "[ANCESTOR NAME]" [BIRTH YEAR]`
   d. `site:wikitree.com "[ANCESTOR NAME]"`
   e. `"[ANCESTOR NAME]" genealogy [LOCATION] [SURNAME]`

4. **Evaluate results**: For each search hit:
   - Does the person match on name, dates, AND location? All three must align.
   - Is the source a primary record (vital record, church register) or secondary (user tree, published genealogy)?
   - If secondary, does at least one other independent source corroborate?

5. **Update the vault**: For each confirmed new ancestor:
   - Add to Family_Tree.md in the correct position
   - If sufficient data exists, create a person file using the template at `[VAULT_PATH]/templates/person.md`
   - Note the source in the person file's Document Sources section

6. **Log the search**: In `[VAULT_PATH]/Research_Log.md`, record:
   - Date and search target
   - Queries used
   - Results (positive or negative)
   - New individuals added (if any)
   - What remains unresolved

7. **Update the count**: After each iteration, recount named individuals in Family_Tree.md. Report the delta.

8. **Repeat**: Move to the next set of expansion targets. Prioritize:
   - Lines with the fewest known generations (shallowest branches)
   - Ancestors with specific dates and locations (higher probability of finding records)
   - Lines with no person files yet (lowest current documentation)

## Tips

- **Patronymic names**: In Scandinavian genealogy, "Johnson" means "son of Johan." The same person may appear under different names in different records (farm name, patronymic, Americanized name). Search all variants.
- **Name changes at immigration**: Many immigrants changed or simplified their names. Search both the original and Americanized versions.
- **Negative results matter**: If you search for an ancestor and find nothing, log it. This prevents duplicate searches later.
- **Sibling research**: Sometimes the easiest way to find an ancestor's parents is to find their siblings first. Siblings often appear in more records.
