package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableRecipeHandlerTrait
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenCustomHashMap
import it.unimi.dsi.fastutil.objects.Object2LongMap
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.trait.MEStock.IOptimizedMEList
import org.gtlcore.gtlcore.utils.NumberUtils
import com.gtladd.gtladditions.utils.TransferHelper
import kotlin.math.max
import kotlin.math.min
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.crafting.SizedIngredient
import net.neoforged.neoforge.items.IItemHandler
import org.gtlcore.gtlcore.api.machine.trait.ILongItemStorage
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient

/** Native input capability with exact long quantities; keys never own the caller's mutable stack. */
class FastNotifiableInputItemStack : NotifiableRecipeHandlerTrait<SizedIngredient>(),
    ICapabilityTrait, IItemHandler, IOptimizedMEList, ILongItemStorage {
    // Declare both Map type arguments for native GT's generic map transformer.
    @field:SaveField
    @field:SyncToClient
    private val savedInventory: MutableMap<ItemStack, Long> =
        Object2LongLinkedOpenCustomHashMap<ItemStack>(ItemStackHashStrategy.comparingAllButCount())
    private val inventory: Object2LongLinkedOpenCustomHashMap<ItemStack>
        get() = savedInventory as Object2LongLinkedOpenCustomHashMap<ItemStack>
    val realSize: Int get() = inventory.size
    fun isEmpty(): Boolean = inventory.isEmpty()
    fun getItemStorage(): Object2LongLinkedOpenCustomHashMap<ItemStack> = inventory
    override fun getMEItemMap(): Object2LongMap<ItemStack> = inventory
    override fun `gtlcore$getStoredAmount`(slot: Int): Long = inventory.object2LongEntrySet().elementAtOrNull(slot)?.longValue ?: 0
    fun addDisplayText(textList: MutableList<Component>) {
        inventory.object2LongEntrySet().forEach { textList.add(it.key.hoverName.copy().append(" " + NumberUtils.formatLong(it.longValue))) }
    }
    fun importFromNearby(vararg facings: Direction) {
        val level = machine.level ?: return
        facings.forEach { TransferHelper.importToTarget(this, machine.getItemCapFilter(it, IO.IN), level, machine.blockPos.relative(it), it.opposite) }
    }
    fun exportToNearby(vararg facings: Direction) {
        val level = machine.level ?: return
        facings.forEach { TransferHelper.exportToTarget(this, machine.getItemCapFilter(it, IO.OUT), level, machine.blockPos.relative(it), it.opposite) }
    }
    override fun getHandlerIO(): IO = IO.IN
    override fun getCapabilityIO(): IO = IO.IN
    override fun getCapability(): RecipeCapability<SizedIngredient> = ItemRecipeCapability.CAP
    fun onContentsChanged() { machine.setChanged(); syncDataHolder.markClientSyncFieldDirty("savedInventory"); notifyListeners() }
    override fun handleRecipeInner(io: IO, recipe: GTRecipe?, left: MutableList<SizedIngredient>, simulate: Boolean): List<SizedIngredient> {
        if (io != IO.IN) return left
        val available = Object2LongLinkedOpenCustomHashMap<ItemStack>(ItemStackHashStrategy.comparingAllButCount()).apply { putAll(inventory) }
        var changed = false
        val iter = left.listIterator()
        while (iter.hasNext()) {
            val ingredient = iter.next()
            var remaining = LongIngredient.amount(ingredient)
            if (remaining <= 0) { iter.remove(); continue }
            val matching = LongIngredient.matchingIngredient(ingredient)
            val entries = available.object2LongEntrySet().iterator()
            while (entries.hasNext() && remaining > 0) {
                val entry = entries.next()
                if (!matching.test(entry.key)) continue
                val used = min(entry.longValue, remaining)
                if (used <= 0) continue
                remaining -= used
                val stored = entry.longValue - used
                if (stored == 0L) entries.remove() else entry.setValue(stored)
                changed = true
            }
            if (remaining == 0L) iter.remove() else iter.set(LongIngredient.create(matching, remaining))
        }
        if (!simulate && changed) { inventory.clear(); inventory.putAll(available); onContentsChanged() }
        return left
    }
    override fun getContents(): List<Any> = inventory.object2LongEntrySet().map { it.key.copyWithCount(min(Int.MAX_VALUE.toLong(), it.longValue).toInt()) }
    override fun getTotalContentAmount(): Double = inventory.values.sumOf { it.toDouble() }
    override fun getSlots(): Int = max(realSize, 1)
    override fun getStackInSlot(slot: Int): ItemStack = inventory.object2LongEntrySet().elementAtOrNull(slot)?.let {
        it.key.copyWithCount(min(Int.MAX_VALUE.toLong(), it.longValue).toInt())
    } ?: ItemStack.EMPTY
    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY
        val inserted = min(Long.MAX_VALUE - inventory.getLong(stack), stack.count.toLong())
        if (!simulate && inserted > 0) { inventory.addTo(stack.copyWithCount(1), inserted); onContentsChanged() }
        return stack.copyWithCount(stack.count - inserted.toInt())
    }
    // This input capability never allows automation to take the machine's paid recipe inputs back.
    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = ItemStack.EMPTY
    override fun getSlotLimit(slot: Int): Int = Int.MAX_VALUE
    override fun isItemValid(slot: Int, stack: ItemStack): Boolean = true
    fun serializeNBT(registries: HolderLookup.Provider): CompoundTag = syncDataHolder.serializeNBT(registries)
    fun deserializeNBT(registries: HolderLookup.Provider, tag: CompoundTag) { syncDataHolder.deserializeNBT(registries, tag); notifyListeners() }
}
