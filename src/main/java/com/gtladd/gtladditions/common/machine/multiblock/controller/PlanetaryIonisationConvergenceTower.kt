package com.gtladd.gtladditions.common.machine.multiblock.controller

import appeng.client.render.effects.ParticleTypes
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.sound.AutoReleasedSound
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.translatable
import com.gtladd.gtladditions.utils.MachineUtil.input
import net.neoforged.neoforge.fluids.FluidStack
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine
import org.gtlcore.gtlcore.utils.Registries
import kotlin.enums.enumEntries
import kotlin.math.min

class PlanetaryIonisationConvergenceTower(holder: BlockEntityCreationInfo) :
    StorageMachine(holder, 64, PICTRecipeLogic()) {

    @SaveField
    private var storageEUt = 0L

    @SaveField
    private var cycleAmount = 0

    @SaveField
    private var startCycle = false

    private var coilEnergy: CoilToEnergy? = null
    private var stellarTier = 0
    private var maxStorageEUt = 0L
    private var particlePos: BlockPos? = null

    init {
        isWorkingEnabled = false
    }

    override fun filter(itemStack: ItemStack): Boolean {
        val requiredDrone = when (coilEnergy?.workTier) {
            1 -> SPACE_DRONE_MK2
            2 -> SPACE_DRONE_MK4
            3 -> SPACE_DRONE_MK6
            else -> null
        }
        return requiredDrone != null && itemStack.`is`(requiredDrone)
    }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        val coilType = com.gtladd.gtladditions.api.machine.multiblock.NativeCoilStructure.match(this, substructureName) ?: return
        stellarTier = org.gtlcore.gtlcore.api.pattern.PatternMatchData.of(defaultPatternState.context).tier("SCTier").orElse(0)
        coilEnergy = findCoil(coilType.material)
        maxStorageEUt = when (stellarTier) {
            1 -> 0x3297345d9a000
            2 -> 0x1d4f54cf65a0000
            3 -> 0x15c9c2ae895d0000
            else -> 0L
        }
        particlePos = blockPos.offset(
            when (frontFacing) {
                Direction.WEST -> 7
                Direction.EAST -> -7
                else -> 0
            },
            20,
            when (frontFacing) {
                Direction.SOUTH -> -7
                Direction.NORTH -> 7
                else -> 0
            }
        )
    }

    override fun invalidateStructure(substructureName: String) {
        super.invalidateStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE) return
        coilEnergy = null
        stellarTier = 0
        cycleAmount = 0
        maxStorageEUt = 0
        particlePos = null
    }

    override fun onPartUnload() {
        super.onPartUnload()
        coilEnergy = null
        stellarTier = 0
        cycleAmount = 0
        maxStorageEUt = 0
        particlePos = null
    }



    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = definition.additionalDisplay.apply(this, syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("PlanetaryIonisationConvergenceTowerStatus", syncManager, {
            mutableListOf<Component>().also(::collectPlanetaryIonisationConvergenceTowerStatusText)
        }, 130)
    )

    private fun collectPlanetaryIonisationConvergenceTowerStatusText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed)
            .setWorkingStatus(recipeLogic.isWorkingEnabled, recipeLogic.isActive)
            .addEnergyUsageLine(energyContainer)
            .addEnergyTierLine(tier)
            .addWorkingStatusLine()
            .addProgressLine(recipeLogic.progressPercent)
            .addComponent(
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_1", (coilEnergy?.material?.unlocalizedName ?: "").translatable),
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_2", this.stellarTier),
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_3", FormattingUtil.formatNumbers(this.storageEUt), "EU")
            )
            .addRecipeStatus(recipeLogic as IRecipeStatus)
    }

    override fun getRecipeLogic(): PICTRecipeLogic = super.getRecipeLogic() as PICTRecipeLogic

    private val sphereOffsetCache = mutableMapOf<Int, IntArray>()

    private fun getSphereOffsets(radius: Int): IntArray = sphereOffsetCache.getOrPut(radius) {
        val list = ArrayList<Int>((4.19 * radius * radius * radius).toInt() + 100)
        val radiusSquared = radius * radius
        for (dx in -radius..radius) {
            val dxSquared = dx * dx
            for (dy in -radius..radius) {
                val dxDySquared = dxSquared + dy * dy
                for (dz in -radius..radius) {
                    if (dxDySquared + dz * dz <= radiusSquared) {
                        list.add(((dx + radius) shl 16) or ((dy + radius) shl 8) or (dz + radius))
                    }
                }
            }
        }
        list.toIntArray()
    }

    fun doExplosion(pos: BlockPos, explosionPower: Float) {
        val currentLevel = level ?: return
        currentLevel.removeBlock(this.blockPos, false)

        val radius = explosionPower.toInt()
        if (radius <= 0) return

        val mutablePos = BlockPos.MutableBlockPos()
        val airState = Blocks.AIR.defaultBlockState()
        val minResistanceToKeep = 3_600_000f

        for (encodedOffset in getSphereOffsets(radius)) {
            val dx = (encodedOffset shr 16 and 0xFF) - radius
            val dy = (encodedOffset shr 8 and 0xFF) - radius
            val dz = (encodedOffset and 0xFF) - radius

            mutablePos.set(pos.x + dx, pos.y + dy, pos.z + dz)
            val state = currentLevel.getBlockState(mutablePos)
            if (state.isAir) continue

            val resistance = state.block.getExplosionResistance(state, currentLevel, mutablePos, null)
            if (resistance >= minResistanceToKeep) continue

            currentLevel.setBlock(mutablePos, airState, 2)
        }
    }

    private fun addStorageEUt(start: Boolean): Long {
        val addEUt = if (start) coilEnergy?.instantPower ?: 0 else coilEnergy?.dischargePower ?: 0
        storageEUt += addEUt
        if (storageEUt > maxStorageEUt) {
            doExplosion(blockPos, 80f)
        }
        return addEUt
    }

    private fun renderParticles() {
        val targetPos = particlePos ?: return
        val serverLevel = level as? ServerLevel ?: return
        serverLevel.sendParticles(
            ParticleTypes.LIGHTNING,
            targetPos.x.toDouble(),
            targetPos.y.toDouble(),
            targetPos.z.toDouble(),
            200,
            4.0,
            4.0,
            4.0,
            0.01
        )
    }

    enum class CoilToEnergy(
        val material: Material,
        val instantPower: Long,
        val dischargePower: Long,
        val workTier: Int
    ) {
        TITAN_STEEL(TitanSteel, 0x7fffffff000, 0x7fffffff0, 1),
        ADAMANTINE(Adamantine, 0x3fffffff8000, 0x3fffffff80, 1),
        NAQUADRIATIC_TARANIUM(NaquadriaticTaranium, 0x3fffffff80000, 0x7fffffff00, 2),
        STAR_METAL(Starmetal, 0x1ffffffffc0000, 0x3fffffff800, 2),
        INFINITY(Infinity, 0x3fffffff80000, 0x7fffffff000, 3),
        HYPOGEN(Hypogen, 0x1ffffffffc00000, 0x3fffffff8000, 3),
        ETERNITY(Eternity, 0x7fffffff0000000, 0xfffffffe0000, 3)
    }

    companion object {

        private const val DURATION = 60

        private val SPACE_DRONE_MK2 = Registries.getItem("kubejs:space_drone_mk2")
        private val SPACE_DRONE_MK4 = Registries.getItem("kubejs:space_drone_mk4")
        private val SPACE_DRONE_MK6 = Registries.getItem("kubejs:space_drone_mk6")

        private val RHENIUM = Rhenium.getFluid(73_728)
        private val ICE = Ice.getFluid(8_000_000)
        private val PROMETHIUM = Promethium.getFluid(36_864)
        private val HELIUM = Helium.getFluid(FluidStorageKeys.LIQUID, 4_000_000)
        private val CRYSTALMATRIX = Crystalmatrix.getFluid(9_216)
        private val CRYOTHEUM = FluidStack(Registries.getFluid("kubejs:gelid_cryotheum"), 1_000_000)

        private fun findCoil(material: Material?): CoilToEnergy? =
            enumEntries<CoilToEnergy>().find { it.material == material }

        class PICTRecipeLogic :
            RecipeLogic(),
            IRecipeStatus {

            override fun getMachine(): PlanetaryIonisationConvergenceTower = super.getMachine() as PlanetaryIonisationConvergenceTower
            private val pictMachine: PlanetaryIonisationConvergenceTower get() = getMachine()

            init {
                duration = DURATION
            }

            override fun serverTick() {
                if (!isSuspend) {
                    if (progress < DURATION) {
                        handleRecipeWorking()
                    }
                    if (progress >= DURATION) {
                        pictMachine.startCycle = false
                        progress = 0
                    }
                } else if (subscription != null) {
                    subscription?.unsubscribe()
                    subscription = null
                }
            }

            override fun handleRecipeWorking() {
                workingStatus = null
                pictMachine.renderParticles()
                if (progress == 0) {
                    val droneResult = DroneResult(false, "")
                    val nextCycle = pictMachine.cycleAmount + 1
                    pictMachine.coilEnergy?.let {
                        when (it.workTier) {
                            1 -> {
                                when {
                                    (nextCycle % 10000 == 0 || pictMachine.cycleAmount == 0) && !pictMachine.machineStorageItem.`is`(SPACE_DRONE_MK2) -> {
                                        pictMachine.startCycle = false
                                        droneResult.isDrone = false
                                        droneResult.tier = SPACE_DRONE_MK2.descriptionId
                                    }
                                    else -> {
                                        pictMachine.startCycle =
                                            input(pictMachine).fluid(RHENIUM).fluid(ICE).execute()
                                        if (pictMachine.startCycle) {
                                            if (nextCycle % 10000 == 0 || pictMachine.cycleAmount == 0) {
                                                pictMachine.machineStorage.extractItemInternal(0, 1, false)
                                                droneResult.isDrone = true
                                                droneResult.tier = SPACE_DRONE_MK2.descriptionId
                                            }
                                            pictMachine.cycleAmount = nextCycle
                                        }
                                    }
                                }
                            }
                            2 -> {
                                when {
                                    (nextCycle % 20000 == 0 || pictMachine.cycleAmount == 0) && !pictMachine.machineStorageItem.`is`(SPACE_DRONE_MK4) -> {
                                        pictMachine.startCycle = false
                                        droneResult.isDrone = false
                                        droneResult.tier = SPACE_DRONE_MK4.descriptionId
                                    }
                                    else -> {
                                        pictMachine.startCycle =
                                            input(pictMachine).fluid(PROMETHIUM).fluid(HELIUM).execute()
                                        if (pictMachine.startCycle) {
                                            if (nextCycle % 20000 == 0 || pictMachine.cycleAmount == 0) {
                                                pictMachine.machineStorage.extractItemInternal(0, 1, false)
                                                droneResult.isDrone = true
                                                droneResult.tier = SPACE_DRONE_MK4.descriptionId
                                            }
                                            pictMachine.cycleAmount = nextCycle
                                        }
                                    }
                                }
                            }
                            3 -> {
                                when {
                                    (nextCycle % 100000 == 0 || pictMachine.cycleAmount == 0) && !pictMachine.machineStorageItem.`is`(SPACE_DRONE_MK6) -> {
                                        pictMachine.startCycle = false
                                        droneResult.isDrone = false
                                        droneResult.tier = SPACE_DRONE_MK6.descriptionId
                                    }
                                    else -> {
                                        pictMachine.startCycle =
                                            input(pictMachine).fluid(CRYSTALMATRIX).fluid(CRYOTHEUM).execute()
                                        if (pictMachine.startCycle) {
                                            if (nextCycle % 100000 == 0 || pictMachine.cycleAmount == 0) {
                                                pictMachine.machineStorage.extractItemInternal(0, 1, false)
                                                droneResult.isDrone = true
                                                droneResult.tier = SPACE_DRONE_MK6.descriptionId
                                            }
                                            pictMachine.cycleAmount = nextCycle
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!droneResult.isDrone && !droneResult.tier.isEmpty()) {
                        workingStatus = RecipeResult.fail("gui.gtladditions.planetary_ionisation_convergence_tower_4".toComponent.append(droneResult.tier.toComponent))
                    }
                }
                if (pictMachine.startCycle) {
                    val actualEUt = min(
                        pictMachine.energyContainer?.energyCanBeInserted ?: 0L,
                        pictMachine.addStorageEUt(progress == 0)
                    )
                    pictMachine.energyContainer?.addEnergy(actualEUt)
                    pictMachine.storageEUt -= actualEUt
                    this.status = Status.WORKING
                    ++this.progress
                } else {
                    this.setWaiting(null)
                }
                if (this.status == Status.WAITING) {
                    this.regressRecipe()
                }
            }

            @OnlyIn(Dist.CLIENT)
            override fun updateSound() {
                if (isWorking && pictMachine.shouldWorkingPlaySound()) {
                    val sound = GTSoundEntries.ARC.get()
                    if (workingSound is AutoReleasedSound) {
                        val soundEntry = workingSound as AutoReleasedSound
                        if (soundEntry.soundEntry == sound && !soundEntry.isStopped) {
                            return
                        }
                        soundEntry.release()
                        workingSound = null
                    }
                    if (sound != null) {
                        workingSound = sound.playAutoReleasedSound(
                            {
                                pictMachine.shouldWorkingPlaySound() &&
                                    isWorking &&
                                    !pictMachine.isRemoved &&
                                    pictMachine.level!!.isLoaded(pictMachine.blockPos) &&
                                    getMachine(
                                        pictMachine.level!!,
                                        pictMachine.blockPos
                                    ) == pictMachine
                            },
                            pictMachine.blockPos,
                            true,
                            0,
                            1f,
                            1f
                        )
                    }
                } else if (workingSound is AutoReleasedSound) {
                    (workingSound as AutoReleasedSound).release()
                    workingSound = null
                }
            }

            private data class DroneResult(var isDrone: Boolean, var tier: String)
        }
    }
}
