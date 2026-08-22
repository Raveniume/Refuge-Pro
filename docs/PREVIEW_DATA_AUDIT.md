# Production Preview Data Audit

Preview data is allowed only in the Reference Lab and Design Lab. Production
routes must depend on a repository/cache adapter, even when that adapter is
read-only and offline.

| Preview source | Used by | Replacement | Status |
| --- | --- | --- | --- |
| `CachedHangarRepository` local snapshot in `data/HangarRepository.kt` | Hangar main, hero, inventory, detail | Same interface backed by mapped legacy cache/API response | OPEN |
| `rebuyItems` in `screens/HangarScreen.kt` | Hangar buyback section | `BuybackRepository` mapped from `lib/src/repo/buyback.dart` | OPEN |
| `catalogSnapshot` in `data/StoreRepository.kt` | Store categories, product detail | `StoreRepository` backed by versioned catalog cache and RSI adapter | OPEN |
| `terminalSnapshot` in `data/ProductionData.kt` | Terminal categories and detail | Category repositories backed by wiki/database cache | OPEN |
| `CachedProfileRepository` local snapshot in `data/ProductionData.kt` | Profile/account panels | `UserRepository` and session/account cache mapper | OPEN |
| `CachedUtilityRepository` local groups in `data/ProductionData.kt` | Tools and Profile utility rows | Utility repositories for valid legacy tools | OPEN |
| `CachedCcuRepository` local ships/owned list in `data/ProductionData.kt` | CCU selectors | Local catalog store populated by production cache/import | OPEN |
| Hard-coded Hangar log entries | Hangar logs | `HangarLogRepository` mapped from parsed log records | OPEN |
| Hard-coded Social/Gift/Referral rows | Tools/Profile sheets | Read-only account/referral/social adapters | OPEN |
| `ReferenceLabScreen` samples | Reference Lab only | Keep as lab fixture | EXEMPT |
| `DesignLabScreen` samples | Design Lab only | Keep as lab fixture | EXEMPT |
| `RefugeImagePlaceholder` | Empty/error/loading states | Keep as explicit fallback; never as successful production image | ACCEPTED |

## Audit rule

An entry may move to `CLOSED` only after the production screen consumes the
repository interface, loading/error/empty states are covered, and no preview
class or hard-coded list is reachable from the production root for that feature.
