# Extended Functional Parity Repair

Source: the user's later 29-item numbered list in attachment
`dab3d5ae-9138-4a27-b1b2-99d13f3461d1/pasted-text.txt`, combined with the earlier
28-section request and follow-up requirements in task `01a03295-e0a4-7ef3-975d-1dce81e748cd`.
This is a working acceptance record, not a claim that the entire migration passed.

The working tree already contained substantial changes. This continuation fixes
observed regressions in that tree without reverting its prior work.
No Computer Use, account mutations, checkout, gift, recall, reclaim, or upgrade
application is permitted in QA. Feature tests use isolated fixture repositories;
root-navigation, theme and gesture tests also use the retained account session
for read-only browsing and local settings.

## Acceptance Tracking

FAIL includes missing current runtime evidence. Entries are promoted only after
implementation and corresponding runtime checks. Screenshots are in
`qa/extended-20260905/`; older screenshots do not prove this build passed.
Current result: 28 PASS, 1 FAIL (performance, item 21). This is not a release
approval or a declaration of complete Flutter functional parity.

| # | Requirement | Status | Evidence / Remaining Work |
| --- | --- | --- | --- |
| 1 | Detail toolbar safe area | PASS | Runtime bounds and detail screenshot; 48dp controls clear navigation inset |
| 2 | M80 Hero real glass | PASS | Fixed opaque compatibility surface; actual hero responds to changing underlying pixels |
| 3 | Upgrade selector readability | PASS | Target, SKU and source screenshots reviewed; unavailable SKU disabled, Warbond differentiated |
| 4 | Owned ships excluded from targets | PASS | Stable ship IDs persist into owned ships and both planners; localized alias regression and purchase selector tests pass |
| 5 | Modified Refuge icons restored | PASS | Compared modified Flutter HangarActionIcons with current toolbar; jump now opens the item's RSI page |
| 6 | Upgrade route localization/alignment | PASS | Translated target/source display, Chinese labels, equal-size centered arrows; raw identity retained |
| 7 | Tools two-column layout | PASS | Equal-width columns and aligned bounds verified in production composable |
| 8 | Detail action vertical placement | PASS | Explicit navigation inset plus 18dp action bottom padding; current screenshot reviewed |
| 9 | Item-scoped Hangar Log | PASS | Fixture with another item's log proves pledge-ID filtering; localized timeline screenshot |
| 10 | Log minimum sheet height | PASS | Single-entry runtime sheet at least 520dp, larger histories scroll |
| 11 | Detail field parity | PASS | Dates, insurance, status, pledge ID/page, included items, value and upgrade endpoint fields rendered and checked |
| 12 | Center action group compact width | PASS | Runtime 144dp group with three 48dp controls and separate side controls |
| 13 | Upgrade eligibility parity | PASS | CCU/ordinary pledge enabled states; RSI chooseUpgradeTarget IDs replace name guessing; authoritative fixture and retry pass |
| 14 | Gift/Recall logic parity | PASS | Gifted/locked state paths, recipient validation, password requirement, correct pledge/recipient confirmation and edit retention passed. Flutter giftPledge/cancelGift field contracts pass. Scope ends before mutation |
| 15 | Segmented white-line artifact | PASS | compact-nav-optical.mp4: tap, drag/release and fast taps; native captured frames reviewed in compact-motion-native.png without interior white seam |
| 16 | Bottom nav optical icon sizing | PASS | Per-path optical correction and fixed slots; all four selected states in nav-optical-final.png and compact-nav-final.mp4 reviewed; labels share y=2248 in final ADB hierarchy |
| 17 | Terminal detail parity/readability | PASS | All eight category detail screens checked for description, prices and retained specification fields |
| 18 | Profile tools/statistics hierarchy | PASS | Spending/hangar/credit remain statistics; tools form a separate two-column section |
| 19 | Real Light/Dark switching + persistence | PASS | Actual root test plus ADB cold-process restart recording; final mode returned to Light |
| 20 | Settings switches functional audit | PASS | Actual root theme toggle and cache-completion tests; local image caches clear and login survives; version/license actions open sheets |
| 21 | Liquid Glass performance optimization | FAIL | Identical-pixel A/B did not show reliable improvement; experimental layer is disabled by default; details below |
| 22 | Long mode selector idle state | PASS | Actual shared selector fixture; current plus two nearby labels |
| 23 | Selector press expansion | PASS | Touch down increases measured strip width; screenshot captured |
| 24 | Selector drag | PASS | Actual touch movement changes selected category after release |
| 25 | Selector release snap | PASS | Nearest category springs into the lens |
| 26 | Selector collapse | PASS | State and width return after settling; vertical cancellation covered |
| 27 | Edge mode geometry | PASS | First and last lens align with selected label inside track bounds |
| 28 | Fast-tap continuous animation | PASS | Interruptible spring retains velocity; rapid touch sequence settles correctly |
| 29 | Shared Liquid Mode Selector | PASS | Store and Terminal production screens use shared component; Store category runtime test passes |

## Additional Regressions Fixed

- Store default order now preserves the incoming catalogue, and both explicit
  price sort directions are available.
- Price bands match the Flutter baseline: 0-100 inclusive, above 100 through 500,
  and above 500. Search trims whitespace and includes descriptions.
- Page state holder survives route transitions; category, search, filter and sort
  state use saved state. Real account Store -> Terminal -> Store test retains
  the Paints category and M80 search.
- The inventory detail arrow now invokes the actual open callback.
- Chinese ship identities no longer normalize to an empty alias and collide.
- Cache clearing removes actual Coil image memory/disk caches on IO, reporting
  completion or failure. Account settings and hangar snapshots are retained.
- Store now displays native USD prices like the modified Flutter app. A public
  RSI response returned Avenger Stalker nativePrice=6000 and price=6212; the old
  code displayed $62.12 as USD. Current live screenshot displays $60, alongside
  Gladius $90 and Arrow $75. The price cache has a new version to avoid retaining
  incorrectly denominated prices. Category subtitles are translated too.
- Owned CCU application now reads RSI's chooseUpgradeTarget response and parses
  the returned pledge IDs with Jsoup. A same-name but unapproved pledge is not
  eligible, while an approved translated name is eligible. Errors can retry.
- An owned-target/catalog refresh invalidates stale selected upgrade targets or
  SKUs. Inventory gift/reclaim arrows now open the relevant item flow.
- Gift now preserves the actual recipient and pledge in a request matching
  Flutter's giftPledge contract, including current_password. It displays a
  separate confirmation with recipient, pledge ID and quantity before the
  debug guard. Recall uses cancelGift with pledge_id and its own action type.
  Forms validate eligibility and required fields; returning from confirmation
  retains edits. Passwords are memory-only and redacted from request logging.
  Compose inventory rows are individual pledge IDs, so quantity here is one.
- Navigation glyphs use per-path optical correction and fixed 24dp slots so
  selecting a tab does not shift the caption baseline. Terminal was reduced
  further after reviewing the captured images; Profile was enlarged.

## Verification

- JVM suite: 49 tests passed, including identity, RSI target HTML parsing,
  Flutter gift/recall request fields, validation and mutation guard checks.
- Full instrumentation run: 17 passed before the final identity/eligibility
  follow-up. Extended suite then passed all 8 tests, including list action routes
  and authoritative upgrade eligibility/retry. The native-price assertion passed
  in a targeted rerun.
- Root navigation/search retention passed. Root theme, activity recreation,
  cache completion and retained login passed separately. Cold process restart
  was also captured via ADB in theme-persistence.mp4.
- Final combined acceptance run passed all 12 tests after the gift and icon
  changes (instrumentation-acceptance-final.txt). It includes eight extended
  functional cases, three material pixel cases and the authenticated gesture
  workflow. That gesture workflow was also recorded on the current APK in
  compact-nav-final.mp4; its recording run passed in 44.885 seconds.
- The earlier gift follow-up failed only its final edit-retention assertion:
  Compose merged the field label with EditableText, so assertTextEquals also
  saw the label. The retained email was correct; assertTextContains now tests
  it correctly. The subsequent complete suite passed. The initial screenshot
  also exposed an input-method overlay, fixed by clearing focus and hiding the
  keyboard before the confirmation page.
- Hero vertical drag makes Perseus active without opening a detail dialog.
  Final MainActivity is open in the visible emulator, Light theme, 75 real
  inventory items, retained login. manual-final.png/xml capture that state;
  the current process has no AndroidRuntime fatal errors. Its cold launch
  still took 9.191 seconds under host contention.
- Build and Android lint passed. No account mutation was sent.
- A newly tried child drawing cache failed the live-modal sampling test and was
  rejected. Moving its layer outside the capture passed sampling, but did not
  establish performance benefit. That extra modifier has now been removed.
  A subsequent diagnostic switch rasterizes the existing backdrop RenderNode;
  this is also disabled in production after the inconclusive A/B below.
- An obsolete instrumentation run was deliberately force-stopped after detecting
  the sampling regression; its reported process exit is not an unexplained app
  crash and is not counted as a passing suite.

## Performance Evidence

Official reference reviewed: AndroidLiquidGlass LayerBackdropModifier,
LayerBackdrop, CombinedBackdrop, BackdropEffectScope and RuntimeShaderCache.
Shader objects are already reused in the dependency. Production retains one
shared page capture and ordinary lightweight list rows. Refraction/blur settings
were unchanged in the A/B workload.

Debug API35 emulator, RTX 4070 host GPU, 60 local-image Store rows, eight native
swipes after warmup per run. Host load was not isolated; DSPGAME was active.

| Run | Additional Page Layer | Median Frame | P95 Frame | Median Draw | Median GPU |
| --- | --- | ---: | ---: | ---: | ---: |
| 0 | Off | 215.51ms | 307.82ms | 0.74ms | 61.29ms |
| 1 | On | 274.73ms | 454.87ms | 0.78ms | 77.16ms |
| 2 | Off | 302.99ms | 546.02ms | 0.69ms | 88.30ms |
| 3 | On | 377.29ms | 612.86ms | 0.65ms | 111.08ms |

All four PNGs are byte-identical, SHA-256
`5B13500E73886AEB58F7EF871338E62921B0CEFA2E52BAEBAD2DC56553D0C881`.
There is no basis to claim a speed-up or 60Hz smoothness. GPU time and scheduling
dominate these samples; host contention cannot establish device performance.
Cold-start samples varied from roughly 6 to 9.5 seconds. Performance acceptance
needs a quiet host and a profileable release/physical-device run.

The subsequent RenderNode rasterization experiment retained the same screenshot
hash in all four runs and all optical parameters. It also failed to establish
an improvement in total frame duration:

| Run | Rasterize Page Backdrop | Median Frame | P95 Frame | Median GPU |
| --- | --- | ---: | ---: | ---: |
| 0 | Off | 314.62ms | 436.79ms | 77.59ms |
| 1 | On | 320.90ms | 427.15ms | 75.32ms |
| 2 | Off | 303.85ms | 403.48ms | 71.57ms |
| 3 | On | 324.10ms | 409.97ms | 61.90ms |

Evidence: instrumentation-raster-cache.txt and store-raster-*.json. Host GPU
utilization was still 99% at 20:24 local time, with DSPGAME running. The task
does not have a controlled performance environment. No FPS improvement is
claimed, and item 21 remains FAIL. Blur/refraction were not reduced.

## Parity Limits

PASS rows describe the specified rendering, browsing and pre-submission flows.
They are not a claim that every Flutter feature has been migrated. Flutter
4.5.6-pro (code 442) is installed but still shows its login welcome screen, so
same-account two-app runtime comparison remains incomplete. Source and five
existing Flutter contract tests provide the reference comparison.

Remote cart/checkout and final asset mutations still use the pre-existing local
cart/SafeMutationGuard boundary. Gift/recall now construct their real request
fields, but no transport is connected beyond the guard in this QA build.
These are not claims of real transaction implementations. Gift/recall/reclaim/apply/purchase actions
were not submitted. Full migration acceptance therefore remains open even if
the listed UI and read-only tests pass.

Current application APK SHA-256:
`24A8D1A6B30F6EEEAF727F621EFE7B1A8BF0B06B40FF2848C0DB25CE8A90B5A9`.
