# Getting Started

A step-by-step guide to setting up your genealogy research vault and running your first AI-assisted research session.

## Prerequisites

- A markdown editor (Obsidian recommended, but any editor works)
- Claude Code installed (or another AI tool that supports web search and file editing)
- Whatever you already know about your family (names, dates, locations, even if incomplete)
- Any physical documents you have (photos, certificates, letters)

## Step 1: Set Up Your Vault

1. Clone or download this repository
2. Copy the entire `vault-template/` folder into your Obsidian vault (or your preferred location)
3. Rename it to something meaningful (e.g., `Genealogy/`)

Your folder structure should look like:

```
YourVault/
└── Genealogy/
    ├── _Index.md
    ├── Family_Tree.md
    ├── Open_Questions.md
    ├── Research_Log.md
    ├── Data_Inventory.md
    ├── Timeline.md
    └── templates/
        ├── person.md
        ├── transcription.md
        └── region.md
```

## Step 2: Fill In What You Know

Open `Family_Tree.md` and start with yourself. Work backward through your parents, grandparents, and as far as you can go.

For each person, record whatever you know:
- Full name (including maiden names)
- Birth date and place (even approximate: "around 1870, somewhere in Poland")
- Death date and place
- Marriage date and spouse
- Parents' names

Do not worry about completeness. Gaps are what the research will fill.

**Example entry:**

```
## Paternal Line

### Smith Family

**John Smith** (b. March 15, 1935, Chicago, IL; d. January 2, 2010, Chicago, IL)
- Married Mary Jones on June 10, 1958 at St. Patrick's Church, Chicago
- Children: Robert (1960), Susan (1962), David (1965)
- Occupation: Electrician, IBEW Local 134
- Sources: family knowledge, obituary in Chicago Tribune

**Robert Smith** (b. 1905, Poland?; d. 1978, Chicago, IL)
- Father of John Smith
- Immigrated to US sometime before 1930
- Original surname may have been different
- Sources: family oral history (unverified)
```

## Step 3: Scan Your Documents

If you have physical documents (old photos, certificates, letters, postcards, military papers), scan or photograph them.

**Recommended setup:**
- Use a flatbed scanner at 300 DPI for documents, 600 DPI for photos
- If using a phone camera, use a document scanning app (not regular photos) for text documents
- Organize scans into folders by family line or document type

**File organization:**
```
~/Files/Genealogy/
├── [Family Line 1]/
│   ├── birth_certificate_john_smith.jpg
│   ├── wedding_photo_1958.jpg
│   └── military_discharge_robert_smith.jpg
└── [Family Line 2]/
    └── ...
```

Update `Data_Inventory.md` with your scan collections.

## Step 4: Run the Tree Expansion Prompt

1. Open Claude Code in your genealogy vault directory
2. Type `/autoresearch` (or paste the prompt contents directly)
3. Paste the contents of `prompts/01-tree-expansion.md`
4. Replace all placeholders:
   - `[VAULT_PATH]` → the path to your genealogy folder
   - Other placeholders as needed
5. Let it run

The AI will:
- Read your entire family tree
- Search the web for every ancestor's parents, siblings, and extended family
- Add new ancestors to your tree (with sources)
- Log every search in your Research Log
- Report how many new individuals were found

**Expect**: 8 iterations, each adding a few ancestors. After completion, review the changes.

## Step 5: Run the Cross-Reference Audit

After expanding the tree, verify the data:

1. Paste the contents of `prompts/02-cross-reference-audit.md`
2. Replace placeholders
3. Let it run

This will catch discrepancies introduced during expansion (conflicting dates, misspelled names, incorrect locations) and fix them using the source hierarchy.

## Step 6: Review and Plan Next Research

After the first two prompts, you will have:
- An expanded family tree with sources
- A research log documenting every search
- An audit file showing any discrepancies found and resolved

Now review:

1. **Open Questions**: What gaps remain? Add them to `Open_Questions.md` with priority rankings.
2. **Person files**: For important ancestors, create person files using the template. This centralizes everything known about one individual.
3. **Regional research**: If your family comes from a specific country or region, create a region file to track geographic-specific research.
4. **Next prompts**: Consider running `03-findagrave-sweep` or `04-gedcom-completeness` depending on your goals.

## What to Do Next

- **Want to find burial records?** Run the Find a Grave sweep (`prompts/03-findagrave-sweep.md`)
- **Want to export your tree?** Run the GEDCOM completeness check (`prompts/04-gedcom-completeness.md`)
- **Have scanned documents to process?** See `workflows/ocr-pipeline.md`
- **Want to understand your DNA results?** See `reference/dna-interpretation-guardrails.md`
- **Want to understand the methodology?** See `reference/why-autoresearch-for-genealogy.md`
