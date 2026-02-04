# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Colorful Diagrams** is a JetBrains DataGrip plugin that extends the built-in database diagram designer with visual organization features: table coloring, visual containers, and sticky notes. All features are purely visual and do not modify the database schema.

- **Language:** Java (IntelliJ Platform SDK)
- **Build System:** Gradle with IntelliJ Plugin DSL
- **Target:** JetBrains DataGrip
- **Editing Environment:** VSCode

## Build Commands

```bash
# Build the plugin
gradle build

# Run DataGrip with plugin loaded for testing
gradle runIde

# Create distributable ZIP
gradle buildPlugin

# Run tests
gradle test
```

## Architecture

### Core Features

1. **Table Coloring** - Color picker for diagram tables, persisted in metadata
2. **Visual Containers** - Custom diagram objects that group tables and apply inherited colors
3. **Sticky Notes** - Free-floating notes with text, color, and position
4. **Toolbar Actions** - Custom toolbar in diagram editor for all plugin actions
5. **Persistence** - JSON metadata files (`*_colorfuldiag.json`) stored alongside diagram source

### Planned Project Structure

```
src/main/java/com/example/colorfuldiag/
├── actions/        # Toolbar action implementations
├── diagram/        # IntelliJ Diagram API extensions
├── model/          # Data model (TableColor, Container, Note)
├── persistence/    # JSON serialization layer
└── ui/             # Color picker, dialogs
```

### Key Integration Points

- **IntelliJ Diagram API** - Extend rendering, context menus, drag & drop
- **Action System** - Register custom toolbar actions in plugin.xml
- **Database Plugin** - Depends on `bundledPlugin("com.intellij.database")`
- **Persistent Components** - Store/restore metadata across sessions

### Data Model

JSON file format for `(diagram_name)_colorfuldiag.json`:
```json
{
  "tables": { "public.users": { "color": "#ff0000" } },
  "containers": [{ "id": "c1", "title": "Auth", "color": "#00ff00", "bounds": [10,10,300,200], "tables": ["public.users"] }],
  "notes": [{ "id": "n1", "text": "Important", "color": "#ffff00", "position": [100,100] }]
}
```

## Constraints

- Plugin must not modify database schema
- All features layered on top of existing diagram editor without breaking native features (layout, zoom, export, navigation)
- Works offline with no vendor lock-in
