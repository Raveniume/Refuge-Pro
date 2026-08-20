# RefugeNext Compose Slice 1

This project is an isolated Android presentation layer. `RefugeNext-main` and
`RefugeNext-main-backup-20260819` are reference-only; no production Flutter UI
is imported into this project.

## Audited business boundaries

| Existing contract | Reference location | Compose boundary |
| --- | --- | --- |
| Shared app state and auth/session lifecycle | `lib/src/datasource/data_model.dart` | Future `RefugeSessionDataSource` adapter |
| RSI API and presence/session calls | `lib/src/network/` and `lib/src/services/spectrum_ws_service.dart` | Future Kotlin API/auth adapter |
| Hangar persistence and refresh | `lib/src/repo/hangar.dart`, `lib/src/repo/base/` | `HangarRepository` |
| Hangar parsing and item contract | `lib/src/network/parsers/hangar_parser.dart`, `lib/src/datasource/models/` | Immutable `HangarItem` mapping |
| Buyback and upgrade/CCU behavior | `lib/src/repo/buyback.dart`, `lib/src/repo/ccu_planner.dart` | Deferred until Slice 5 |
| Images and cache loading | Flutter asset/network image loaders | Image repository/cache adapter, not a UI concern |

## Slice 1 status

- `PreviewHangarRepository` is intentionally read-only sample data.
- `HangarScreen` freezes the existing inventory information order while using
  new Compose components and tokens.
- Heavy Liquid Glass is limited to segmented navigation, controls, bottom
  navigation, and transient actions. Inventory and hero surfaces use lightweight
  content material.
- Modal bases are opaque material surfaces with a platform scrim. Their
  controls may use Liquid Glass without sampling the app root as modal content.
- `DampedDragAnimation` drives bottom-tab tap, drag, release, and fast-tap
  continuity. The visible selection slider is rendered above the tab icons.

## Next adapter work

This work is gated on user confirmation of the Reference Replication Lab. The V4 report records AndroidLiquidGlass behavior as review-ready, but Apple component-level geometry remains unverified; do not begin this adapter or any Hangar migration yet.

The next implementation step is a Kotlin read-only adapter for the existing
hangar/cache contract. It must preserve filtering, stacking, translation,
pricing, and image identity before Store or Terminal migration begins.
