# Hangar Detail Parity Audit

This table is filled from the original Flutter detail implementation before a
field can be removed or marked complete.

| Original field | Compose field | Data source | Status |
| --- | --- | --- | --- |
| Hero image | Hero image | Pledge/package image | PASS: package and gear runtime captures |
| Chinese name | Title | `ProductionTranslationRepository` | PASS: package and gear runtime captures |
| Original name / type | Subtitle | Pledge parser | PASS: bilingual title/type runtime captures |
| Melt value | 可融 | Pledge value | PASS: `$308.75` package and `$30` gear runtime captures |
| Current ship value | 当前舰值 | Ship/catalog/upgrade resolution | PASS: `$800` Perseus package runtime capture |
| Savings | 节省 | Current value minus melt value | PASS: `$491.25` package runtime capture |
| Package contents | Typed included rows | Pledge item contents | PASS: long package and compact gear lists scroll in one content stream |
| Also contains | Plain content list after typed rows | All pledge `.title` entries | IMPLEMENTED; RUNTIME PENDING |
| Upgrade from/to and values | 升级路径 | Owned CCU/pledge metadata | IMPLEMENTED; RUNTIME PENDING |
| Created date | 入库日期 | Pledge metadata | IMPLEMENTED; RUNTIME PENDING |
| Insurance | 保险 | Package contents/pledge metadata | IMPLEMENTED; RUNTIME PENDING |
| Status and other metadata | 状态、机库编号、页码、能力 flags | Pledge metadata | IMPLEMENTED; RUNTIME PENDING |
| Actions | Guarded Gift/Reclaim, safe jump, owned-CCU apply route | Original route/action contract | IMPLEMENTED; RUNTIME PENDING |
| Logs | Persistent live pledge-log sheet | `api/account/pledgeLog` | IMPLEMENTED; RUNTIME PENDING |
