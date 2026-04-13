# Document Triage

How to classify and route scanned documents for processing.

## Step 1: Sort Into Categories

Examine each scanned document and classify it:

| Category | Examples | Route To |
|---|---|---|
| **Vital records** | Birth, marriage, death certificates | `templates/certificate.md` → high priority |
| **Military records** | Discharge papers, service records | `templates/transcription.md` → high priority |
| **Church records** | Baptism, confirmation, burial records | `templates/certificate.md` |
| **Legal documents** | Wills, deeds, naturalization papers | `templates/transcription.md` |
| **Newspapers** | Obituaries, wedding announcements | `templates/transcription.md` |
| **Personal correspondence** | Letters, postcards | `templates/transcription.md` |
| **Photographs** | Portraits, group photos, buildings | Photo catalog (no OCR) |
| **Family records** | Baby books, family bibles, diaries | `templates/transcription.md` → careful handling |
| **Genealogical documents** | Ahnenpass, family tree printouts | `templates/transcription.md` |

## Step 2: Determine OCR Method

| Document Characteristic | OCR Method |
|---|---|
| Clearly printed English text | Tesseract (ocrmypdf) |
| Clearly printed non-English text | Tesseract with language pack |
| Typed/typewritten text | Tesseract |
| Handwritten (any language) | Claude multimodal |
| Old printing (Fraktur, blackletter) | Claude multimodal |
| Mixed printed + handwritten | Layered (Tesseract + Claude) |
| Faded, damaged, or stained | Preprocess with ImageMagick, then Claude multimodal |
| Photograph with no text | No OCR; catalog only |

## Step 3: Process in Priority Order

1. **Vital records and military records first**: These are primary sources with the highest genealogical value
2. **Church and legal records second**: Primary sources with structured data
3. **Newspapers and correspondence third**: Secondary sources with contextual value
4. **Family records fourth**: Often contain unique information but require careful interpretation
5. **Photographs last**: Catalog, but no OCR needed

## Step 4: Quality Review

After processing each document:
- Grade the OCR quality (Good / Partial / Bad / Photo-Only)
- Verify extracted facts against existing vault data
- Flag any discrepancies for the cross-reference audit
- Update the Data Inventory with the new source
