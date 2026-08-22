# Removed Features Audit

This separates RefugeNext-owned paid/member gates from normal Star Citizen/RSI commerce. The legacy Flutter checkout is reference-only; the Compose migration does not copy its paid service gates.

| Feature or signal | Legacy evidence | Compose decision | Removal boundary |
|---|---|---|---|
| AI tab/menu/page | `lib/src/widgets/ai_chat/ai_chat_page.dart` and legacy navigation model. | Removed. | No Compose destination, menu entry, settings row, state, resource, or flag. |
| AI service and quota | `lib/src/network/cirno/ai_chat_service.dart`, `lib/src/datasource/ai_chat_model.dart`, `会员 · 无限额度`. | Not ported. | No AI service/repository, usage call, quota model, or network request. |
| AI executor/tool bridge | `lib/src/repo/repo_ai_tool_executor.dart`. | Not ported. | No AI-specific tool bridge or adapter. |
| RefugeNext premium/VIP/member unlock | Legacy `isVip` / `vipExpire` / `MainDataModel.isVIP` branches around CCU service. | Removed from migration target. | No entitlement state, paywall, sponsor unlock, local VIP flag, or member-only route. |
| Remote paid CCU/pathfinding service | Legacy `v2/upgrade/path` server contract. | Replaced by the tested local planner. | No remote planner call; local catalog and deterministic cost calculation remain. |
| Normal RSI store products | Legacy shop pages and Compose catalog contain ships, paints, gear, packages, Warbond and prices. | Retained. | These are Star Citizen/RSI commerce, not RefugeNext membership gates. |
| Normal RSI checkout/upgrade | Legacy shop checkout and CCU purchase semantics. | Retained as a product boundary. | Keep product detail, price, cart/checkout and CCU purchase intent without RefugeNext membership state. |

## Current Compose search

No AI route/service/repository/state/network/resource/feature flag exists in `app/src/main`. `StoreCategory.SUBSCRIBER` is a catalog category and is not treated as a RefugeNext entitlement without an explicit gate; RSI catalog data remains available.

## Acceptance gate

- AI removed: no AI implementation or gate under `app/src/main`.
- Paid RefugeNext gate removed: CCU UI uses only local planner state.
- RSI commerce retained: store products, prices, product detail and checkout boundaries remain reachable.
