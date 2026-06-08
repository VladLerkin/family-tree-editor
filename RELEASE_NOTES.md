# Release Notes

This document contains the release history of the Family Tree Editor project, from the first commits to the current version 2.1.2. It highlights when new features were added, bug fixes, and CI/CD improvements.

## 2.1.x Versions (June 2026)

**v2.1.2** (2026-06-08) — Current release

**v2.1.1** (2026-06-08)

**v2.0.44** (2026-06-08)
* **AI & Improvements:** Fixed encoding in AI clients and optimized the performance of offline audio transcription (Vosk).

---

## 2.0.x Versions (April - June 2026)

A major update of the application's second generation, focusing on autonomous AI capabilities (including local models), improved integration, and platform stability.

**v2.0.41 - v2.0.43** (2026-06-07)
* **Fully Local AI:** Added fully offline (local AI) inference using **Llamatik** and **Metal GPU** hardware acceleration for processing texts and relationships without an internet connection.

**v2.0.40** (2026-06-05)
* **New AI Models:** Added a preset for the `Qwen 2.5 3B` model and finalized the offline audio transcription setup via Vosk.
* **Fix:** Resolved a JSON parser crash when receiving data from AI (`coerceInputValues`).

**v2.0.28 - v2.0.39** (2026-06-03 — 2026-06-04)
* **Build (Desktop/CI):** Configured a dynamic JMODs resolver for ProGuard, ensuring Java 25 support both in CI and locally. Removed `settings.json` from tracking and added a template.

**v2.0.8 - v2.0.27** (2026-04-22 — Late May 2026)
* **Genealogy Agent (AI Researcher):** Introduced deep integration with **FamilySearch** for autonomous genealogical research. Optimized profile matching, improved search precision and error handling, and increased the number of analysis iterations for the agent (2026-04-22).

---

## 1.4.x Versions (January - April 2026)

A period of architectural UI refactoring (introducing MVI/MVVM), dependency updates (Compose, Koin, Voyager), and the introduction of Markdown export.

**v1.4.37 - v1.4.44** (2026-04-12 — 2026-04-14)
* **Architecture:** A massive project refactoring, migrating to **Koin** for Dependency Injection across all platforms.
* **Build (iOS):** Fixed issues with launching iOS from Android Studio.
* **Build (Android):** Automated the synchronization of Android resources from common resources, upgraded AGP to 9.1.1.

**v1.4.20 - v1.4.36** (2026-04-03 — 2026-04-11)
* **UI Architecture:** Dialog windows were migrated to the MVI/MVVM architecture, and the **Voyager Navigator** navigation library was integrated (2026-04-03).
* **CI Stabilization:** Major fixes for Kotlin compiler warnings, introduced secure Keystore from GitHub Secrets, and removed deprecated Material 3 APIs. Improved the `[skip ci]` logic.

**v1.4.13 - v1.4.19** (2026-03-15 — 2026-04-02)
* **Feature (Export):** Implemented **family tree export to Markdown format** (2026-03-19).
* **UI Modularization:** The `MainScreen` interface and dialogs were extracted into separate Compose modules (2026-04-02).
* **Build:** Transitioned to `Version Catalog` for dependency management.

**v1.4.0 - v1.4.12** (2026-01-02 — 2026-02-12)
* **Platform Update:** Full update to Gradle 9.2.1/9.3.1, Kotlin 2.3.0, Java 25, and Compose 1.10.x.
* **Refactoring:** Applied the DRY principle to the AI configuration (AiConfigDialog, BaseAiClient) and the GEDCOM exporter. Improved UUID generation.

---

## 1.3.x Versions (November - December 2025)

The phase of adding Web support, Android TV, AI recognition integration, and significant UI improvements.

**v1.3.27** (2025-12-26)
* **New Platform (Web):** Added support for the Web platform (Wasm/JS) with native file dialogs and specific implementation for Wasm.

**v1.3.23 - v1.3.26** (2025-12-22 — 2025-12-24)
* **Android TV / Voice:** Fixed voice input on Android TV, improved Google Speech-to-Text integration. Removed Kotlin/Native-incompatible calls (System.gc) in the iOS build.

**v1.3.15 - v1.3.17** (2025-12-05 — 2025-12-06)
* **UI/UX:** Menu reorganization — all menu items were moved to the native **MenuBar** for Desktop, while mobile versions retained the "three-dot" menu (2025-12-06).
* **Improvements:** Updated platform icons, error messages, and download logic.

**v1.3.14** (2025-12-03)
* **Fix:** Fixed the voice recording stop button.

**v1.3.2 - v1.3.12** (2025-12-01 — 2025-12-02)
* A series of technical releases aimed at stabilizing the CI/CD build process for all platforms (iOS, Android, Desktop, Windows).

**New Features and Changes (between v1.3.0 and v1.3.2)**:
* **FamilySearch Integration (REL-55):** Added the architecture for the Search API and FamilySearch API integration (2025-12-01).
* **AI Audio Recognition (REL-52, REL-53, REL-54):** Added support for voice recognition in 90 languages (Yandex STT and LLMs) for Desktop and iOS, along with automatic family tree building based on audio (2025-11-22...2025-11-29).
* **AI Text Analysis (REL-51):** Added text import with automatic extraction of people and relationships (2025-11-19).
* **Interactive Canvas (REL-47, REL-45):** Added the ability to add/remove persons directly on the canvas and fixed person Drag & Drop (2025-11-16).
* **Project Management (REL-42, REL-44):** Added source records management and the loading of sample data (2025-11-15).
* **UI Improvements (REL-35, REL-39, REL-43):** Properties panel, Date Picker component, and "About" window.

**v1.3.0** (2025-11-10)
* **Feature:** GEDCOM support for the Kotlin-Multiplatform version (REL-34).
* **UI/UX:** iPhone support, added a canvas with transformations (pan & zoom) (REL-33).

---

## 1.2.x Versions (September - October 2025)

A period of active project migration from Java to Kotlin Multiplatform and setup of automated releases in GitHub CI/CD.

**v1.2.25** (2025-10-23)
* **Architecture:** Completed the project transfer from Java to Kotlin (REL-30).

**v1.2.23 - v1.2.24** (2025-10-16)
* **Feature:** Added full support for the GEDCOM 5.5.5 format (REL-29).

**v1.2.20 - v1.2.22** (2025-10-12)
* **Feature:** Complex date phrase input dialog (REL-026).
* **Feature:** Added a place field for events (REL-027).

**v1.2.0 - v1.2.19** (2025-10-10)
* **CI/CD & UI:** Intensive setup of GitHub Actions. Introduced Linux build support. Fixed media editing dialogs.

---

## Initial Releases and Prototypes (August - September 2025)

* **REL-008:** Added a custom import format (rel import format) (2025-09-28).
* **REL-006:** Basic support for GEDCOM 5.5 import and export (2025-09-26).
* **Project Start:** Initial commits (2025-08-21).
