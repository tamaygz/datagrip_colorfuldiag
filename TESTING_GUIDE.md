# Colorful Diagrams Plugin - Testing Guide

## Build Status
✅ **BUILD SUCCESSFUL** - All compilation errors resolved.

## Ready for Testing

The plugin is now ready for testing in DataGrip. Follow these steps to test the implementation:

### 1. Launch DataGrip with Plugin
```bash
cd c:\Users\Tamay\vscode\datagrip_colorfuldiag
.\gradlew.bat runIde
```

This starts a DataGrip IDE sandbox with the plugin installed.

### 2. Create a Database Connection & Diagram
- Create a new database connection (or use an existing test database)
- Open the Database Tool Window
- Create a new ER Diagram by selecting tables
- The diagram should open in the editor

### 3. Test Toolbar Integration
The diagram editor should show our custom toolbar with buttons for:
- **Color Selected Tables** - Click to open color picker
- **Create Container** - Click to create a visual container
- **Add Sticky Note** - Click to add a floating note
- **Reset Colors** - Click to remove custom colors

### 4. Test Sticky Notes Feature
- Click "Add Sticky Note" button
- It should open a dialog asking for:
  - Note text
  - X coordinate (optional)
  - Y coordinate (optional)
- After clicking OK:
  - A yellow sticky note should appear on the diagram
  - A `{diagram_name}_colorfuldiag.json` file should be created next to the diagram source

### 5. Test Tool Window
- The "Colorful Diagrams" tool window should auto-open at the bottom
- It displays the available actions and current diagram metadata
- Shows registered tables and containers

### 6. Test Metadata Persistence
- Create a sticky note with text "Test Note"
- Close the diagram
- Reopen the diagram
- The sticky note should still be there

### 7. Test Container Creation
- Click "Create Container"
- A dialog should open for container name and color
- A colored rectangle should appear on the diagram overlay
- The container should be shown in the metadata JSON file

## Troubleshooting

### Visual Elements Not Appearing
If sticky notes or containers don't appear:

1. **Check Console for Errors**
   - Open Help → Show Logs in Explorer
   - Look for exception stack traces

2. **Verify Component Hierarchy**
   - The diagram editor uses a complex component structure
   - Overlay panel attachment might fail if component doesn't match expected hierarchy

3. **Check Metadata File**
   - Look for `{diagram_name}_colorfuldiag.json` next to the diagram
   - Should contain entries for sticky notes or containers

### Plugin Not Appearing
If toolbar/actions don't appear:
1. Check `plugin.xml` was properly registered
2. Restart DataGrip IDE sandbox
3. Check Help → About → Plugins to see if plugin is loaded

## Expected File Structure

When you use the plugin on a diagram, it should create:
```
my-diagram.graphml  (original diagram)
my-diagram_colorfuldiag.json  (plugin metadata)
```

The JSON structure:
```json
{
  "tables": {
    "schema.table_name": {
      "color": "#RRGGBB"
    }
  },
  "containers": [
    {
      "id": "container_1",
      "title": "Container Name",
      "color": "#RRGGBB",
      "bounds": [x, y, width, height],
      "tables": ["schema.table1", "schema.table2"]
    }
  ],
  "notes": [
    {
      "id": "note_1",
      "text": "Sticky note content",
      "color": "#FFFF00",
      "position": [x, y]
    }
  ]
}
```

## Next Steps for Enhancement

### 1. Direct Table Coloring
To color the actual database tables in the diagram (not just overlays), you have two options:

**Option A: Use DataGrip Native Coloring**
- Right-click table in Database window → Tools → Set Color
- This persists with the database metadata
- More integrated with DataGrip

**Option B: Custom DiagramProvider**
- Implement a custom `DiagramProvider` that hooks into DataGrip's diagram API
- Use `BasicGraphPresentationModel` to customize `NodeRealizer`
- More powerful but requires deeper API integration

See `DIAGRAM_RENDERING_GUIDE.md` for architectural options.

### 2. Interactive Features
- Drag & drop sticky notes (partially implemented in OverlayPanel)
- Resize containers
- Drag tables into containers

### 3. Export/Import
- Save diagram metadata as separate JSON file
- Allow importing metadata from other diagrams
- Add UI for import/export in toolbar

## Architecture Overview

The plugin consists of:

- **Actions** - Toolbar buttons in diagram editor
- **DiagramMetadataService** - Persists metadata to JSON files
- **DiagramEditorListener** - Listens for diagram open events
- **OverlayPanel** - Renders containers and sticky notes as transparent overlays
- **DiagramRefreshManager** - Synchronizes metadata with UI in real-time
- **Tool Window** - Dockable UI panel showing plugin features
- **Dialogs** - Color picker, sticky note input, container creation

## Known Limitations

1. **Overlay-based approach** - Visual elements are overlays, not integrated into diagram rendering
   - Pro: Non-invasive, works with any diagram
   - Con: Doesn't inherit diagram theming, may not align perfectly with zoom/scroll

2. **Color application to tables** - Currently doesn't color actual database tables
   - Tables must be colored using DataGrip's native "Set Color" feature
   - Future: Custom DiagramProvider for native integration

3. **Component attachment** - Relies on component hierarchy matching
   - If DataGrip changes diagram editor structure, overlay attachment may fail
   - Defensive implementation with fallback logic

## Verification Checklist

- [ ] Plugin loads without errors
- [ ] Toolbar buttons appear in diagram editor
- [ ] Sticky note dialog opens and accepts text
- [ ] Sticky note appears on diagram after creation
- [ ] Metadata JSON file is created
- [ ] Metadata persists after diagram reload
- [ ] Tool window appears and shows metadata
- [ ] Container creation dialog works
- [ ] Container appears on diagram
- [ ] Console shows no critical errors

## Support

For issues or questions:
1. Check console logs (Help → Show Logs in Explorer)
2. Review `DIAGRAM_RENDERING_GUIDE.md` for architecture details
3. Check `projectspecs.instructions.md` for feature specifications
