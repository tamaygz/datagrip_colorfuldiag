# Colorful Diagrams -- DataGrip Plugin Specification

**Plugin Name:** Colorful Diagrams\
**Slug:** colorfuldiag\
**Author:** Tamay Gündüz (@tamaygz on GitHub)\
**Target Product:** JetBrains DataGrip\
**Development Environment:** IntelliJ Platform Plugin SDK\
**Editing Environment:** VSCode

------------------------------------------------------------------------

## 1. Goal

Create a DataGrip plugin that extends the built‑in database diagram
designer with additional visual organization features.\
The plugin must not modify database schema or create real database
entities. All additions are purely visual and stored separately.

------------------------------------------------------------------------

## 2. Core Features

### 2.1 Toolbar in Diagram Editor

-   Add a custom toolbar inside the DataGrip diagram view.
-   Toolbar actions:
    -   Color selected tables\
    -   Create Container\
    -   Add Sticky Note\
    -   Reset colors\
    -   Export / Import plugin metadata

### 2.2 Table Coloring

-   User can select one or multiple tables in a diagram.
-   Action "Color Table" opens a color picker.
-   Selected color is applied as table background / header tint.
-   Colors must persist when diagram is reopened.

### 2.3 Visual Containers

-   Introduce a new visual object type: **Container**
-   Containers:
    -   are not database objects\
    -   exist only inside the diagram\
    -   can be created via toolbar\
    -   have title and color\
    -   can be resized and moved\
    -   tables can be dragged into a container
-   Behavior:
    -   Tables inside a container inherit container color\
    -   Individual table color can override container color\
    -   Removing table from container restores previous color

### 2.4 Sticky Notes

-   User can add free‑floating notes to diagram.
-   Notes support:
    -   Text content\
    -   Background color\
    -   Resizing\
    -   Drag & drop positioning

### 2.5 Persistence

-   All plugin data stored relative to diagram source.

-   File name pattern:

    (original_name)\_colorfuldiag.json

-   Data to store:

    -   table colors\
    -   container definitions\
    -   table → container mapping\
    -   sticky notes with position & text

-   File must be updated on:

    -   diagram save\
    -   explicit export action

------------------------------------------------------------------------

## 3. Technical Requirements

### 3.1 Project Setup

-   Gradle IntelliJ Plugin project
-   Target: DataGrip
-   Required dependency:

bundledPlugin("com.intellij.database")

-   Code must be compatible with latest IntelliJ Platform APIs.

### 3.2 Integration Points

-   Use IntelliJ Diagram API to extend:
    -   rendering\
    -   context menus\
    -   drag & drop\
    -   custom elements
-   Register:
    -   Actions\
    -   Diagram provider\
    -   Persistent component

### 3.3 UI Behavior

-   Plugin must not break native diagram features:
    -   layout\
    -   zoom\
    -   export\
    -   navigation
-   All features layered on top of existing editor.

### 3.4 Data Model

JSON structure example:

{ "tables": { "public.users": { "color": "#ff0000" } }, "containers": \[
{ "id": "c1", "title": "Auth", "color": "#00ff00", "bounds":
\[10,10,300,200\], "tables": \["public.users"\] } \], "notes": \[ {
"id": "n1", "text": "Important", "color": "#ffff00", "position":
\[100,100\] } \] }

------------------------------------------------------------------------

## 4. Deliverables

1.  Gradle project structure\
2.  plugin.xml with all registrations\
3.  Actions implementation\
4.  Diagram extensions\
5.  JSON persistence layer\
6.  README for VSCode editing\
7.  Build instructions

------------------------------------------------------------------------

## 5. Constraints

-   No modification of database schema\
-   No vendor lock‑in\
-   Works offline\
-   Compatible with existing diagrams\
-   Purely visual enhancement

------------------------------------------------------------------------

## 6. Acceptance Criteria

-   User can color tables\
-   User can create containers\
-   Tables inherit container color\
-   Sticky notes work\
-   JSON file created next to source\
-   Reload restores state\
-   Plugin does not affect database

------------------------------------------------------------------------

## 7. Build & Run

-   Gradle runIde targeting DataGrip\
-   Distributable ZIP plugin\
-   Compatible with marketplace rules
