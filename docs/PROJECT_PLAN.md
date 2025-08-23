### **Technical Specification for the Software "Pedigree Chart Editor"**

**1. General Information**

* **Project Name:** "Pedigree Chart Editor"
* **Basis for Development:** Analysis of existing documentation (`help.chm` file) for the purpose of recreating or developing a similar software product.
* **Purpose:** The software is intended for creating, editing, visualizing, storing, and exporting genealogical data (family trees).

**2. Project Goals and Objectives**

* **Goal:** To create an intuitive desktop application for users involved in genealogy (both hobbyists and professionals).
* **Objectives:**
    * To implement a graphical interface for visually constructing and editing a family tree.
    * To provide functionality for managing data about individuals (persons) and families.
    * To support the standard genealogical data interchange format, GEDCOM.
    * To provide functions for exporting the tree into popular formats (HTML, SVG, images).
    * To implement user-friendly tools for data navigation and search.

**3. System Functional Requirements**

Based on the analysis of `howto*.htm`, `menu*.htm`, `list*.htm`, and other files, the system must have the following functionality:

**3.1. Data Management**

* **Individuals (Records):**
    * Creation of a new record for an individual. Required fields: First Name, Last Name, Gender. Additional fields: Date and place of birth, date and place of death, notes, media files.
    * Editing the data of an existing individual.
    * Deleting an individual from the database.
* **Families:**
    * Creation of a new family by establishing a relationship between two individuals (spouses).
    * Adding children to a family, establishing parent-child relationships.
    * Editing family information (e.g., date and place of marriage).
    * Deleting a family (breaking the links between spouses and children).
* **Lists and Views:**
    * Display lists of all individuals, families, and relationships in a tabular format (`listRec.htm`, `listFam.htm`, `listRel.htm`).

**3.2. Graphical Tree Editor** (`editor.htm`, `pedigree.htm`)

* **Workspace (Canvas):** An infinite canvas for visualizing and editing the genealogical tree.
* **Visual Elements:** Individuals and families should be represented as graphical blocks connected by lines that reflect their relationships.
* **Navigation:**
    * **Scaling (Zoom):** Increasing and decreasing the display scale of the tree (`howtoZoom.htm`).
    * **Panning:** Moving across the canvas using the mouse (`howtoMove.htm`).
* **Object Manipulation:**
    * **Selection:** Ability to select one or more objects on the canvas (`howtoSelect.htm`).
    * **Copy, Cut, Paste:** Standard operations for objects (`howtoCopyCutPaste.htm`).
    * **Move:** Changing the position of objects on the canvas for better viewing.
* **Alignment Tools:** (`menuViewAlign.htm`, `menuViewDist.htm`)
    * Functions for automatically aligning selected objects (by top edge, center, etc.).
    * Functions for evenly distributing objects on the canvas.

**3.3. File Management** (`menuFile.htm`)

* **Create New Project:** (`howtoNewFile.htm`).
* **Open/Save:** Saving the project in a native format that stores both the data and the visual layout of the elements.
* **Import:**
    * Import data from the **GEDCOM 5.5** standard (`menuFileImpGED5.5.htm`).
* **Export:**
    * Export data to the **GEDCOM 5.5** format (`menuFileExpGED55.htm`).
    * Export the visual representation of the tree to **HTML** (`menuFileExpHTML.htm`).
    * Export to the vector format **SVG** (`menuFileExpSVG.htm`).
    * Export to a raster image (e.g., PNG, JPG) (`menuFileExpImage.htm`).
* **Print:** Output the current view of the tree to a printer (`menuFilePrint.htm`).

**3.4. Additional Features**

* **Search:** A quick search function for individuals by first or last name (`quickSearch.htm`).
* **Notes:** The ability to add text notes to individuals and families (`notes.htm`).
* **Media Files:** Attach images, documents, and other media files to records (`mediamaster.htm`).
* **User Tags:** The ability to assign tags to records for categorization (`menuEditUserTags.htm`).
* **Undo/Redo:** (`menuEditUndo.htm`).

**4. Non-Functional Requirements**

* **Platform:** The application should be developed for the Windows OS (an assumption based on the `.chm` file format).
* **Performance:** The system must ensure fast rendering and navigation for trees containing up to 5,000 individuals.
* **Interface:** The graphical user interface must be intuitive, with a classic menu (File, Edit, View, Tools) and a toolbar for quick access to main functions.
* **Reliability:** Ensure data integrity during save, import, and export operations. Provide for data backup.

**5. Development Stages (Preliminary)**

1.  **Design (TBD):**
    * Develop the application architecture.
    * Design the database structure or project file format.
    * Create user interface mockups (UI/UX).
2.  **Core Implementation (TBD):**
    * Create classes for individuals, families, and relationships.
    * Implement import and export functions for GEDCOM 5.5.
3.  **Editor Development (TBD):**
    * Create the graphical canvas.
    * Implement rendering, navigation (zoom, pan), and basic object manipulation.
4.  **Implementation of Additional Functionality (TBD):**
    * Add export to HTML/SVG/Image, printing, search, notes, etc.
5.  **Testing and Debugging (TBD):**
    * Functional testing, performance testing, usability testing.
6.  **Release Preparation (TBD):**
    * Create an installer, write user documentation.