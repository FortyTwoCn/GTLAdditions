package com.gtladd.gtladditions.utils

import appeng.api.AECapabilities
import appeng.api.config.Actionable
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.storage.MEStorage
import appeng.me.helpers.MachineSource
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputFluidTank
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper
import java.util.function.Predicate
import kotlin.math.min

/** Component-sensitive transfers. Deduct only quantities the real destination accepted, bounded per tick. */
object TransferHelper {
    fun exportToTarget(source: FastNotifiableInputFluidTank, filter: Predicate<FluidStack>, level: Level, pos: BlockPos, direction: Direction?) {
        val me = level.getCapability(AECapabilities.ME_STORAGE, pos, direction)
        if (me != null) { exportFluidToME(source, filter, me); return }
        level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction)?.let { exportToTarget(source, filter, it) }
    }
    fun exportToTarget(source: FastNotifiableInputItemStack, filter: Predicate<ItemStack>, level: Level, pos: BlockPos, direction: Direction?) {
        val me = level.getCapability(AECapabilities.ME_STORAGE, pos, direction)
        if (me != null) { exportItemToME(source, filter, me); return }
        level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction)?.let { exportToTarget(source, filter, it) }
    }
    fun exportToTarget(source: FastNotifiableInputItemStack, filter: Predicate<ItemStack>, target: IItemHandler) {
        val iterator = source.getItemStorage().object2LongEntrySet().iterator()
        var changed = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!filter.test(entry.key)) continue
            val offered = min(Int.MAX_VALUE.toLong(), entry.longValue).toInt()
            if (offered <= 0) continue
            val remainder = ItemHandlerHelper.insertItemStacked(target, entry.key.copyWithCount(offered), false)
            val inserted = offered - remainder.count
            require(inserted in 0..offered) { "Invalid item-handler insertion remainder" }
            if (inserted == 0) continue
            val remaining = entry.longValue - inserted
            if (remaining == 0L) iterator.remove() else entry.setValue(remaining)
            changed = true
        }
        if (changed) source.onContentsChanged()
    }
    fun exportToTarget(source: FastNotifiableInputFluidTank, filter: Predicate<FluidStack>, target: IFluidHandler) {
        val iterator = source.getFluidStorage().object2LongEntrySet().iterator()
        var changed = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!filter.test(entry.key)) continue
            val offered = min(Int.MAX_VALUE.toLong(), entry.longValue).toInt()
            if (offered <= 0) continue
            val inserted = target.fill(entry.key.copyWithAmount(offered), IFluidHandler.FluidAction.EXECUTE)
            require(inserted in 0..offered) { "Invalid fluid-handler insertion amount" }
            if (inserted == 0) continue
            val remaining = entry.longValue - inserted
            if (remaining == 0L) iterator.remove() else entry.setValue(remaining)
            changed = true
        }
        if (changed) source.onContentsChanged()
    }
    fun importToTarget(target: FastNotifiableInputItemStack, filter: Predicate<ItemStack>, level: Level, pos: BlockPos, direction: Direction?) {
        level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction)?.let { importToTarget(target, filter, it) }
    }
    fun importToTarget(target: FastNotifiableInputFluidTank, filter: Predicate<FluidStack>, level: Level, pos: BlockPos, direction: Direction?) {
        level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction)?.let { importToTarget(target, filter, it) }
    }
    fun importToTarget(target: FastNotifiableInputItemStack, filter: Predicate<ItemStack>, source: IItemHandler) {
        var changed = false
        val inventory = target.getItemStorage()
        for (slot in 0 until source.slots) {
            val preview = source.extractItem(slot, Int.MAX_VALUE, true)
            if (preview.isEmpty || !filter.test(preview)) continue
            val offered = min(Long.MAX_VALUE - inventory.getLong(preview), preview.count.toLong()).toInt()
            if (offered <= 0) continue
            val extracted = source.extractItem(slot, offered, false)
            if (extracted.isEmpty) continue
            check(ItemStack.isSameItemSameComponents(preview, extracted) && extracted.count <= offered) { "Item handler changed its extraction after simulation" }
            inventory.addTo(extracted.copyWithCount(1), extracted.count.toLong())
            changed = true
        }
        if (changed) target.onContentsChanged()
    }
    fun importToTarget(target: FastNotifiableInputFluidTank, filter: Predicate<FluidStack>, source: IFluidHandler) {
        var changed = false
        val inventory = target.getFluidStorage()
        for (tank in 0 until source.tanks) {
            val preview = source.getFluidInTank(tank).copy()
            if (preview.isEmpty || !filter.test(preview)) continue
            val offered = min(Long.MAX_VALUE - inventory.getLong(preview), preview.amount.toLong()).toInt()
            if (offered <= 0) continue
            val drained = source.drain(preview.copyWithAmount(offered), IFluidHandler.FluidAction.EXECUTE)
            if (drained.isEmpty) continue
            check(FluidStack.isSameFluidSameComponents(preview, drained) && drained.amount <= offered) { "Fluid handler changed its extraction" }
            inventory.addTo(drained.copyWithAmount(1), drained.amount.toLong())
            changed = true
        }
        if (changed) target.onContentsChanged()
    }
    fun insertFromME(itemTarget: FastNotifiableInputItemStack, fluidTarget: FastNotifiableInputFluidTank,
                     what: AEKey, amount: Long, mode: Actionable): Long {
        if (amount <= 0) return 0
        return when (what) {
            is AEItemKey -> {
                val stack = what.toStack(1)
                val inventory = itemTarget.getItemStorage()
                val inserted = min(amount, Long.MAX_VALUE - inventory.getLong(stack))
                if (mode == Actionable.MODULATE && inserted > 0) { inventory.addTo(stack, inserted); itemTarget.onContentsChanged() }
                inserted
            }
            is AEFluidKey -> {
                val stack = what.toStack(1)
                val inventory = fluidTarget.getFluidStorage()
                val inserted = min(amount, Long.MAX_VALUE - inventory.getLong(stack))
                if (mode == Actionable.MODULATE && inserted > 0) { inventory.addTo(stack, inserted); fluidTarget.onContentsChanged() }
                inserted
            }
            else -> 0
        }
    }
    private fun exportItemToME(source: FastNotifiableInputItemStack, filter: Predicate<ItemStack>, target: MEStorage) {
        val iterator = source.getItemStorage().object2LongEntrySet().iterator()
        var changed = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!filter.test(entry.key)) continue
            val key = AEItemKey.of(entry.key) ?: continue
            val inserted = target.insert(key, entry.longValue, Actionable.MODULATE, MachineSource { null })
            require(inserted in 0..entry.longValue) { "Invalid ME item insertion amount" }
            if (inserted == 0L) continue
            val remaining = entry.longValue - inserted
            if (remaining == 0L) iterator.remove() else entry.setValue(remaining)
            changed = true
        }
        if (changed) source.onContentsChanged()
    }
    private fun exportFluidToME(source: FastNotifiableInputFluidTank, filter: Predicate<FluidStack>, target: MEStorage) {
        val iterator = source.getFluidStorage().object2LongEntrySet().iterator()
        var changed = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!filter.test(entry.key)) continue
            val key = AEFluidKey.of(entry.key)
            val inserted = target.insert(key, entry.longValue, Actionable.MODULATE, MachineSource { null })
            require(inserted in 0..entry.longValue) { "Invalid ME fluid insertion amount" }
            if (inserted == 0L) continue
            val remaining = entry.longValue - inserted
            if (remaining == 0L) iterator.remove() else entry.setValue(remaining)
            changed = true
        }
        if (changed) source.onContentsChanged()
    }
}
