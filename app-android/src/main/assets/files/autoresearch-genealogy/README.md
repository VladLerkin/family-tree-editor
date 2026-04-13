# autoresearch-genealogy

Structured prompts, vault templates, and research workflows for AI-assisted genealogy research. Built for Claude Code, adaptable to any AI tool or manual workflow.

This project extracts and generalizes methods developed during a real genealogy research effort that produced 105 files spanning 9 generations across 6 family lines, using Claude Code's autonomous research capabilities.

## Who This Is For

- **Genealogy researchers** who want to use AI to accelerate their family history work without sacrificing source rigor
- **AI/tech enthusiasts** who want a concrete example of autonomous research loops applied to a humanities domain
- **Anyone** who has a box of old photos, a DNA test, and unanswered questions about their family

## Quick Start

1. Clone this repo
2. Copy the `vault-template/` folder into your Obsidian vault (or any markdown editor)
3. Fill in `Family_Tree.md` with what you already know (start with yourself, work backward)
4. Scan any physical documents you have (certificates, photos, letters)
5. Open Claude Code, paste the contents of `prompts/01-tree-expansion.md`, and run it
6. Review the results, then run `prompts/02-cross-reference-audit.md` to verify

See `workflows/getting-started.md` for the full walkthrough.

## What's Included

### Prompts (`prompts/`)

12 autoresearch prompts designed for Claude Code's `/autoresearch` command. Each defines a Goal, Metric, Direction, Verify condition, Guard rails, Iterations, and Protocol. They run autonomously: searching the web, updating your vault, and verifying their own work.

| Prompt | Purpose |
|---|---|
| 01-tree-expansion | Push every branch as far back as possible using web research |
| 02-cross-reference-audit | Find and fix discrepancies between your tree and source documents |
| 03-findagrave-sweep | Locate Find a Grave memorials for every deceased ancestor |
| 04-gedcom-completeness | Ensure your GEDCOM file matches your vault data |
| 05-source-citation-audit | Verify every person file cites at least two independent sources |
| 06-unresolved-persons | Identify and resolve unnamed people mentioned in your documents |
| 07-timeline-gap-analysis | Find life events where records should exist but have not been found |
| 08-open-question-resolution | Systematically attack every open research question |
| 09-bygdebok-extraction | Extract data from digitized local history books (any country) |
| 10-colonial-records-search | Search for colonial American ancestors in pre-1800 records |
| 11-immigration-search | Locate passenger manifests and naturalization records |
| 12-dna-chromosome-analysis | Analyze per-chromosome ancestry data to map genetic segments |

### Vault Template (`vault-template/`)

19 files: a complete Obsidian vault starter kit with YAML frontmatter, plain markdown, readable anywhere.

- **Core files**: Family tree, research log, open questions, data inventory, timeline, genetic profile, chromosome painting, witness network, unresolved persons, research strategy
- **Templates**: Person, transcription, certificate, postcard, region, surname, hypothesis, draft letter

### Archive Guides (`archives/`)

24 country and region-specific guides covering where to find records, what is free vs paid, and what AI tools can access directly vs what requires a browser.

**Europe**: Ireland, England/Wales, Scotland, France, Italy, Spain/Portugal, Germany, Netherlands, Austria, Hungary, Norway, Sweden, Poland, Russia/Ukraine

**Americas**: USA (colonial, immigration, census, vital records), African American, Canada, Mexico/Latin America

**Oceania**: Australia/New Zealand

**Cross-national**: Jewish genealogy

### Reference Guides (`reference/`)

9 methodology documents: confidence tiers, source hierarchy, DNA interpretation guardrails, naming conventions (patronymics, farm names, przydomki), GEDCOM format guide, common pitfalls, glossary, AI capabilities assessment, and the case for autoresearch in genealogy.

### Workflows (`workflows/`)

7 step-by-step guides: getting started, OCR pipeline, new ancestor intake, document triage, oral history protocol, discrepancy resolution, phase planning.

### Examples (`examples/`)

6 anonymized worked examples showing autoresearch in action: tree expansion session, cross-reference audit, DNA-to-genealogy mapping, name resolution, colonial deep dive.

## Philosophy

**Structured autonomous research with mechanical verification, not AI guessing.**

Genealogy is different from most AI tasks. There is no compiler. Sources disagree with each other. Confidence is probabilistic, not binary. A name that appears as "Sakkarias" in one record and "Zacharias" in another might both be correct. A date listed as 1820 in one source and 1925 in another is almost certainly wrong somewhere.

The autoresearch approach adapts to this by:

- **Defining measurable metrics** (count of sourced claims, count of resolved questions, count of remaining discrepancies)
- **Requiring verification after every iteration** (cross-reference audit, not just accumulation)
- **Logging negative results** (what you searched for and did not find is as important as what you found)
- **Maintaining confidence tiers** (Strong Signal / Moderate Signal / Speculative) rather than treating all claims as equal

This is inspired by Andrej Karpathy's autoresearch concept: autonomous goal-directed loops where the AI modifies, verifies, keeps or discards, and repeats. Applied to genealogy, the "compiler" is replaced by cross-referencing independent sources.

## License

MIT. See `LICENSE`.

## Developer Notes: Resource Manifest

This project uses a `manifest.json` file in the root of the resources directory. 

### Why it exists
Kotlin Multiplatform (KMP) with Compose Resources does not currently support listing files in a directory at runtime (the `Res` API is strictly typed). To allow the AI agents to "discover" which archive guides, prompts, or workflows are available, we maintain this manifest.

### How to update
If you add, remove, or rename any files in the subdirectories (e.g., `archives/*.md`, `prompts/*.md`), you **must** update the `manifest.json` accordingly.

The manifest follows this structure:
```json
{
  "archives": ["norway.md", "russia-ukraine.md", ...],
  "prompts": ["01-tree-expansion.md", ...],
  "workflows": [...]
}
```

## Contributing

Contributions welcome. If you have prompts, workflows, or archive guides that worked for your research, open a PR. Please ensure all examples use placeholder names (no real family data).
