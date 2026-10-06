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
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidParallelLogic
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient
import org.gtlcore.gtlcore.api.machine.trait.ILongFluidStorage
import org.gtlcore.gtlcore.api.recipe.ingredient.LongFluidIngredient

/** Native stacks expose an int view; the map owns component-sensitive, exact long quantities. */
class FastNotifiableInputFluidTank : NotifiableRecipeHandlerTrait<SizedFluidIngredient>(),
    ICapabilityTrait, IFluidHandler, IOptimizedMEList, ILongFluidStorage {
    @field:SaveField
    @field:SyncToClient
    private val savedInventory: MutableMap<FluidStack, Long> =
        Object2LongLinkedOpenCustomHashMap<FluidStack>(LongFluidParallelLogic.fluidIdentity())
    private val inventory: Object2LongLinkedOpenCustomHashMap<FluidStack>
        get() = savedInventory as Object2LongLinkedOpenCustomHashMap<FluidStack>
    val realSize: Int get() = inventory.size
    fun isEmpty(): Boolean = inventory.isEmpty()
    fun getFluidStorage(): Object2LongLinkedOpenCustomHashMap<FluidStack> = inventory
    override fun getMEFluidMap(): Object2LongMap<FluidStack> = inventory
    override fun `gtlcore$getStoredAmount`(tank: Int): Long = inventory.object2LongEntrySet().elementAtOrNull(tank)?.longValue ?: 0
    fun addDisplayText(textList: MutableList<Component>) {
        inventory.object2LongEntrySet().forEach { textList.add(it.key.hoverName.copy().append(" " + NumberUtils.formatLong(it.longValue) + " mB")) }
    }
    fun importFromNearby(vararg facings: Direction) {
        val level = machine.level ?: return
        facings.forEach { TransferHelper.importToTarget(this, machine.getFluidCapFilter(it, IO.IN), level, machine.blockPos.relative(it), it.opposite) }
    }
    fun exportToNearby(vararg facings: Direction) {
        val level = machine.level ?: return
        facings.forEach { TransferHelper.exportToTarget(this, machine.getFluidCapFilter(it, IO.OUT), level, machine.blockPos.relative(it), it.opposite) }
    }
    override fun getHandlerIO(): IO = IO.IN
    override fun getCapabilityIO(): IO = IO.IN
    override fun getCapability(): RecipeCapability<SizedFluidIngredient> = FluidRecipeCapability.CAP
    fun onContentsChanged() { machine.setChanged(); syncDataHolder.markClientSyncFieldDirty("savedInventory"); notifyListeners() }
    override fun handleRecipeInner(io: IO, recipe: GTRecipe?, left: MutableList<SizedFluidIngredient>, simulate: Boolean): List<SizedFluidIngredient> {
        if (io != IO.IN) return left
        val available = Object2LongLinkedOpenCustomHashMap<FluidStack>(LongFluidParallelLogic.fluidIdentity()).apply { putAll(inventory) }
        var changed = false
        val iter = left.listIterator()
        while (iter.hasNext()) {
            val ingredient = iter.next()
            var remaining = LongFluidIngredient.amount(ingredient)
            if (remaining <= 0) { iter.remove(); continue }
            val matching = LongFluidIngredient.matchingIngredient(ingredient)
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
            if (remaining == 0L) iter.remove() else iter.set(LongFluidIngredient.create(matching, remaining))
        }
        if (!simulate && changed) { inventory.clear(); inventory.putAll(available); onContentsChanged() }
        return left
    }
    override fun getContents(): List<Any> = inventory.object2LongEntrySet().map { it.key.copyWithAmount(min(Int.MAX_VALUE.toLong(), it.longValue).toInt()) }
    override fun getTotalContentAmount(): Double = inventory.values.sumOf { it.toDouble() }
    override fun getTanks(): Int = max(realSize, 1)
    override fun getFluidInTank(tank: Int): FluidStack = inventory.object2LongEntrySet().elementAtOrNull(tank)?.let {
        it.key.copyWithAmount(min(Int.MAX_VALUE.toLong(), it.longValue).toInt())
    } ?: FluidStack.EMPTY
    override fun getTankCapacity(tank: Int): Int = Int.MAX_VALUE
    override fun isFluidValid(tank: Int, stack: FluidStack): Boolean = true
    override fun fill(resource: FluidStack, action: IFluidHandler.FluidAction): Int {
        if (resource.isEmpty) return 0
        val inserted = min(Long.MAX_VALUE - inventory.getLong(resource), resource.amount.toLong()).toInt()
        if (action.execute() && inserted > 0) { inventory.addTo(resource.copyWithAmount(1), inserted.toLong()); onContentsChanged() }
        return inserted
    }
    override fun drain(resource: FluidStack, action: IFluidHandler.FluidAction): FluidStack = FluidStack.EMPTY
    override fun drain(maxDrain: Int, action: IFluidHandler.FluidAction): FluidStack = FluidStack.EMPTY
    fun serializeNBT(registries: HolderLookup.Provider): CompoundTag = syncDataHolder.serializeNBT(registries)
    fun deserializeNBT(registries: HolderLookup.Provider, tag: CompoundTag) { syncDataHolder.deserializeNBT(registries, tag); notifyListeners() }
}
