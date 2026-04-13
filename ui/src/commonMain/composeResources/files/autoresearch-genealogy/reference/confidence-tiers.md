# Confidence Tiers

A framework for classifying genealogical claims by the strength of their evidence.

## The Three Tiers

### Strong Signal

Both independent data sources agree, and documented genealogy confirms the connection.

**Criteria**:
- At least two independent sources corroborate the claim
- Primary documents (vital records, church registers, military records) support it
- The claim is consistent with known historical and geographic context
- No contradicting evidence exists

**Examples** (anonymized):
- "~48% Scandinavian ancestry" when two DNA providers agree within 1%, multiple documented ancestors are from Scandinavia, and haplogroup analysis is consistent
- A birth date confirmed by both a birth certificate and a baptism record
- An emigration confirmed by both a departure record and an arrival manifest

### Moderate Signal

Sources roughly agree, but with some ambiguity. Partial genealogical support.

**Criteria**:
- One primary source supports the claim, or
- Multiple secondary sources agree, or
- DNA and genealogy point in the same direction but with some imprecision
- Minor contradictions exist but can be explained

**Examples** (anonymized):
- "~12 to 28% French/German" when providers give different percentages due to different categorization systems
- A birthplace identified from a secondary source (newspaper obituary) but not yet confirmed by a vital record
- A parent-child relationship established through census records but not a birth certificate

### Speculative

Suggested by limited or ambiguous evidence. Needs further research before it can be relied upon.

**Criteria**:
- Single source only, or
- Source is tertiary (family tree, oral history, photo caption), or
- Contradicted by other evidence, or
- Based on inference rather than direct documentation

**Examples** (anonymized):
- A maiden name listed in a photo caption but contradicted by a vital record
- A trace ancestry percentage (<2%) that appears in one provider but not another
- A family relationship stated in oral history but not found in any records

## When to Assign Each Tier

| Situation | Tier |
|---|---|
| Two independent primary sources agree | Strong |
| One primary source, no contradictions | Strong |
| One primary source, minor contradictions | Moderate |
| Multiple secondary sources agree | Moderate |
| Single secondary source | Moderate (leaning Speculative) |
| Tertiary source only | Speculative |
| Inference from circumstantial evidence | Speculative |
| Contradicted by a primary source | Speculative (or reject) |

## The Assignment Rule

A genealogical claim should be elevated in confidence only when:

1. **The evidence independently predicts it.** The claim should follow from the sources, not be retrofitted to match a desired conclusion.
2. **The magnitude is plausible.** If a DNA percentage implies a fully Scandinavian great-grandparent, there should be a documented Scandinavian great-grandparent in the tree.
3. **No alternative explanation exists.** If two different ancestor lines could explain a genetic signal, you cannot confidently assign it to just one.
4. **Ideally, phased data confirms it.** For genetic claims, being able to trace a segment to a specific parent's contribution is the strongest form of assignment.

## Using Tiers in Practice

- **In person files**: Set the `confidence` frontmatter field to `high`, `moderate`, `low`, or `stub`
- **In the family tree**: Mark unverified dates or relationships with `(unverified)` or `(speculative)`
- **In the genetic profile**: Group findings by Strong Signal / Moderate Signal / Speculative
- **In open questions**: Use tiers to prioritize which questions to research first (resolve Speculative claims before spending time on Strong ones)
