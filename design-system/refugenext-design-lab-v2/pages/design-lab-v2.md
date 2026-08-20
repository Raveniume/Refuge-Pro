# Design Lab V2

This page is the visual gate for RefugeNext. It is a component gallery, not a production page and not a restyled copy of the old Flutter UI.

## Hierarchy

- Environment: quiet deep navy or quiet mineral light background with two low-contrast curved traces.
- Content: stable, low-alpha grouped material with thin dividers and restrained typography.
- Functional controls: sparse AndroidLiquidGlass controls with backdrop sampling, blur, vibrancy, lens, and interaction response.

## Optical Test

The gallery must show a thin curved trace crossing normal background, a centered glass lens, and normal background again. The trace must visibly bend or stretch at both lens edges; blur alone is not acceptable. The center may soften slightly.

## Controls

- Segmented outer track stays transparent and quiet.
- The selected segmented element is the primary liquid object and carries the visible refraction.
- Bottom navigation is a narrow, floating overlay. Content continues behind it.
- Search and header actions use functional glass.
- Filter and sort use compact quiet pills with the `筛选` and `排序：默认` information density.

## Content Samples

Use grouped rows, stable square thumbnails, light dividers, and a compact M80 featured row. Do not use individual heavy glass cards for content.

## Typography

Use the project PingFang font tokens. Keep titles medium-weight and reserve strong weight for values or selected control labels. Metadata is visibly quieter than content titles.

## Theme

Dark and light use the same hierarchy. Do not increase surface opacity in light mode to compensate for missing material depth; preserve translucency, edge response, and contrast through tokens.
