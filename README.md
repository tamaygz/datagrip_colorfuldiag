# Colorful Diagrams - DataGrip Plugin

A JetBrains DataGrip plugin that extends the built-in database diagram designer with visual organization features.

## Features

### Table Coloring
- Select one or multiple tables in a diagram
- Apply custom colors using a color picker
- Choose from preset colors or create your own
- Colors persist when diagrams are reopened

### Visual Containers
- Create containers to visually group related tables
- Containers have customizable titles and colors
- Drag and resize containers freely
- Tables inside containers inherit the container's color (unless overridden)

### Sticky Notes
- Add free-floating notes anywhere on the diagram
- Notes support custom text and colors
- Resize and reposition notes as needed
- Perfect for documentation and annotations

### Persistence
- All plugin data is stored in JSON files alongside your diagrams
- File naming: `<diagram_name>_colorfuldiag.json`
- Export and import metadata for sharing

## Important Notes

- **No Database Modifications**: This plugin is purely visual. It does not modify your database schema or create any database objects.
- **Works Offline**: All features work without internet connection.
- **Compatible with Existing Diagrams**: Enhances existing diagrams without affecting native functionality.

## Installation

### From JetBrains Marketplace
1. Open DataGrip
2. Go to Settings → Plugins → Marketplace
3. Search for "Colorful Diagrams"
4. Click Install

### From ZIP
1. Download the plugin ZIP from releases
2. Go to Settings → Plugins → ⚙️ → Install Plugin from Disk
3. Select the downloaded ZIP file

## Usage

### Color Tables
1. Open a database diagram
2. Select one or more tables
3. Right-click → Colorful Diagrams → Color Table
   - Or use the toolbar: Colorful Diagrams → Color Selected Tables
4. Choose a color and click OK

### Create Containers
1. In the diagram toolbar, click Colorful Diagrams → Create Container
2. Enter a title and select a color
3. The container appears on the diagram
4. Drag tables into the container using Add to Container action

### Add Sticky Notes
1. Click Colorful Diagrams → Add Sticky Note in the toolbar
2. Enter your text and choose a color
3. Drag and resize the note as needed

### Export/Import Metadata
- **Export**: Colorful Diagrams → Export Metadata
- **Import**: Colorful Diagrams → Import Metadata

## Building from Source

### Prerequisites
- JDK 17 or higher
- Gradle 8.x

### Build Commands

```bash
# Build the plugin
./gradlew build

# Run DataGrip with the plugin loaded for testing
./gradlew runIde

# Create distributable ZIP
./gradlew buildPlugin

# Run tests
./gradlew test
```

The built plugin will be located at `build/distributions/colorfuldiag-<version>.zip`

## Development

### Project Structure

```
src/main/java/com/tamaygz/colorfuldiag/
├── actions/        # Toolbar and context menu actions
├── diagram/        # Diagram extensions and overlays
├── model/          # Data model classes
├── persistence/    # JSON serialization layer
└── ui/             # Dialogs and UI components

src/main/resources/
├── META-INF/       # plugin.xml configuration
└── icons/          # SVG icons
```

### Key Classes

- `DiagramMetadata` - Root model for all diagram customization data
- `DiagramMetadataService` - Project-level service for loading/saving metadata
- `ColorTableAction` - Action to color selected tables
- `OverlayPanel` - Renders containers and sticky notes

### Editing in VSCode

This project is designed to be edited in VSCode:
1. Open the project folder in VSCode
2. Install the "Extension Pack for Java" extension
3. The project will be recognized as a Gradle project
4. Use the terminal for Gradle commands

## Data Format

Metadata is stored in JSON format:

```json
{
  "tables": {
    "public.users": { "tableId": "public.users", "color": "#FF6B6B" },
    "public.orders": { "tableId": "public.orders", "color": "#4ECDC4" }
  },
  "containers": [
    {
      "id": "c1",
      "title": "Authentication",
      "color": "#45B7D1",
      "bounds": [10, 10, 300, 200],
      "tables": ["public.users", "public.sessions"]
    }
  ],
  "notes": [
    {
      "id": "n1",
      "text": "Main user tables",
      "color": "#FFEAA7",
      "position": [400, 50],
      "size": [150, 100]
    }
  ]
}
```

## License

MIT License - See LICENSE file for details.

## Author

Tamay Gündüz ([@tamaygz](https://github.com/tamaygz))

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
