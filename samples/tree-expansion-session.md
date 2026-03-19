# Example: Tree Expansion Session

How the tree expansion prompt discovers new ancestors over 8 iterations.

## Starting State

The researcher has a Family_Tree.md with 15 named individuals across 3 generations. Most entries have approximate dates and limited source citations. The tree looks like:

```
[RESEARCHER]
├── [PARENT-1]
│   ├── [GRANDPARENT-1] (b. ~1935, d. 2010, [CITY-A])
│   │   ├── [GREAT-GP-1] (b. ~1905, d. 1978) — only name known
│   │   └── [GREAT-GP-2] (b. ~1910, d. 1995) — maiden name unknown
│   └── [GRANDPARENT-2] (b. 1938, [CITY-B])
└── [PARENT-2]
    ├── [GRANDPARENT-3] (b. ~1930, d. 2005)
    └── [GRANDPARENT-4] (b. ~1932, d. 2018)
```

## Iteration 1: Find a Grave Sweep

The prompt begins by searching Find a Grave for each deceased ancestor.

**[GRANDPARENT-1]**: Found. Memorial reveals exact dates (March 15, 1933 to January 2, 2010), cemetery location, and spouse's name. Also lists parents: [GREAT-GP-1] (1904 to 1978) and [GREAT-GP-2, nee MAIDEN-NAME] (1908 to 1995).

**Result**: Maiden name for [GREAT-GP-2] discovered. +2 confirmed dates, +1 new fact (maiden name).

## Iteration 2: Extending the [SURNAME-1] Line

With [GREAT-GP-1]'s confirmed dates, search for his parents.

**Search**: `"Find a Grave" "[GREAT-GP-1]" 1978 [STATE]`
**Result**: Memorial found. Parents listed: [2G-GP-1] (1875 to 1940, born in [COUNTRY-A]) and [2G-GP-2] (1880 to 1955).

**Search**: `site:geni.com "[2G-GP-1]" [COUNTRY-A]`
**Result**: Geni profile exists. Lists 6 children, emigration year (1901), and marriage date.

**Evaluation**: Geni data is Tier 3. Find a Grave corroborates the parents' names. Two sources agree: PASS.

**Result**: +2 new individuals (great-great-grandparents), +6 siblings identified. Tree now at 23 individuals.

## Iteration 3: Immigration Records

**Search**: `"[2G-GP-1]" passenger manifest 1901`
**Result**: No direct hit. Tried variant spellings. Found a possible match with phonetic spelling of the surname.

**Evaluation**: Name matches phonetically, age matches, destination matches. Country of origin matches. Accepted with note: "(manifest spelling: [VARIANT])."

**Result**: +1 source citation, departure port and ship name recorded. Approximate arrival date confirmed.

## Iterations 4 through 8: Pattern Continues

Each iteration targets the shallowest branches:
- Iteration 4: Extended the [SURNAME-2] line by 2 generations through a published county history found on Google Books
- Iteration 5: Dead end on [GREAT-GP-2]'s parents. Name too common, no death record found online. Logged as negative result.
- Iteration 6: Found [GRANDPARENT-3]'s siblings through obituary, leading to parents' names
- Iteration 7: Extended [SURNAME-3] line to colonial era through DAR Patriot Index
- Iteration 8: Final pass: filled in missing marriage dates from census cross-referencing

## Final State

- **Starting individuals**: 15
- **Ending individuals**: 42
- **New generations added**: 3 (tree now spans 6 generations on one line)
- **New person files created**: 8 (for ancestors with sufficient detail)
- **Negative results logged**: 4 (searches that found nothing)
- **Open questions identified**: 3 (added to Open_Questions.md)
