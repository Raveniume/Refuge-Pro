# RefugeNext Full Migration Matrix

This matrix is the production migration inventory for the Compose checkout. The
legacy Flutter checkout is used only for feature contracts, information
architecture, and navigation behavior. Flutter presentation is not copied.

Statuses are `NOT STARTED`, `PARTIAL`, `MIGRATED`, `QA PASS`, or
`REMOVED BY DESIGN`.

| Feature/Page | Original Flutter source | Compose status | Business logic | UI | Real data | Navigation | QA | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Root navigation | `lib/src/widgets/navigation/main_navigation_bar.dart`, `root_top_navigation.dart` | PARTIAL | Root tab state exists | Liquid bottom tabs reused | Local models | Root -> subpage -> back implemented | PARTIAL | Four production tabs; no Tools tab in root |
| Hangar main | `lib/src/widgets/hangar/hangar_page.dart` | PARTIAL | Hangar item/value models exist | Migration Golden Baseline | `CachedHangarRepository` read-only boundary | Root -> detail/sheet | PARTIAL | Replace local cache snapshot with API/cache mapper |
| Hangar search | `lib/src/widgets/hangar/hangar_search_bottomsheet.dart` | PARTIAL | Query contract audited | Search field/sheet available | Cached inventory | Hangar -> search sheet | NOT STARTED | Add parser-backed refresh/error state |
| Hangar filter | `lib/src/widgets/hangar/hangar_page.dart`, `hangar_search_bottomsheet.dart` | PARTIAL | Gift/reclaim/type predicates exist in model | Compact control and sheet exist | Cached inventory | PARTIAL | Add parser-backed refresh/error state |
| Hangar sort | `lib/src/widgets/hangar/hangar_page.dart` | PARTIAL | Stable date/value sort pending | Compact control exists | Cached inventory | PARTIAL | Persisted only for current session |
| Hangar detail | `lib/src/widgets/hangar/hangar_item_detail_widget.dart` | PARTIAL | Detail mapping preserves value/insurance/package fields | Liquid sheet and actions exist | Cached inventory | PARTIAL | Safe action intercept required |
| Hangar package contents | `hangar_item_detail_widget.dart`, `hangar_export_bottomsheet.dart` | PARTIAL | Included item model exists | Included rows exist | Preview list only | Detail -> contents | NOT STARTED | Export remains read-only until adapter exists |
| Hangar logs | `lib/src/widgets/hangar/hangar_log/hangar_log_page.dart`, `hangar_log_bottomsheet.dart` | PARTIAL | Log repository contract identified | Read-only sheet exists | Hard-coded evidence rows | Hangar -> logs -> back | NOT STARTED | Map parsed log entries |
| Hangar buyback | `lib/src/widgets/hangar_buyback/hangar_buyback_page.dart`, `lib/src/repo/buyback.dart` | PARTIAL | Read-only buyback model pending | Rebuy section exists | Local sample rows | Hangar segmented section | NOT STARTED | Add repository boundary and loading/error states |
| Hangar gift | `lib/src/widgets/hangar/ship_gift_modal.dart` | PARTIAL | Request validation can be represented | Detail action exists | None | Detail -> confirmation | NOT STARTED | Must be safeNoOp/intercept; never submit |
| Hangar reclaim | `ship_recall_modal.dart`, `ship_reclaim_modal.dart` | PARTIAL | Request validation can be represented | Detail action exists | None | Detail -> confirmation | NOT STARTED | Must be safeNoOp/intercept; never submit |
| Hangar upgrade entry | `upgrade_from_choose_bottomsheet.dart` | PARTIAL | Local CCU engine exists | Detail action routes to CCU | Local catalog | Detail -> CCU | PARTIAL | Preserve source/target constraint |
| Store main | `lib/src/widgets/shop/shop_page.dart`, `normal_shop_page.dart` | PARTIAL | Catalog product model exists | Liquid list, search, filter, sort | Cached catalog snapshot | Root -> Store | PARTIAL | Replace snapshot with cache/API adapter |
| Store categories | `shop_list_page.dart`, `subscriber_shop_page.dart` | MIGRATED | Category enum exists | Moving Liquid segmented | Snapshot | Store category -> list | PARTIAL | RSI subscription products remain commerce, not app VIP |
| Store search/filter/sort | `shop_search_bottomsheet.dart`, shop page widgets | MIGRATED | Local predicates and price bands | Shared search/compact controls | Snapshot | Store -> control sheet | PARTIAL | Need error/retry state and persistent filter semantics |
| Store product detail | `catalog_detail_bottomsheet.dart` | MIGRATED | Product detail contract mapped | Liquid sheet with image/metadata | Snapshot + network image | Store -> detail -> back | PARTIAL | Cart intent currently read-only |
| Store cart | `lib/src/widgets/shop/cart/cart.dart`, checkout sheets | PARTIAL | Cart API contract identified | Notice sheet only | None | Store -> cart | NOT STARTED | Implement local cart state; no real purchase |
| Store upgrade selector | `upgrade_page.dart`, `ship_select_bottomsheet.dart`, `upgrade_select_sku.dart` | PARTIAL | RSI upgrade semantics retained | CCU route exists | Local catalog | Store -> CCU | NOT STARTED | Add selector and safe purchase intent |
| Terminal categories | `lib/src/widgets/database/database_page.dart` | PARTIAL | Five production categories modeled | Moving Liquid segmented | Terminal snapshot | Root -> Terminal -> category | PARTIAL | Smoke each category and detail/back |
| Terminal search/filter/sort | `database_search_page.dart`, database page | PARTIAL | Query/tag/price predicates exist | Search/filter/sort controls | Snapshot | Terminal -> controls | NOT STARTED | Add category-specific data adapters |
| Terminal detail | `ship_info_neo/*_detail_page.dart`, `ship_info/*` | PARTIAL | Generic detail model exists | Detail sheet exists | Snapshot | Category -> detail -> back | PARTIAL | Expand components/shops/ports fields |
| Profile/account | `lib/src/widgets/user_info/user_page.dart`, `refuge_account_detail_page.dart` | PARTIAL | ProfileData contract mapped | Profile panels and account group | Local profile snapshot | Root -> Profile | PARTIAL | Add session/cache adapter |
| Presence/status | `status_avatar.dart`, `spectrum_ws_service.dart` | PARTIAL | Shared root boolean state | Avatar indicator/toggle | Local state | Header/Profile share state | NOT STARTED | Replace boolean with UserStatus source |
| Friends/social | `lib/src/widgets/friends/*` | PARTIAL | Tool entry and sheet exist | Social sheet | Hard-coded rows | Profile/Tools -> social | NOT STARTED | Add read-only session adapter |
| Tools | `lib/src/widgets/utility/*`, `user_info/referral*` | PARTIAL | Tool inventory mapped | Shared Glass rows/sheets | Hard-coded rows | Profile/Tools -> utility | NOT STARTED | Implement valid utility adapters; omit dead routes |
| Settings | `lib/src/widgets/settings/settings_page.dart` | PARTIAL | Theme/local settings modeled | Liquid toggles/action rows | Local state | Profile -> Settings -> back | PARTIAL | Add persistence/cache diagnostics |
| Local CCU planner | `ccu_optimizor/*`, `local_ccu_planner.dart` | PARTIAL | Tested local cost engine exists | Seed/target/owned/result flow exists | Local catalog | Hangar/Store -> CCU | PARTIAL | Add owned CCU editor and chain detail |
| Global sheets/dialogs | `lib/src/widgets/general/*`, game/hangar sheets | PARTIAL | Shared modal boundaries exist | Liquid sheets/dialogs reused | N/A | Deep routes | NOT STARTED | Audit alert/menu/loading/error/empty states |
| AI | `lib/src/widgets/ai_chat/*`, `lib/src/repo/ai_chat.dart`, `lib/src/network/cirno/*` | REMOVED BY DESIGN | Not migrated | No production UI | None | No route | QA PASS | Do not restore AI services, state, settings, or API |
| RefugeNext Premium/VIP/paywall | `cirno_auth.dart`, `data_model.dart`, CCU gates | REMOVED BY DESIGN | Local planner replaces paid planner | No entitlement UI | None | No gated route | QA PASS | RSI product prices/commerce remain |

## Completion gate

The migration phase is complete only when every applicable row is `QA PASS`,
Preview production dependencies are zero or explicitly justified, and the
destructive-action checks below remain negative:

- Real Gift executed: **NO**
- Real Reclaim executed: **NO**
- Real Upgrade purchase executed: **NO**
- Real RSI purchase executed: **NO**
