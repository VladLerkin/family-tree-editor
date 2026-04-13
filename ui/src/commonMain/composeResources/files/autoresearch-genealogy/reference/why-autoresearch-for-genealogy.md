# Why Autoresearch for Genealogy

## The Case for Autonomous AI Loops in Family History Research

Genealogy research is uniquely suited to autonomous AI loops. Unlike code (where there is a compiler) or math (where there are proofs), genealogy has no single source of truth. Sources disagree with each other. Names change across records. Dates are approximate. Confidence is a spectrum, not a binary.

This makes genealogy both harder and more interesting for AI. The "autoresearch" approach (inspired by Andrej Karpathy's concept of autonomous goal-directed iteration) adapts to these challenges.

## What Makes Genealogy Different

### No Compiler

In software, you write code, run it, and the compiler tells you if it works. In genealogy, there is no equivalent. A birth certificate says one date. A gravestone says another. A family tree screenshot says a third. All three might be "valid" documents, but at most one date is correct.

The autoresearch solution: replace the compiler with **cross-referencing**. After each research iteration, run a verification pass that compares every claim against every source. Discrepancies become the metric to minimize.

### Sources Disagree

A marriage certificate lists a maiden name as "Beck." A family photo caption says "Arnold." A family tree screenshot says "Arnold." Which is correct?

The autoresearch solution: establish a **source hierarchy**. Primary documents (vital records, church registers) outrank secondary sources (newspapers, published genealogies), which outrank tertiary sources (family trees, oral history). When sources conflict, the hierarchy breaks the tie. When the hierarchy is ambiguous, document both versions and flag for human review.

### Confidence Is Probabilistic

A DNA test says 47.7% Scandinavian. Does that mean exactly 47.7%? No. It means roughly half, give or take a few percentage points. A regional match to a specific county is suggestive, not definitive.

The autoresearch solution: use **confidence tiers** (Strong Signal / Moderate Signal / Speculative) instead of treating all claims as equally certain. Every fact in the vault carries an implicit or explicit confidence level.

### Negative Results Are Data

You searched an entire emigration database and found nothing. That is not a failure. That is information: the person either did not emigrate through that port, used a different name, or the records are incomplete.

The autoresearch solution: **log every search**, positive or negative. The Research Log becomes a map of what has been explored and what has not. This prevents duplicate work and reveals patterns.

## How Autoresearch Adapts to Genealogy

### Metrics

In code autoresearch, the metric might be "test pass rate" or "benchmark score." In genealogy, useful metrics include:

- **Count of sourced individuals**: How many people in the tree have at least two independent sources?
- **Count of open questions resolved**: How many research gaps have been closed?
- **Count of discrepancies remaining**: How many conflicts exist between the family tree and source documents?
- **Count of deceased without memorials**: How many dead ancestors lack Find a Grave or cemetery records?
- **GEDCOM completeness**: How many people in the tree are missing from the GEDCOM export?

### Verification

Each iteration produces new claims. The verification step cross-references those claims against existing data. This catch-and-correct loop prevents error accumulation.

### Guard Rails

Genealogy autoresearch needs specific guards:
- Do not fabricate ancestors (AI hallucination is especially dangerous when adding people to a tree)
- Do not trust user-contributed trees without independent corroboration
- Do not silently resolve conflicts (document them instead)
- Do not infer from DNA what genealogical evidence should confirm
- Do not include living persons' private data

## What AI Can and Cannot Do

### AI Can

- **Search the open web** for free genealogical databases (Find a Grave, FamilySearch wiki, Geni, WikiTree, Chronicling America, Digitalarkivet)
- **Cross-reference** facts across dozens of files instantly
- **Extract structured data** from OCR'd documents (dates, names, places, relationships)
- **Read and interpret** scanned documents using multimodal capabilities (handwriting, old typefaces, foreign languages)
- **Maintain consistency** across a large vault by checking every file against every other file
- **Log exhaustively** without fatigue or forgetting

### AI Cannot

- **Access login-required databases** (Ancestry.com, Newspapers.com, most archival portals)
- **Verify handwriting with certainty** (old scripts like German Kurrent or faded ink require human judgment)
- **Replace professional genealogists** for complex research (courthouse visits, archival appointments, reading deteriorated originals)
- **Guarantee accuracy** (AI can and will make mistakes; human review of every claim is essential)
- **Generate DNA** or run genetic analyses (it can only interpret existing test results)
- **Contact archives or order records** (it can draft the letter, but a human must send it)

## The Honest Assessment

AI-assisted genealogy accelerates the mechanical parts of research: searching, cross-referencing, extracting, logging, and organizing. For a real project, one extended AI session produced 105 files spanning 9 generations across 6 family lines. That same work would have taken weeks or months of manual effort.

But AI does not replace judgment. Every AI-generated claim must be reviewed by a human who understands the context. The source hierarchy must be enforced by a person who knows the difference between a vital record and a family legend. The confidence tiers only work if someone with domain knowledge assigns them honestly.

The best results come from a partnership: AI does the searching, extracting, and organizing; the human does the evaluating, deciding, and contextualizing.
