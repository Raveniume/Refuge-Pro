# Old Detail Field Audit

This audit keeps the Compose ship/package detail aligned with the legacy hangar detail information architecture. The new surface owns presentation and interaction; it does not copy the Flutter widget tree or its styling.

| Legacy field / section | Compose status | New Detail location |
| --- | --- | --- |
| Item image | Present | Hero image |
| Item title / translated name | Present | Hero title |
| Package or item subtitle | Present | Hero subtitle |
| Entry date | Present | Hero metadata and Other information |
| Insurance | Present | Hero metadata and Other information |
| Melt / reclaim value (`可融`) | Present | Value Summary |
| Current ship value (`当前舰值`) | Present for owned ships | Value Summary |
| Savings (`节省`) | Present for owned ships | Value Summary |
| Description / local status | Present | Description and Other information |
| Package contents (`内含`) | Present | Included Items |
| Gift action | Present | Persistent action area |
| Reclaim action | Present | Persistent action area |
| Upgrade / CCU action | Present | Persistent action area |
| Hangar log / history action | Present | Persistent action area |

The preview repository currently supplies deterministic local values. When the production adapter is connected, each row maps to the same slots without changing the information hierarchy.
