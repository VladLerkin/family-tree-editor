# GEDCOM Guide

A practical guide to GEDCOM 5.5.1 format for AI-generated family tree exports.

## What Is GEDCOM

GEDCOM (GEnealogical Data COMmunication) is the standard file format for exchanging genealogical data between software programs. Version 5.5.1 is the most widely supported.

A GEDCOM file is a plain text file with the extension `.ged`. It uses a hierarchical structure with numbered levels and tags.

## Basic Structure

```
0 HEAD                          ← File header
1 SOUR autoresearch-genealogy   ← Source software
1 GEDC
2 VERS 5.5.1                   ← GEDCOM version
2 FORM LINEAGE-LINKED
1 CHAR UTF-8                   ← Character encoding

0 @I1@ INDI                    ← Individual record (ID: I1)
1 NAME Given /Surname/         ← Name (surname in slashes)
1 SEX M                        ← Sex (M or F)
1 BIRT                         ← Birth event
2 DATE 10 MAY 1866             ← Date (DD MMM YYYY format)
2 PLAC City, County, State, Country  ← Place (comma-separated, specific to general)
1 DEAT                         ← Death event
2 DATE 12 DEC 1950
2 PLAC City, State, Country
1 FAMS @F1@                    ← Family as spouse (link to FAM record)
1 FAMC @F2@                    ← Family as child (link to FAM record)

0 @F1@ FAM                     ← Family record (ID: F1)
1 HUSB @I1@                    ← Husband (link to INDI)
1 WIFE @I2@                    ← Wife (link to INDI)
1 CHIL @I3@                    ← Child (link to INDI)
1 CHIL @I4@                    ← Another child
1 MARR                         ← Marriage event
2 DATE 15 JUN 1911
2 PLAC City, State, Country

0 TRLR                         ← File trailer (required, must be last)
```

## Common Tags

| Tag | Level | Meaning |
|---|---|---|
| HEAD | 0 | File header |
| INDI | 0 | Individual record |
| FAM | 0 | Family record |
| TRLR | 0 | File trailer |
| NAME | 1 | Person's name |
| SEX | 1 | Sex (M/F) |
| BIRT | 1 | Birth event |
| DEAT | 1 | Death event |
| MARR | 1 | Marriage event |
| BURI | 1 | Burial event |
| DATE | 2 | Date of event |
| PLAC | 2 | Place of event |
| SOUR | 2 | Source citation |
| NOTE | 1/2 | Free-text note |
| HUSB | 1 | Husband in FAM |
| WIFE | 1 | Wife in FAM |
| CHIL | 1 | Child in FAM |
| FAMS | 1 | Family as spouse |
| FAMC | 1 | Family as child |
| OCCU | 1 | Occupation |
| RESI | 1 | Residence |
| IMMI | 1 | Immigration |
| NATU | 1 | Naturalization |
| EVEN | 1 | General event |

## Date Formats

GEDCOM dates use the format `DD MMM YYYY` with three-letter English month abbreviations:

| Format | Example | Meaning |
|---|---|---|
| DD MMM YYYY | 10 MAY 1866 | Exact date |
| MMM YYYY | MAY 1866 | Month and year only |
| YYYY | 1866 | Year only |
| ABT YYYY | ABT 1866 | Approximate year |
| BEF YYYY | BEF 1866 | Before this year |
| AFT YYYY | AFT 1866 | After this year |
| BET YYYY AND YYYY | BET 1860 AND 1870 | Between two years |

Month abbreviations: JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC

## Place Format

Places should be listed from most specific to least specific, separated by commas:

```
City, County, State, Country
```

Examples:
- Clinton, Big Stone County, Minnesota, USA
- Davik, Sogn og Fjordane, Norway
- Vienna, Austria

## Privacy Considerations

- For living persons, use `1 NAME Living /Surname/` or omit birth dates
- Some genealogy programs will automatically privatize living persons on export
- Consider whether to include living persons at all in a shareable GEDCOM

## Validation Checklist

After generating a GEDCOM file:

1. Every `INDI` has at least a `NAME`
2. Every `FAM` has at least a `HUSB` or `WIFE`
3. Every `CHIL` ID referenced in a `FAM` exists as an `INDI`
4. Every `FAMS` and `FAMC` reference in an `INDI` exists as a `FAM`
5. No duplicate `INDI` records for the same person
6. No circular relationships (a person listed as their own ancestor)
7. The file ends with `0 TRLR`
8. Dates use the correct format (DD MMM YYYY)

## Import Testing

After creating a GEDCOM, import it into a genealogy program to verify:
- All persons appear correctly
- All relationships are properly linked
- Dates and places display as expected
- No orphaned records or broken links

Free programs for testing: Gramps (gramps-project.org), RootsMagic Essentials
