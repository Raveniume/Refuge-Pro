# Functional Parity Repair — 2026-08-23

This matrix is evidence-driven. Historical migration documents are not accepted
as runtime proof. A row remains **FAIL** until the original Flutter behavior,
Compose data path, and emulator evidence have all been checked.

| Issue | Original source of truth | Current Compose implementation | Root cause | Fix | Runtime evidence | PASS / FAIL |
| --- | --- | --- | --- | --- | --- | --- |
| 1 Buyback completeness | `lib/src/repo/buyback.dart`, `lib/src/network/parsers/buyback_parser.dart`, `lib/src/widgets/hangar_buyback/` | `RsiLiveBuybackRepository` | Single-page parsing truncated the account | 1-based 100-row pagination, persistent snapshot, raw/parsed/repository/group audit counts | Current-run emulator proof pending | FAIL |
| 2 Featured ship filtering | Hangar model, parser, upgrade log/chain and package contents | Typed `includedEntries` / `containedShip` projection | Resolved CCU destinations were incorrectly treated as owned ships | Require a non-Upgrade pledge with a typed ship child; emit aggregate candidate audit | 2026-08-24 Pixel 7 API 35: `inventory=75 featured=2 ships=M80, Perseus acceptedUpgradeResults=0`; vertical-drag runtime recheck pending after gesture correction | FAIL |
| 3 Inventory date placement | Approved Hangar inventory layout and legacy locale formatting | `HangarInventoryRow` | Text column did not share the image's bottom edge | 88dp image and 88dp text column; price/date/actions share the bottom row | 2026-08-24 1080x2400 capture `qa/runtime_after_swipe.png`: price, localized date and action icons share the image-bottom information row | PASS |
| 4 Hangar detail parity | `lib/src/widgets/hangar/hangar_item_detail_widget.dart` | `HangarDetailSheet` | Reduced detail projection omitted package and upgrade metadata | One scroll container with 21sp bilingual hero, 20sp values, 16sp content rows, metadata and fixed guarded actions | 2026-08-24 Pixel 7 API 35 small-item capture `qa/runtime_detail_typography_final.png`; ordinary Package and owned-Upgrade runtime sweep still pending | FAIL |
| 5 Bottom nav icon sizing | Original icons and accepted AndroidLiquidGlass demo tabs | Persistent `OfficialLiquidBottomTabsPort` | Per-route nav recreation reset lens motion; idle terminal glyph read larger; the root bar still exposed Tools during the migration | One root-owned official Liquid Glass nav with exactly four root tabs (`机库`, `商店`, `终端`, `我的`); Tools remains a Profile utility route; selected glyphs are optically larger than idle glyphs | `qa/nav-four-current.png` + `qa/nav-four-current.xml` and `qa/nav-four-profile.png` + `qa/nav-four-profile.xml` show only the four root labels and stable selection bounds | PASS |
| 6 Translation parity | `lib/src/repo/translation.dart`, `game_item_translation.dart` | `ProductionTranslationRepository` | Translation tables were not loaded | Original translation tables, correction map and English fallback; lazy IO-safe parse | Current-run translated UI screenshots pending | FAIL |
| 7 Terminal images | Original database models/API image fields | `WikiTerminalRepository` -> Coil | Image URL mapping/cache was incomplete and route-scoped refresh cancellation prevented the first snapshot | Preserve source image URLs; repository-owned prewarm/refresh job writes the full snapshot even after route exit; placeholder only when source has none | 2026-08-24 Pixel 7 API 35 `qa/runtime_terminal_cached_final.png`; `terminal_items.json` contains 1,256 rows / 884,527 bytes and opens cache-first; five-per-category image sweep pending | FAIL |
| 8 Real profile data | RSI account/billing/ledger and original account model | `RsiLiveProfileRepository` | Credit minor units and unapplied CCU destination MSRP were summed incorrectly | Credit uses cents; top hangar value sums paid/melt values; lower current value sums package ship MSRP + CCU deltas + other paid values | 2026-08-24 authenticated log/capture: spent `$453.00`, hangar `$896.25`, credit `$1.50`, current `$2687.50`, 75 real items and 2 owned ships | PASS |
| 9 Profile/tools parity | `lib/src/widgets/utility/`, referral and account routes | `RsiLiveUtilityRepository`, terminal routes, safe external intents | Static descriptions replaced several usable flows | Live crowdfunding/player/referral/social/promotion reads, terminal routes, and guarded gift boundary | Entry-by-entry runtime sweep pending | FAIL |
| 10 Real theme switching | Original settings persistence and app theme state | Shared preferences-backed root theme | Full-tree transition caused stalls and old toggle state could diverge | One persisted theme state plus lightweight 240ms tint reveal | Switch/return/restart recording pending | FAIL |
| 11 Settings test-center removed | `lib/src/widgets/settings/settings_page.dart` plus migration requirement | Compose settings | Obsolete production entry | Entry removed from Settings | Current-run Settings screenshot pending | FAIL |
| 12 Avatar center-crop | `status_avatar.dart` | Coil/Image `ContentScale.Crop` + `CircleShape` | An extra 1.9x graphics transform clipped the remote avatar | True circular crop without post-clip scaling in every header/profile surface | 2026-08-24 authenticated Hangar/Profile/Store captures show the complete avatar inside the circular crop | PASS |
| 13 Avatar status switching | `status_avatar.dart`, shared app state/local mode | Persisted `UserStatusSource` | Per-screen state drift | One presence source shared by all headers/profile; immediate color change | Cross-page runtime recording pending | FAIL |
| 14 Upgrade flows separated | Store purchase widget, owned CCU sheet, local planner | `StoreUpgradePurchaseScreen`, `HangarOwnedCcuApplyScreen`, `CcuScreen` | One generic route mixed three business meanings; planner subtracted every owned CCU regardless of connectivity | Separate route/state/UI; local fromShip-toShip graph uses only connected strictly increasing MSRP edges and exposes the selected route; all mutations guarded | 2026-08-24 local-planner runtime `qa/runtime_upgrade_route_final.png` plus connected/unrelated/reverse-edge unit tests; store-purchase and apply-owned runtime recording pending | FAIL |

## Safety gate

`SafeMutationGuard` must block final mutations for Gift, Reclaim, Buy CCU,
Apply owned CCU, cart checkout, and any other real account-changing request.
Runtime QA stops before the mutation boundary.

## 2026-08-24 cache/performance delta

- Hangar, buyback, hangar logs, store, terminal, profile and CCU now publish a persistent snapshot before starting their network refresh.
- Root navigation is the frozen AndroidLiquidGlass port and remains composed while destinations change; Tools moved under Profile so the root bar contains four tabs.
- M80 and Perseus use a stacked, vertically draggable pair of quiet real-refraction Liquid Glass cards.
- Store and Terminal use horizontally scrollable moving-lens segmented bars and quiet Liquid Glass list rows.
- Runtime evidence is collected on `RefugePro_Pixel7_API35` at 1080x2400. Rows without direct current-run evidence remain FAIL.

## 2026-08-25 non-computer validation

- `.\\gradlew.bat testDebugUnitTest lintDebug assembleDebug --rerun-tasks --console=plain`: **BUILD SUCCESSFUL**; 51 actionable tasks executed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `4D1A40521A4C4AAE379A8EC6B6455E480487C916455E51626E93FA32DB66E704`.
- `qa/nav-four-current.xml`, `qa/nav-four-profile.xml`, and `qa/nav-four-upgrade.xml` contain the four root labels (`机库`, `商店`, `终端`, `我的`) and no root `工具` item. The `工具` route remains available from Profile's utility section.
- Android SDK CLI Emulator was attempted without `computer-use` (`-accel off`, SwiftShader). It reached `emulator-5554` but remained `offline`; no APK install or new runtime screenshot is claimed from this attempt. Existing rows without direct current-run evidence remain **FAIL**.

## 2026-08-25 shared-backdrop performance pass

- Hangar inventory, buyback, Store, and Terminal now virtualize at an eight-row group boundary. Each group performs one shared Liquid Glass backdrop capture; its rows retain their existing data, image, detail route, and stable group key without creating per-row lenses.
- Eight rows bound the group height to 896dp or less, avoiding oversized high-density RenderTargets while still reducing the number of visible backdrop captures by up to 8x.
- Row press feedback is a 160ms draw-only overlay. Its animated alpha is read in the draw phase so the press animation does not recompose the row content.
- Static `RefugeLightweightGlassSurface` instances no longer allocate a coroutine scope, `ReferenceInteractiveHighlight`, gesture detector, or `MutableInteractionSource`. Clickable instances preserve the existing highlight and semantics path.
- `.\\gradlew.bat testDebugUnitTest lintDebug assembleDebug --rerun-tasks --console=plain` passes after the refactor with all 51 tasks executed; 17 unit tests pass with zero failures/errors. Debug APK SHA-256: `E51FAD3E86E19C4BD829160AC175F1BC274AA6837EB41C480393802B785A65AC`.
- Runtime frame timing and screenshot comparison remain pending because the CLI emulator is still unavailable; performance issue 21 remains **FAIL**, not an inferred PASS.

## 2026-08-25 eight-item visual repair acceptance

Runtime checks were performed without `computer-use` on `emulator-5554`
(`RefugePro_Pixel7_API35`, 1080x2400, 420 dpi). Account-changing actions were
not invoked and `SafeMutationGuard` remains in place.

| Requested repair | Implementation and current-run evidence | Result |
| --- | --- | --- |
| 1 Hangar top spacing | The header/search, selector, featured stack, and list header use explicit 20dp section spacing instead of a global spaced arrangement. `qa/runtime_hangar_final_top.png` shows the corrected rhythm without the collapsed-search phantom gap. | PASS |
| 2 Connected inventory list | Eight-row render groups share one continuous surface; only the first and final groups retain outer rounding. `qa/runtime_hangar_group_boundary_final.png` shows no gap or duplicate rounding at the group boundary. | PASS |
| 3 Featured stack clipping/fade | The vertical stack clips to its fixed bounds and interpolates both active and queued card alpha. The in-gesture frame `qa/runtime_ship_stack_drag_mid2.png` shows the farther card faded while both cards remain above the list header; `qa/runtime_ship_stack_drag_end2.png` shows the settled Perseus card. | PASS |
| 4 Detail toolbar side-circle position | The 144dp center capsule is flanked by equal weighted regions, with each 48dp side action centered between the capsule and screen edge. `qa/runtime_detail_polish.png` confirms the final placement. | PASS |
| 5 Hangar log icon and height | Both item and global logs use the log glyph and a 780dp sheet-height cap, matching the detail maximum. `qa/runtime_hangar_overflow_final4.png` confirms the global log icon and bounded sheet; `qa/runtime_log_polish.png` covers the item log. | PASS |
| 6 Inventory action alignment | Each row reserves a fixed 36dp information-line height and a fixed 108dp three-action region. `qa/runtime_hangar_final_top.png` shows price, date, tag, reclaim, and chevron aligned independently of title length. | PASS |
| 7 Store/detail/navbar icon weight | Store upgrade now uses the same outlined upgrade glyph as detail; detail actions are 18dp and selected navbar icons use outlined variants at 19-19.5dp. `qa/runtime_store_icon_top_final.png` and `qa/runtime_detail_polish.png` confirm the shared style and reduced weight. | PASS |
| 8 Profile stats and utility height | Consumption/melt, hangar value, and credit render in one row; utility cards use a 68dp minimum height in the two-column layout. `qa/runtime_profile_polish.png` confirms both changes with authenticated cached values. | PASS |

Final verification: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` passed
with all 51 tasks executed; 17 unit tests passed with zero failures, errors, or
skips. Debug APK SHA-256:
`665552D9C46B52100A3A46B7EC61C2153DECB6E9D2ECB73DECA4B04F6C6A4BCB`.

## 2026-08-27 follow-up

- Reworked the featured-ship stack resting geometry: the queued card has a
  stable 76dp reveal above the active card, while the existing vertical drag
  and distance fade remain intact. This keeps both eligible owned ships
  visible without hardcoding their names.
- Reduced the detail sheet's transparent action-area scrim and modal surface
  tail so the five floating detail actions no longer sit on a rectangular
  dark band.
- Re-ran `.\\gradlew.bat testDebugUnitTest lintDebug assembleDebug
  --rerun-tasks --console=plain`: all 51 tasks completed successfully and 17
  unit tests passed.
- Current debug APK SHA-256 after the cache-mode default fix:
  `2EC68DBF17306D90FB916E40FDC9D3B565473B3DB7E38B5908377D8DE71431A9`.
  The same APK is copied to the desktop as
  `RefugeNext-functional-parity-20260827.apk`.
- Cache-first/background refresh is now the default for fresh installs;
  local-only mode remains an explicit setting.
- This host currently has no Android SDK `adb.exe` or `emulator.exe` on disk,
  so these two changes still need a fresh Pixel 7 interaction pass before
  their runtime rows can be promoted from static evidence to PASS.
