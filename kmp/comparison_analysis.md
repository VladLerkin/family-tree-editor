# Line Rendering Comparison: JavaFX vs KMP

## Original JavaFX Implementation (TreeRenderer.kt lines 229-278)

### Key Logic:
1. **Multiple children case** (lines 261-277):
   - Calculates `minChildX`, `maxChildX`, `minTopY` for all children
   - Sets `childBarY = minTopY - childBarGap` (8.0 * zoom above children)
   - Routes stem from parents' bar:
     - Vertical from `barMidX` to `midY` (halfway point)
     - Horizontal from `barMidX` to `childMidX` at `midY`
     - Vertical from `childMidX` to `childBarY`
     - Horizontal children bar from `minChildX` to `maxChildX` at `childBarY`
     - Vertical lines from `childBarY` down to each child top

2. **Single child case** (lines 248-260):
   - Routes toward child's center even if offset
   - Vertical from `barMidX` to `midY`
   - Horizontal from `barMidX` to child center `cx` at `midY`
   - Vertical from `cx` to `childBarY`
   - Vertical from `childBarY` to child top

### Critical Features:
- Uses a **children bar** (`childBarY`) that spans horizontally across all children
- Has intermediate routing level (`midY`) for smooth transitions
- Gap above children: `8.0 * zoom`
- Line width: `1.0 * zoom`

## Current KMP Implementation (TreeRenderer.kt lines 243-287)

### Key Logic:
1. **Both parents case** (lines 247-269):
   - Creates marriage bar between parents
   - For each child individually:
     - Sets `topY = c.top - 6f / scale` ⚠️ WRONG: dividing by scale
     - Draws three segments:
       1. Vertical from `mid` to `Offset(mid.x, topY)`
       2. Horizontal from `Offset(mid.x, topY)` to `Offset(c.centerX, topY)`
       3. Vertical from `Offset(c.centerX, topY)` to `Offset(c.centerX, c.top)`
   - strokeWidth: `2f * lineWidth * scale` ⚠️ INCONSISTENT with gap calculation

2. **Single parent case** (lines 270-284):
   - Similar logic with same issues

### IDENTIFIED PROBLEMS:

#### Problem 1: Incorrect Gap Calculation
```kotlin
val topY = c.top - 6f / scale  // WRONG!
```
Should be:
```kotlin
val topY = c.top - 6f * scale  // Correct: multiply by scale
```
OR (to match JavaFX 8.0*zoom):
```kotlin
val topY = c.top - 8f * scale
```

#### Problem 2: No Children Bar for Multiple Children
The KMP version routes **each child individually** from the marriage bar midpoint.
The JavaFX version creates a **horizontal children bar** that connects all children first.

This means:
- KMP: Marriage bar → individual routes to each child (star pattern)
- JavaFX: Marriage bar → vertical stem → horizontal children bar → verticals to each child (bus pattern)

#### Problem 3: No Intermediate Routing Level (midY)
JavaFX uses `midY = (barY + childBarY) / 2.0` for smooth orthogonal routing.
KMP routes directly from marriage bar to child level.

## Recommended Fixes:

### Fix 1: Correct the gap calculation (CRITICAL)
Change `6f / scale` to `6f * scale` or `8f * scale` (to match JavaFX 8.0*zoom)

### Fix 2: Implement proper children bar routing (IMPORTANT)
For multiple children, the KMP version should:
1. Calculate the span of all children (minChildX, maxChildX, minTopY)
2. Draw a horizontal children bar above them
3. Route from marriage bar midpoint to children bar center
4. Draw vertical lines from children bar to each child

### Fix 3: Add intermediate routing level (OPTIONAL but cleaner)
Use `midY` for smoother orthogonal transitions between parent bar and children bar.
