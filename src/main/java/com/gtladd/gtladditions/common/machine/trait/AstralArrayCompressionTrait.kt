package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.MachineUtil
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import java.math.BigDecimal
import kotlin.math.pow

class AstralArrayCompressionTrait : MachineTrait() {

    @field:SaveField
    @field:SyncToClient
    var compressionAstralArrayCount = 0L
        private set

    @field:SaveField
    @field:SyncToClient
    var pendingCompressedAstralArrays = 0L
        private set

    private var outputSubscription: TickableSubscription? = null

    override fun getMachine(): ArcanicAstrograph = super.getMachine() as ArcanicAstrograph

    fun handleCompressionWorking() {
        flushPendingOutputs()
        collectAstralArraysInCompressionArea()
    }

    fun finishCompression() {
        if (!flushPendingOutputs()) return
        val batches = compressionAstralArrayCount / IAstralArrayInteractionMachine.COMPRESSED_ASTRAL_ARRAY_EQUIVALENT
        if (batches <= 0) return
        if (machine.level !is ServerLevel) return
        // Commit one probability result before attempting delivery. A blocked output must not reroll it.
        pendingCompressedAstralArrays = calculateCompressionOutputs(batches)
        compressionAstralArrayCount -= batches * IAstralArrayInteractionMachine.COMPRESSED_ASTRAL_ARRAY_EQUIVALENT
        onOwnedStateChanged()
        flushPendingOutputs()
    }

    fun pauseCompression() {
        // Collected inputs and already rolled outputs remain owned through stopping or changing recipes.
        flushPendingOutputs()
    }

    override fun onMachineLoad() {
        super.onMachineLoad()
        if (!isRemote && pendingCompressedAstralArrays > 0) subscribeOutputRetry()
    }

    override fun onMachineUnload() {
        unsubscribe(outputSubscription)
        outputSubscription = null
        super.onMachineUnload()
    }

    private fun subscribeOutputRetry() {
        if (isRemote || pendingCompressedAstralArrays <= 0) return
        outputSubscription = subscribeServerTick(outputSubscription) {
            if (getMachine().offsetTimer % 20L == 0L) flushPendingOutputs()
        }
    }

    fun flushPendingOutputs(): Boolean {
        if (pendingCompressedAstralArrays <= 0) return true
        if (machine.level !is ServerLevel) return false
        if (!outputCompressedAstralArrays(pendingCompressedAstralArrays)) {
            subscribeOutputRetry()
            return false
        }
        pendingCompressedAstralArrays = 0L
        onOwnedStateChanged()
        unsubscribe(outputSubscription)
        outputSubscription = null
        return true
    }

    /** Native machine-item components carry owned buffers without spawning an unbounded number of drops. */
    fun saveOwnedItemState(): CompoundTag = CompoundTag().also {
        it.putLong("collected", compressionAstralArrayCount)
        it.putLong("pending", pendingCompressedAstralArrays)
    }

    fun loadOwnedItemState(state: CompoundTag) {
        compressionAstralArrayCount = state.getLong("collected").coerceAtLeast(0)
        pendingCompressedAstralArrays = state.getLong("pending").coerceAtLeast(0)
        onOwnedStateChanged()
        if (machine.level is ServerLevel) subscribeOutputRetry()
    }

    private fun collectAstralArraysInCompressionArea() {
        val serverLevel = machine.level as? ServerLevel ?: return
        val center = getCompressionCenter()
        val radius = COMPRESSION_AREA_RADIUS
        val aabb = AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius))
        val entities = serverLevel.getEntitiesOfClass(ItemEntity::class.java, aabb) {
            it.item.`is`(GTLAddItems.ASTRAL_ARRAY.asItem())
        }

        for (entity in entities) {
            val accepted = minOf(entity.item.count.toLong(), Long.MAX_VALUE - compressionAstralArrayCount)
            if (accepted <= 0) break
            compressionAstralArrayCount += accepted
            entity.item.shrink(accepted.toInt())
            if (entity.item.isEmpty) entity.discard() else entity.item = entity.item.copy()
            onOwnedStateChanged()
        }
    }

    private fun onOwnedStateChanged() {
        // Persist entity ownership immediately; native markAsChanged only queues the next sync tick.
        machine.setChanged()
        syncDataHolder.markClientSyncFieldDirty("compressionAstralArrayCount")
        syncDataHolder.markClientSyncFieldDirty("pendingCompressedAstralArrays")
    }

    private fun calculateCompressionOutputs(batches: Long): Long {
        val chance = compressedAstralArrayOutputChance
        if (chance >= 1.0) return batches
        val expectedOutputs = BigDecimal.valueOf(batches).multiply(BigDecimal.valueOf(chance))
        val parts = expectedOutputs.divideAndRemainder(BigDecimal.ONE)
        val guaranteedOutputs = parts[0].longValueExact()
        val extraChance = parts[1].toDouble()
        val random = machine.level?.random ?: return 0L
        return guaranteedOutputs + if (random.nextDouble() < extraChance) 1L else 0L
    }

    private fun outputCompressedAstralArrays(amount: Long): Boolean {
        if (amount <= 0) return true
        val compressedAstralArray = GTLAddItems.COMPRESSED_ASTRAL_ARRAY.asItem()
        return MachineUtil.outputItem(getMachine(), LongIngredient.create(Ingredient.of(compressedAstralArray), amount))
    }

    private fun getCompressionCenter(): Vec3 {
        val machine = getMachine()
        val offset = getRotatedRenderPosition(Direction.NORTH, machine.frontFacing, 0.0, 0.0, COMPRESSION_FRONT_OFFSET)
        return Vec3.atLowerCornerOf(machine.blockPos).add(offset)
    }

    val compressedAstralArrayOutputChance: Double
        get() {
            val astralArrayCount = getMachine().astralArrayCount
            if (astralArrayCount >= MAX_CHANCE_ASTRAL_ARRAY_COUNT) return 1.0

            val progress = (
                astralArrayCount.toDouble() /
                    MAX_CHANCE_ASTRAL_ARRAY_COUNT.toDouble()
                ).coerceIn(0.0, 1.0)
            return BASE_COMPRESSION_OUTPUT_CHANCE +
                (1.0 - BASE_COMPRESSION_OUTPUT_CHANCE) * progress.pow(COMPRESSION_OUTPUT_CHANCE_EXPONENT)
        }

    companion object {
        // First embedded array count that makes the existing parallel formula reach Int.MAX_VALUE.
        private const val MAX_CHANCE_ASTRAL_ARRAY_COUNT = 42441
        private const val COMPRESSION_FRONT_OFFSET = 16.0
        private const val COMPRESSION_AREA_RADIUS = 11.5
        private const val BASE_COMPRESSION_OUTPUT_CHANCE = 0.3
        private const val COMPRESSION_OUTPUT_CHANCE_EXPONENT = 1.5

    }
}
