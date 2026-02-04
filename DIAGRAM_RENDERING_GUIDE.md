# Diagram Canvas Real-Time Rendering - Implementation Guide

## Research Findings & Best Practices

### 1. **IntelliJ/DataGrip Diagram Rendering Architecture**

**Key Components:**
- **BasicGraphPresentationModel** - Customizes diagram appearance and behavior
- **NodeRealizer** - Renders individual nodes using Graphics2D
- **EdgeRealizer** - Renders connections between nodes  
- **DiagramDataModel** - Provides data structure for diagram elements
- **Graph2D/yFiles** - Underlying graph rendering engine

**Key Insight:** DataGrip diagrams use a sophisticated rendering pipeline based on yFiles. Direct node coloring requires hooking into the presentation model layer, not just overlays.

### 2. **Real-Time Modification Approaches**

#### Approach A: Overlay Rendering (Current Implementation)
**Pros:**
- Non-invasive, doesn't modify diagram internals
- Works with any diagram provider
- Fully customizable appearance

**Cons:**
- Doesn't integrate with native diagram behavior (selection, theming)
- Overlay may not cover entire canvas with scrolling
- Separate from actual diagram rendering

**Best For:**
- Temporary annotations
- Visual guides and highlights
- Post-processing visualization

#### Approach B: Presentation Model Customization (Best Practice)
**Implementation:**
```java
public class CustomPresentationModel extends BasicGraphPresentationModel {
    public NodeRealizer getNodeRealizer(NodeObject node) {
        NodeRealizer realizer = super.getNodeRealizer(node);
        Color customColor = metadata.getColor(node.getId());
        if (customColor != null) {
            realizer.setFillColor(customColor);
        }
        return realizer;
    }
}
```

**Pros:**
- Integrated with native diagram rendering
- Proper Z-ordering and layering
- Respects theme and scaling
- Better performance

**Cons:**
- Requires hooking into diagram provider
- More complex implementation
- Platform-specific APIs

#### Approach C: Database Object Coloring (DataGrip Native)
**Method:** Use DataGrip's built-in "Set Color" feature on database objects
- Available via: right-click table → Tools → Set Color
- Integrated with database tool window
- Persists with database metadata

### 3. **Current Implementation (Hybrid Approach)**

We've implemented:

1. **DiagramEditorListener** - Improved attachment logic
   - Better component traversal (scroll panes, layered panes)
   - Proper z-order management
   - Resize listener for responsive overlay

2. **OverlayPanel** - Visual containers and sticky notes
   - Graphics2D custom rendering
   - Interactive drag/resize
   - Supports transparency and styling

3. **DiagramRefreshManager** - Real-time synchronization
   - Metadata change tracking
   - Debounced refresh (prevents flicker)
   - Color resolution for elements

4. **DiagramMetadataDataModel** - Data integration
   - Wraps existing DiagramDataModel
   - Resolves colors from metadata
   - Provides color lookups for rendering

### 4. **Performance Optimization**

**Debouncing:**
```java
long MIN_REFRESH_INTERVAL = 100; // ms
// Prevents refresh spam during rapid metadata updates
```

**Lazy Rendering:**
- Only render visible containers
- Cache color lookups
- Use WeakHashMap for diagram builder references

**Component Pooling:**
- Reuse OverlayPanel instances
- Cache component hierarchy traversal results

### 5. **Troubleshooting Overlay Visibility**

If elements still don't appear, check these in order:

1. **Component Hierarchy**
   - Verify diagram editor uses JLayeredPane
   - Check if diagram uses JScrollPane
   - Ensure viewport allows overlays

2. **Z-Order Issues**
   - Set component z-order to 0 (front)
   - Use JLayeredPane.PALETTE_LAYER for layered panes
   - Bring overlay to front after each addition

3. **Bounds Verification**
   - Overlay bounds match container bounds
   - Handle viewport offset from scroll pane
   - Update bounds on component resize

4. **Visibility Settings**
   - setOpaque(false) for transparency
   - setVisible(true) explicitly
   - Call repaint() after layout changes

### 6. **Alternative: Using DataGrip's Native Coloring**

For production-quality table coloring, consider:
```
Right-click table in Database window 
→ Tools → Set Color 
→ Choose color
```

This integrates with:
- Database metadata persistence
- Theme-aware rendering
- Native diagram styling

### 7. **Future Enhancement: Presentation Model Hook**

When ready for full integration, implement:
```java
public class ColorfulDiagramProvider extends DiagramProvider {
    @Override
    public DiagramPresentationModel createPresentationModel(DiagramDataModel model) {
        return new ColorfulPresentationModel(model, metadata);
    }
}
```

Register in plugin.xml:
```xml
<extensions>
    <diagram.provider
        implementation="com.tamaygz.colorfuldiag.ColorfulDiagramProvider"
        id="database.colorfuldiag"/>
</extensions>
```

## Summary

The current implementation uses a hybrid approach:
1. **Overlay** for containers and sticky notes (visual organization)
2. **Metadata service** for tracking colors and relationships
3. **Refresh manager** for real-time synchronization
4. **Improved component attachment** for better visibility

This approach provides visual organization without requiring deep modification of DataGrip's internal diagram rendering, making it compatible with current and future DataGrip versions.

For production diagram coloring of tables themselves, the native DataGrip coloring feature or a custom DiagramProvider would be the optimal solution.
