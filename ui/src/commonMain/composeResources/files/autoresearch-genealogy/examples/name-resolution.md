# Example: Name Resolution

How to resolve a single ancestor appearing under multiple names in different records.

## The Problem

An ancestor appears in the vault under three different names:
- Family records: "[ANCESTOR] Sakkarias [SURNAME]"
- Find a Grave: "[ANCESTOR] Anton [SURNAME]"
- 45 Ancestry trees: "[ANCESTOR] Zacharias Antonsen [SURNAME]"

Which is correct? Are these the same person?

## Investigation

### Step 1: Understand the Naming System

This ancestor is from a Scandinavian country where patronymic naming was used. This means:
- "Sakkarias" might be a middle name
- "Anton" might be a patronymic reference (son of Anton)
- "Antonsen" explicitly means "son of Anton"

### Step 2: Compare Across Sources

| Source | Name Given | Source Type |
|---|---|---|
| Family oral history | [ANCESTOR] Sakkarias [SURNAME] | Tertiary |
| Find a Grave memorial | [ANCESTOR] Anton [SURNAME] | Secondary |
| Ancestry trees (45) | [ANCESTOR] Zacharias Antonsen [SURNAME] | Tertiary (but consistent across many trees) |
| Immigration record | [ANCESTOR] [SURNAME] (no middle name) | Primary |

### Step 3: Analyze the Variants

- "Sakkarias" and "Zacharias" are phonetically similar. "Sakkarias" appears to be the family's rendering of "Zacharias," a legitimate variant in this region documented from the 1500s.
- "Anton" in the Find a Grave record is not a middle name; it is a shortened form of the patronymic "Antonsen" (son of Anton).
- "Antonsen" in the Ancestry trees is the full patronymic, meaning the ancestor's father was named Anton.

### Step 4: Reconstruct the Full Name

The full name is: **[ANCESTOR] Zacharias Antonsen [SURNAME]**

Breakdown:
- [ANCESTOR]: Given name
- Zacharias: Middle name (rendered as "Sakkarias" by the family)
- Antonsen: Patronymic (son of Anton), shortened to "Anton" in some records
- [SURNAME]: Farm name (used as a fixed surname)

### Resolution

All three name variants are parts of the same full name. Neither the family nor Find a Grave was wrong; they each preserved different parts of the complete name. The family kept the middle name ("Sakkarias"), Find a Grave kept the patronymic ("Anton"), and Ancestry trees reconstructed the full form.

**Vault update**: Person file updated to show the full name with a note explaining all variants. Family_Tree.md updated. A `## Data Discrepancies` section added documenting the resolution.

## Lesson

In patronymic naming systems, what appears to be a name conflict may actually be different parts of the same multi-component name. Before assuming a discrepancy, understand the naming conventions of the region and era.
