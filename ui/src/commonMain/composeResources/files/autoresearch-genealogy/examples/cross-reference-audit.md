# Example: Cross-Reference Audit

How the audit prompt finds and resolves discrepancies.

## Starting State

After the tree expansion, the vault contains 42 individuals in Family_Tree.md and 8 person files. Some data was added from multiple sources that may disagree.

## Discrepancies Found

### Discrepancy 1: Birth Year
- **Person**: [GREAT-GP-1]
- **Family_Tree.md**: "b. ~1905"
- **Person file**: "b. 1904" (from Find a Grave)
- **Resolution**: Find a Grave memorial (Tier 2) is more specific than the approximate "~1905" (Tier 3). Updated Family_Tree.md to "b. 1904." Note: a birth certificate (Tier 1) would be definitive if found.

### Discrepancy 2: Maiden Name
- **Person**: [GREAT-GP-2]
- **Family_Tree.md**: "[MAIDEN-A]" (from Geni tree)
- **Birth certificate of child**: "[MAIDEN-B]" (mother's maiden name field)
- **Resolution**: Birth certificate (Tier 1) outranks Geni (Tier 3). Updated all files to [MAIDEN-B]. Added to Data Discrepancies section in person file.

### Discrepancy 3: Birthplace
- **Person**: [2G-GP-1]
- **Geni profile**: Born in "[CITY-X], [COUNTRY-A]"
- **Passenger manifest**: Last residence listed as "[CITY-Y], [COUNTRY-A]"
- **Resolution**: These may both be correct (born in one city, lived in another before emigrating). Recorded both. Birthplace remains [CITY-X] per Geni; last residence [CITY-Y] per manifest. Neither is a primary source for birthplace. Added to Open Questions.

### Discrepancy 4: Death Date
- **Person**: [GRANDPARENT-3]
- **Find a Grave**: "d. September 15, 2005"
- **Obituary**: "d. September 14, 2005"
- **Resolution**: Both are Tier 2 sources. The obituary was likely written the day after death, so "September 14" is more likely the actual date (with the obituary published on the 15th). Updated to September 14, 2005 with note.

## Audit Summary

| Status | Count |
|---|---|
| Discrepancies found | 7 |
| Resolved (clear hierarchy) | 4 |
| Resolved (judgment call) | 2 |
| Unresolved (added to Open Questions) | 1 |
