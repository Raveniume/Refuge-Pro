# Production Data Audit

Production routes no longer depend on Reference Lab fixtures or UI-local
preview rows. User/account surfaces consume RSI-backed repositories and retain
their last successful private snapshot in app storage. Public Store, Terminal
and CCU catalogues use the same cache-then-refresh rule. The bundled manifest
contains no fabricated production rows.

| Former preview source | Production consumer | Current adapter | Status |
| --- | --- | --- | --- |
| Hangar local snapshot | Hangar main, hero, inventory, detail | `RsiLiveHangarRepository` persistent snapshot | CLOSED |
| Buyback sample rows | Hangar buyback | `RsiLiveBuybackRepository` persistent snapshot | CLOSED |
| Catalog snapshot | Store categories, product detail, cart | `RsiLiveStoreRepository` persistent snapshot | CLOSED |
| Terminal snapshot | Terminal categories and detail | `WikiTerminalRepository` persistent snapshot | CLOSED |
| Profile local values | Profile/account panels | `RsiLiveProfileRepository` persistent snapshot | CLOSED |
| Utility rows and tool details | Tools/Profile utility sheets and external links | `ProductionUtilityRepository` | CLOSED |
| CCU ship and owned list | Seed/target selectors, owned editor, chain detail | `RsiLiveCcuRepository` persistent catalogue + cached hangar | CLOSED |
| Hangar log strings | Hangar log sheet | `RsiLiveHangarLogRepository` persistent snapshot | CLOSED |
| Loading/error placeholders | All production repositories | Shared loading/error/empty states with retry | CLOSED |
| ReferenceLab samples | Reference Lab only | Lab fixture | EXEMPT |
| DesignLab samples | Design Lab only | Lab fixture | EXEMPT |
| `RefugeImagePlaceholder` | Explicit image fallback states | Fallback only; never a successful production image | ACCEPTED |

## Boundary rules

- Production composition uses explicit live repositories with read-only cached fallbacks.
- Reference Lab and Design Lab may keep sample data; neither is reachable from
  a production root tab.
- Empty, loading, and error states are explicit UI states. A repository failure
  never leaves a blank page or triggers a destructive retry.
- External utility links open a browser intent only. They do not submit RSI
  account, gift, reclaim, upgrade, or purchase mutations.

Production preview/mock repositories remaining: **NONE**.
