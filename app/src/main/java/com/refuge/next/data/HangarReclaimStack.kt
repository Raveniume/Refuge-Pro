package com.refuge.next.data

/** Legacy stack identity, scoped to the reclaim form so the inventory stays ungrouped. */
internal fun reclaimStackFor(selected: HangarItem, inventory: List<HangarItem>): HangarItem {
    fun sameStack(other: HangarItem): Boolean =
        other.originalName == selected.originalName && other.title == selected.title &&
            other.status == selected.status && other.price == selected.price &&
            other.includedItems == selected.includedItems &&
            other.includedEntries.map { it.title } == selected.includedEntries.map { it.title } &&
            other.insurance == selected.insurance && other.isUpgrade == selected.isUpgrade &&
            other.isGiftable == selected.isGiftable && other.isReclaimable == selected.isReclaimable
    val group = listOf(selected) + inventory.filter { it.id != selected.id && sameStack(it) }
    val ids = group.flatMap { it.idList }
    return selected.copy(quantity = group.sumOf { it.quantity }, idList = ids)
}
