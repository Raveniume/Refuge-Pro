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

- `CachedHangarRepository` is a read-only cache boundary; its local snapshot is
  tracked in `PREVIEW_DATA_AUDIT.md` until a parser-backed cache is connected.
- `HangarScreen` is the Hangar Golden Master: it preserves the existing
  inventory information order while using the V4 Compose components and
  tokens. The page includes the real local avatar and M80 image assets;
  network/cache-backed data remains behind `HangarRepository`.
- Heavy Liquid Glass is limited to segmented navigation, controls, bottom
  navigation, and transient actions. Inventory and hero surfaces use lightweight
  content material.
- Modal bases are opaque material surfaces with a platform scrim. Their
  controls may use Liquid Glass without sampling the app root as modal content.
- `DampedDragAnimation` drives bottom-tab tap, drag, release, and fast-tap
  continuity. The visible selection slider is rendered above the tab icons.
- Production background uses a restrained deep navy/blue-black field with
  low-frequency light and texture; the Reference Lab optical test is not used
  as the Hangar background.

## Deferred adapter work

The V4 Reference Replication Lab is accepted as the implementation baseline.
Apple component-level geometry remains unverified and is explicitly not a
blocker. The Hangar Golden Master is accepted and all remaining production
routes are tracked in `FULL_MIGRATION_MATRIX.md`.

The next adapter step is to replace the local cache snapshots with parser-backed
legacy cache/API mappers. They must preserve filtering, stacking, translation,
pricing, and image identity without changing the approved Hangar geometry.
