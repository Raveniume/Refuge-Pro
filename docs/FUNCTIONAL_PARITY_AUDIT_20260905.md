# Functional Comparison - 2026-09-05

This is the earlier comparison snapshot. Subsequent fixes and the user's later
29-item acceptance list are tracked in
[Extended Functional Parity Repair](EXTENDED_FUNCTIONAL_PARITY_AUDIT_20260905.md).
Use that report for the current build and remaining failures.

## Conclusion

The tested Compose browsing flows work, but full functional parity with Flutter
4.5.6 is **not established**. Existing migration differences affect search,
sorting, filters, settings, and the cart/transaction boundary. Passing material
or fixture tests does not establish authenticated end-to-end parity.

This follow-up adds tests and this report. It does not change production code.
The previous material changes remain installed for manual inspection.

## Baselines and Scope

- Flutter application installed beside Compose:
  `../RefugeNext-deliverables/RefugeNext-Pro-v4.5.6-spatial-glass-20260812.apk`.
  Package `vip.kirakira.starcitizenlite.pro`, version `4.5.6-pro`, code `442`.
  SHA-256: `32ED6DA6D2E2254494E47E445546235D51CE4333F90626F2D4E2076558D6AA08`.
  It launches the full application's login welcome page. This is a local Pro
  build, not proof of an untouched upstream release binary.
- Excluded `../RefugeNext-main/release/RefugeNext-4.5.6.apk`: despite its name,
  it launches Reference Optical Material Lab. Its SHA-256 is
  `420D23006E65EA0844F6F4FF8F82FB0E84E65E2FA03AD532DDC47DFBDA001214`.
- Flutter logic tests execute the existing `RefugeNext-main` functions, not a
  rewritten equivalent. `lib/src/funcs/search.dart` and
  `lib/src/widgets/shop/shop_page/shop_page.dart` match the August 19 backup
  byte-for-byte. Their SHA-256 values are respectively
  `70BF11AC907313E7D9F43F355272AFD6D1A6C0DEC73323CE3DF00BB3E89D1BB5` and
  `46ADF4EB52805C2A7073AF265926955FDD4FA16343D4DAA39ADB9E263FEBA078`.
  The broader Flutter worktree has existing changes; exact source-to-release
  binary equivalence was not established.
- Current Compose debug APK SHA-256:
  `72F06F3660BDE7DBFD8E6C691674C86A69C79C2544624A1B5957F40FA52D1B3C`.
- Emulator: `RefugePro_Pixel7_API35`, `emulator-5554`, 1080x2400, host GPU.
  This follow-up used Gradle, Flutter test, ADB and Android instrumentation.
  No computer-use calls were made during this follow-up.
- No authenticated Flutter login or same-account two-app browsing comparison
  was completed. No passwords or session tokens are embedded in these tests.
- Per the user's request, no purchase, buyback, upgrade, apply-CCU, gift,
  reclaim, or other real asset-changing action was invoked. Cart tests use
  `InMemoryCartRepository` fixtures only. Settings persistence uses the test
  APK's context, not the user's installed application preferences.

## Verified Tests

| Verification | Result | Scope |
| --- | --- | --- |
| Compose unit suite | 42 passed, 0 failed/skipped | Existing data, refresh, settings migration and presentation tests |
| New Compose instrumentation | 6 passed | Actual production composables with local fixtures, detailed below |
| Flutter logic contract tests | 5 passed | Actual legacy search/filter/sort functions on local fixtures |
| Debug application/test APK build | Passed | APKs compiled and installed using `adb install -r` |
| Android Lint | Passed | 38 warnings and one hint, no errors |
| Earlier material instrumentation | 4 passed in preceding material pass | Not rerun or counted as new functionality tests |

New instrumentation:
`app/src/androidTest/java/com/refuge/next/material/RefugeFunctionalRegressionTest.kt`.

1. Store: case-insensitive manufacturer search, empty-result handling, search
   clearing, all seven catalogue categories including horizontally hidden tabs.
2. Store: descending result order, price filtering, Warbond filtering, and an
   empty intersection of two filters.
3. Local cart: repeated add, quantity feedback, $60.00 and $105.00 totals,
   decrement, removing the last quantity, clear, and empty-cart dismissal.
4. Store: initial repository failure followed by a successful retry without
   restarting the screen.
5. Hangar: original-name search, combined ship/giftable filters, clearing
   filters, date ordering, and preservation of the source inventory.
6. Settings: theme label/toggle behavior, persistence through a new repository
   instance, clear callback, about-sheet dismissal, presence selection callback,
   and navigation callback. This is not a full app process-restart test.

The first instrumentation run passed four tests. Two assertions were corrected:
the empty-result selector matched both input and message, and totals were
expected without the actual two decimal places. Both corrected tests then
passed in a focused rerun. No product defect was hidden by those corrections.

Flutter tests: `qa/flutter_store_contract_test.dart`. They verify preserved
default order, explicit price sorting in both directions, whitespace/case and
description search, exact price-band boundaries, and filter intersection.

## Comparison Findings

| Area | Flutter behavior | Current Compose behavior | Assessment |
| --- | --- | --- | --- |
| Search | Trims surrounding whitespace; searches name/title/subtitle and description fields | Store matches title/metadata without trimming; Hangar matches title/original name without trimming | Partial parity. A query such as `  Anvil  ` or description-only terms can lose matches |
| Store default sort | Keeps catalogue order unless explicit sorting is selected | The `default` choice always sorts by ascending price | Confirmed behavioral difference |
| Store sort choices | Default, ascending, descending | Default (actually ascending), descending | Missing a separate default/ascending distinction |
| Store price bands | 0-100, 100-500, 500+; subsequent lower bounds are exclusive | 0-50, 50-150, 150+; boundary values 50 and 150 overlap adjacent bands | Confirmed range and boundary differences |
| Store filters | Warbond and a clear control | Warbond works; reset requires changing individual choices | Current controls tested, feature set reduced |
| Hangar filters | Type, status, insurance, price, reclaimability, text and source/destination ship conditions; price ordering | Ship/reclaimable/giftable, title/original-name search and date ordering | Partial parity; new fixture test only covers available options |
| Cart | Remote RSI cart, editable quantities, checkout flow | Local in-memory selections, decrement and clear; no persistence across process loss | Local flow passes; remote/persistent parity absent |
| Account mutations | Real transaction integrations exist | `SafeMutationGuard` blocks the final mutation request | Existing intentional migration boundary; excluded from runtime testing |
| Theme/presence | App-level appearance and status settings | Theme toggle and repository persistence work in isolated tests; presence picker updates its callback | Partial verification; root restart/cross-page propagation not verified |
| Other settings | Includes theme customization, translation and refresh controls, logs, source/license links | Only dark theme, cache marker, data version, version/about and license rows; data/license callbacks are empty | Reduced functionality; platform-specific desktop options are not counted as Android gaps |
| Cache clearing | No equivalence assumed | `clearLocalCache()` only timestamps a preference; it does not evict images or refresh repository snapshots | The control must not be treated as a functional cache reset |
| Navigation state | Search state is held by the shared Flutter model | Screen filters use `remember`; the state holder is inside route-scoped content | Source-level concern; tab-return/process restoration needs a dedicated runtime check |

Relevant source references (paths relative to the workspace):

- Flutter search/sort: `RefugeNext-main/lib/src/funcs/search.dart:239`, `:459`,
  `:495`; filter/sort UI: `lib/src/widgets/shop/shop_page/shop_page.dart:153`.
- Flutter remote cart: `RefugeNext-main/lib/src/funcs/shop/cart.dart:33`,
  `lib/src/widgets/shop/shop_page/shop_checkout_bottomsheet.dart:102`.
- Compose search/filter/sort: `app/src/main/java/com/refuge/next/screens/StoreScreen.kt:134`
  and `HangarScreen.kt:201`.
- Compose cart: `app/src/main/java/com/refuge/next/data/StoreRepository.kt:67`;
  guard: `data/ProductionData.kt:405`.
- Compose settings: `screens/ProductionScreens.kt:1078`,
  `data/ProductionState.kt:29`, `:70`; navigation: `RefugeApp.kt:381`.

AI and RefugeNext paid membership features are intentionally excluded according
to `REMOVED_FEATURES_AUDIT.md`; their absence is not reported as a regression.

## Reproduction and Manual Handoff

From `RefugeNext-Compose`, with the configured JDK 17 and Android SDK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.refuge.next.material.RefugeFunctionalRegressionTest com.refuge.next.compose.test/androidx.test.runner.AndroidJUnitRunner
```

From `RefugeNext-main`:

```powershell
..\toolchain\flutter\bin\flutter.bat test --no-pub ..\RefugeNext-Compose\qa\flutter_store_contract_test.dart --reporter expanded
```

The visible emulator is left running the latest Compose `MainActivity` on the
Hangar screen, showing the retained 75-item inventory. The full Flutter app
remains installed separately. Final Android screen capture:
`qa/compose-manual-20260905.png`.

This handoff cold launch reported 7,538 ms through `am start -W`. It is one debug
emulator startup sample, not a benchmark. Smoothness is still unproven; the
earlier frame-time limitations remain in `MATERIAL_LAYER_AUDIT_20260905.md`.

Remaining acceptance work: authenticated same-account browsing/data comparison,
root navigation/state restoration, release/device performance, and the asset
operations reserved for the user's own manual testing.
