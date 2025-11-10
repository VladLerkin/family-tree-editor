# GEDCOM Support for KMP

This document describes the GEDCOM import/export functionality added to the Kotlin Multiplatform (KMP) family tree application.

## Overview

GEDCOM (GEnealogical Data COMmunication) is a standard format for exchanging genealogical data between different family tree applications. The KMP implementation supports GEDCOM 5.5.5 format with full cross-platform compatibility (Android, iOS, Desktop).

## Implementation

### Package Structure
All GEDCOM functionality is located in `kmp/core/src/commonMain/kotlin/com/family/tree/core/gedcom/`:

- **GedcomImporter.kt** - Parses GEDCOM text and builds ProjectData
- **GedcomExporter.kt** - Exports ProjectData to GEDCOM format
- **GedcomMapper.kt** - Utility functions for data mapping
- **GedcomIO.kt** - High-level API with file I/O integration
- **GedcomTest.kt** - Manual testing utilities

### Key Features

1. **Import Support**
   - Parses GEDCOM 5.5.5 format
   - Handles individuals (INDI) with names, gender, events
   - Handles families (FAM) with relationships
   - Handles notes (NOTE) with multi-line support
   - Extracts birth/death years from events
   - Preserves custom tags

2. **Export Support**
   - Generates valid GEDCOM 5.5.5 output
   - Creates proper xref IDs (@I1@, @F1@, @N1@)
   - Exports all individuals, families, events, notes, tags
   - UTF-8 encoding with proper header

3. **Platform Integration**
   - Uses FileGateway for platform-specific file I/O
   - String-based API works on all platforms
   - No platform-specific dependencies in core logic

## Usage Examples

### Basic Import/Export

```kotlin
// Import from string
val gedcomText = """
0 HEAD
1 GEDC
2 VERS 5.5.5
0 @I1@ INDI
1 NAME John /Doe/
1 SEX M
0 TRLR
""".trimIndent()

val projectData = GedcomIO.importFromString(gedcomText)

// Export to string
val exported = GedcomIO.exportToString(projectData)
```

### File-based Import/Export

```kotlin
// Opens platform file picker to import
val projectData = GedcomIO.importFromFile()

// Opens platform save dialog to export
val success = GedcomIO.exportToFile(projectData)
```

### Testing

```kotlin
// Run round-trip test
val success = GedcomTest.testRoundTrip()
// Creates sample data, exports, imports, validates
```

## Data Mapping

### Individual
- `firstName`, `lastName` ↔ NAME ("Given /Surname/")
- `gender` ↔ SEX (M/F/U)
- `birthYear` ← BIRT DATE (year extracted)
- `deathYear` ← DEAT DATE (year extracted)
- `events` ↔ BIRT, DEAT, BURI, ADOP, RESI events
- `notes` ↔ NOTE records
- `tags` ↔ _TAG custom fields

### Family
- `husbandId` ↔ HUSB xref
- `wifeId` ↔ WIFE xref
- `childrenIds` ↔ CHIL xrefs
- `events` ↔ MARR and other events
- `notes` ↔ NOTE records
- `tags` ↔ _TAG custom fields

### Events (GedcomEvent)
- `type` ↔ Event tag (BIRT, DEAT, MARR, etc.)
- `date` ↔ DATE field
- `place` ↔ PLAC field
- `notes` ↔ Subordinate NOTE records

## Technical Details

### Line Parsing
The importer uses regex-free parsing to handle GEDCOM line structure:
```
LEVEL [XREF] TAG [VALUE]
```

Examples:
- `0 @I1@ INDI` (level 0, xref @I1@, tag INDI)
- `1 NAME John /Doe/` (level 1, tag NAME, value "John /Doe/")
- `2 DATE 1 JAN 1950` (level 2, tag DATE, value "1 JAN 1950")

### Context Tracking
A context stack tracks the hierarchical structure to properly associate sub-records with their parents (e.g., DATE under BIRT event).

### ID Generation
During import, GEDCOM xrefs (@I1@, @F1@) are mapped to new internal IDs. During export, sequential xrefs are generated.

### Multi-line Notes
Notes can span multiple lines using CONC (concatenation) and CONT (continuation):
```
0 @N1@ NOTE First line
1 CONC of text
1 CONT Second line
```

## Limitations

1. Only year is extracted from dates (full date support in GedcomMapper but not in Individual model)
2. Media (OBJE) and Source (SOUR) records are supported in models but not yet imported/exported
3. UTF-8 encoding assumed (no auto-detection)
4. Submitter, Repository, Address info not yet handled
5. Limited to common event types (BIRT, DEAT, MARR, etc.)

## Future Enhancements

- Full date support (day/month/year)
- Media and source import/export
- Character encoding detection
- Validation and error reporting
- Support for more event types
- Address and contact information

## Compatibility

The implementation follows GEDCOM 5.5.5 specification and should be compatible with:
- Family Tree Maker
- Ancestry.com
- MyHeritage
- Gramps
- Other GEDCOM-compliant applications

## References

- [GEDCOM 5.5.5 Specification](https://www.gedcom.org/gedcom.html)
- [FamilySearch GEDCOM](https://www.familysearch.org/developers/docs/gedcom/)
