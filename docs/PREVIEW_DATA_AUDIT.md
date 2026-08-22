# Production Data Audit

Production routes no longer depend on Reference Lab fixtures or UI-local
preview rows. The current offline-first build consumes versioned repository
adapters backed by the bundled legacy cache import (`legacy-cache-v1`, refreshed
2026-08-20). A future network refresh can replace the adapter implementation
without changing the Compose contracts or the approved Hangar geometry.

| Former preview source | Production consumer | Current adapter | Status |
| --- | --- | --- | --- |
| Hangar local snapshot | Hangar main, hero, inventory, detail | `ProductionHangarRepository` | CLOSED |
| Buyback sample rows | Hangar buyback | `ProductionBuybackRepository` | CLOSED |
| Catalog snapshot | Store categories, product detail, cart | `ProductionCatalogStoreRepository` | CLOSED |
| Terminal snapshot | Terminal categories and detail | `ProductionTerminalRepository` | CLOSED |
| Profile local values | Profile/account panels | `ProductionProfileRepository` | CLOSED |
| Utility rows and tool details | Tools/Profile utility sheets and external links | `ProductionUtilityRepository` | CLOSED |
| CCU ship and owned list | Seed/target selectors, owned editor, chain detail | `ProductionCcuRepository` plus local planner | CLOSED |
| Hangar log strings | Hangar log sheet | `ProductionHangarLogRepository` | CLOSED |
| Loading/error placeholders | All production repositories | Shared loading/error/empty states with retry | CLOSED |
| ReferenceLab samples | Reference Lab only | Lab fixture | EXEMPT |
| DesignLab samples | Design Lab only | Lab fixture | EXEMPT |
| `RefugeImagePlaceholder` | Explicit image fallback states | Fallback only; never a successful production image | ACCEPTED |

## Boundary rules

- Production composition uses the explicit `Production*Repository` classes.
- Reference Lab and Design Lab may keep sample data; neither is reachable from
  a production root tab.
- Empty, loading, and error states are explicit UI states. A repository failure
  never leaves a blank page or triggers a destructive retry.
- External utility links open a browser intent only. They do not submit RSI
  account, gift, reclaim, upgrade, or purchase mutations.

Production preview/mock repositories remaining: **NONE**.
