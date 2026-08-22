# RefugeNext Full Migration Matrix

The legacy Flutter checkout is used only for feature inventory, information
architecture, and business/data contracts. Production presentation is Compose
and uses the frozen Liquid Glass design system.

| Feature/Page | Original Flutter source | Compose status | Business logic | UI | Real data | Navigation | QA | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Root navigation | `lib/src/widgets/navigation/*` | QA PASS | Shared root and origin tab state | Four official Liquid tabs | Production repositories | Root -> subpage -> back | QA PASS | Tools is a secondary route, not a fifth tab |
| Hangar main/search/filter/sort | `lib/src/widgets/hangar/hangar_page.dart`, `hangar_search_bottomsheet.dart` | QA PASS | Stable query, predicates, date sort | Golden Master preserved | `ProductionHangarRepository` | Root -> controls -> detail | QA PASS | Dense 1:1 inventory layout is frozen |
| Hangar detail/package contents | `hangar_item_detail_widget.dart`, `hangar_export_bottomsheet.dart` | QA PASS | Value, insurance, package, upgrade fields mapped | Adaptive Liquid sheet | Production hangar cache | Detail -> sheet -> back | QA PASS | Read-only export boundary |
| Hangar buyback | `lib/src/widgets/hangar_buyback/*`, `lib/src/repo/buyback.dart` | QA PASS | Buyback model and detail mapping | Rebuy list and sheet | `ProductionBuybackRepository` | Hangar segmented section | QA PASS | No real repurchase |
| Hangar logs | `hangar_log/*`, `lib/src/repo/hangar_log.dart` | QA PASS | Imported log records | Read-only Liquid sheet | `ProductionHangarLogRepository` | Hangar -> logs -> back | QA PASS | No mutation |
| Hangar Gift/Reclaim/Upgrade/Jump | `ship_gift_modal.dart`, `ship_reclaim_modal.dart`, `upgrade_from_choose_bottomsheet.dart` | QA PASS | Validation and request boundary | Confirmation/action sheet | Local request construction | Detail -> safe action / CCU | QA PASS | Gift, Reclaim and purchase actions are safeNoOp |
| Store main/categories | `lib/src/widgets/shop/*` | QA PASS | Category, price band, Warbond predicates | Liquid segmented/list | `ProductionCatalogStoreRepository` | Root -> category -> detail | QA PASS | RSI commerce retained |
| Store search/filter/sort/detail | `shop_search_bottomsheet.dart`, `catalog_detail_bottomsheet.dart` | QA PASS | Local filtering and sorting | Search, compact controls, detail sheet | Versioned catalog import and image URLs | Store -> controls/detail -> back | QA PASS | Loading/error/empty/retry included |
| Store cart/checkout | `shop/cart/*`, checkout sheets | QA PASS | In-memory quantity aggregation | Cart sheet and safe checkout preview | Local cart repository | Store -> cart -> back | QA PASS | Real RSI purchase never submitted |
| Store upgrade selector | `upgrade_page.dart`, `ship_select_bottomsheet.dart` | QA PASS | Routes to local CCU planner | Product detail upgrade intent | Local CCU catalog | Store -> product -> CCU | QA PASS | Safe purchase intent only |
| Terminal categories | `lib/src/widgets/database/database_page.dart` | QA PASS | Nine legacy categories, category predicates | Moving Liquid segmented | `ProductionTerminalRepository` | Root -> category -> detail | QA PASS | Each category has data |
| Terminal search/filter/sort/detail | `database_search_page.dart`, `ship_info_neo/*` | QA PASS | Query/tag/USD filters and name sort | Search, filter sheet, detail sheet | Versioned terminal import | Category -> detail -> back | QA PASS | Detail exposes manufacturer/value/description |
| Profile/account | `user_info/user_page.dart`, `refuge_account_detail_page.dart` | QA PASS | Account balances and identity contract | Avatar, stats, account, utility panels | `ProductionProfileRepository` | Root -> Profile -> Settings | QA PASS | Shared UserStatus source |
| Presence/status | `status_avatar.dart`, `spectrum_ws_service.dart` | QA PASS | Local session status source | Shared avatar indicator/toggle | App `UserStatusSource` | Header/Profile share state | QA PASS | Remote presence intentionally offline |
| Friends/social and utility tools | `friends/*`, `utility/*`, `referral*` | QA PASS | Valid read-only tool contracts | Shared Liquid rows/sheets | `ProductionUtilityRepository` | Profile/Tools -> detail | QA PASS | External RSI links use browser intent only |
| Settings | `settings/settings_page.dart` | QA PASS | Theme, sync, local-only, cache marker | Liquid toggles and grouped rows | `PreferencesSettingsRepository` | Profile -> Settings -> back | QA PASS | Cache clear never removes account/hangar data |
| Local CCU planner | `ccu_optimizor/*`, `local_ccu_planner.dart` | QA PASS | Actual purchase-price formula and target constraint | Seed/target selectors, owned editor, chain | `ProductionCcuRepository` | Hangar/Store -> CCU -> result | QA PASS | No paid/remote planner |
| Global sheets/dialogs/loading/errors | `lib/src/widgets/general/*` and feature sheets | QA PASS | Shared modal and safe-action boundaries | Liquid sheet/dialog/loading/error/empty | N/A | Deep routes return to origin | QA PASS | No production Material fallback |
| AI | `ai_chat/*`, `repo/ai_chat.dart`, `network/cirno/*` | REMOVED BY DESIGN | Not migrated | No production UI | None | No route | QA PASS | Route, service, state, settings removed |
| RefugeNext Premium/VIP/paywall | `cirno_auth.dart`, `isVip` branches, paid planner | REMOVED BY DESIGN | Local planner replaces paid service | No entitlement UI | None | No gated route | QA PASS | RSI store products remain normal commerce |

## Completion gate

- Original feature audit: **COMPLETE**
- Hangar, Store, Terminal, Profile, Tools, Settings, and Local CCU: **COMPLETE**
- AI removal and RefugeNext paid-feature removal: **COMPLETE**
- Production preview/mock repositories remaining: **NONE**
- Navigation and Dark/Light usability: **PASS**
- Runtime stability: **PASS**
- Real Gift/Reclaim/Upgrade purchase/RSI purchase executed: **NO**
