package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.logic.AddMutableRecipesLogic
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.MutableTierCasingMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.MethodsReturnNonnullByDefault
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.Registries
import java.math.BigInteger
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.*

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
open class TaixuTurbidArray(holder: BlockEntityCreationInfo) :
    MutableTierCasingMachine(holder, "SCTier", TaixuTurbidArrayLogic()),
    IAstralArrayInteractionMachine {

    override fun modifyDrops(drops: MutableList<net.minecraft.world.item.ItemStack>) {
        super.modifyDrops(drops)
        addAstralArrayDrops(drops)
    }

    @field:SaveField
    val machineStorage: NotifiableItemStackHandler

    @field:SaveField
    override var astralArrayCount: Int = 0

    private var coilType: ICoilType = CoilBlock.CoilType.CUPRONICKEL
    private var height: Int = 0

    // structure
    private var frameA: Double = 0.0
    private var frameB: Double = 0.0
    private var uuAmplifierAmount: Long = 0
    private var uuMatterAmount: Long = 0

    // itemStack
    private var maxParallel: Long = 0
    private var successRateA: Double = 0.0
    private var successRateB: Double = 0.0

    init {
        this.machineStorage = attachTrait(createMachineStorage())
    }

    private fun filter(itemStack: ItemStack): Boolean = VALID_ITEMS.contains(itemStack.item)

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            if (astralArrayCount == 0) getRecipeLogic().setUseMultipleRecipes(true)
            astralArrayCount += actualIncrease
            recalculateAstralParallel()
        }
        return actualIncrease
    }


    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    override fun getMaxParallel(): Int = Ints.saturatedCast(maxParallel)

    fun getRealParallel(): Long = maxParallel

    // ==================== Life Cycle ====================
    private fun createMachineStorage(): NotifiableItemStackHandler =
        NotifiableItemStackHandler(1, IO.NONE, IO.BOTH).setFilter(::filter).also {
            it.addChangedListener(::recalculateItemCache)
        }

    override fun beforeWorking(recipe: GTRecipe?): Boolean = true

    override fun onMachineDestroyed() { super.onMachineDestroyed() }

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        val matchedCoil = com.gtladd.gtladditions.api.machine.multiblock.NativeCoilStructure.match(this, substructureName)
            ?: return
        this.coilType = matchedCoil
        val data = org.gtlcore.gtlcore.api.pattern.PatternMatchData.of(defaultPatternState.context)
        height = data.blockCount("SpeedPipe") - 2

        recalculateAll()

        if (astralArrayCount > 0) getRecipeLogic().setUseMultipleRecipes(true)
    }

    // ==================== GUI ====================
    override fun buildMainUI(
        mainWidget: brachy.modularui.widget.ParentWidget<*>, guiData: brachy.modularui.factory.PosGuiData,
        syncManager: brachy.modularui.value.sync.PanelSyncManager, settings: brachy.modularui.screen.UISettings
    ) {
        super.buildMainUI(mainWidget, guiData, syncManager, settings)
        val group = brachy.modularui.widgets.slot.SlotGroup("taixuStorage", 1, false)
        syncManager.registerSlotGroup(group)
        mainWidget.child(brachy.modularui.widgets.slot.ItemSlot().right(10).bottom(10)
            .slot(brachy.modularui.value.sync.SyncHandlers.itemSlot(machineStorage.storage, 0).slotGroup(group))
            .tooltipDynamic { tooltip -> slotTooltips().forEach(tooltip::addLine) })
    }

    private fun slotTooltips(): MutableList<Component> = mutableListOf(
        "gtceu.machine.taixu.storage.tooltip.0".toComponent,
        "gtceu.machine.taixu.storage.tooltip.1".toComponent,
        "gtceu.machine.taixu.storage.tooltip.2".toComponent,
        "gtceu.machine.taixu.storage.tooltip.3".toComponent,
        "gtceu.machine.taixu.storage.tooltip.4".toComponent,
        "gtceu.machine.taixu.storage.tooltip.5".toComponent,
        "gtceu.machine.taixu.storage.tooltip.6".toComponent
    )

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("TaixuTurbidArrayStatus", syncManager, {
            mutableListOf<Component>().also(::collectTaixuTurbidArrayStatusText)
        }, 130)
    )

    private fun collectTaixuTurbidArrayStatusText(textList: MutableList<Component>) {
        
        if (isFormed) {
            textList.add("gtceu.machine.taixu.gui.tooltip.0".toComponent(height))
            textList.add("gtceu.machine.taixu.gui.tooltip.1".toComponent(maxParallel))

            if (tier > GTValues.UIV) {
                val df = DecimalFormat(".00'%'")
                textList.add("gtceu.machine.taixu.gui.tooltip.2".toComponent(df.format(successRateA)))
                textList.add("gtceu.machine.taixu.gui.tooltip.3".toComponent(uuAmplifierAmount))

                if (tier > GTValues.OpV) {
                    textList.add(
                        "gtceu.machine.taixu.gui.tooltip.4".toComponent(
                            df.format(
                                successRateB
                            )
                        )
                    )
                    textList.add("gtceu.machine.taixu.gui.tooltip.5".toComponent(uuMatterAmount))
                }
            }

            textList.add(
                "tooltip.gtladditions.astral_array_count".toComponent("$astralArrayCount / $MAX_ASTRAL_ARRAY_COUNT".literal.withStyle(ChatFormatting.GOLD))
            )
        }
    }

    // ==================== Calculation Methods ====================

    private fun recalculateAll() {
        frameA = calculateFrameA()
        frameB = calculateFrameB()
        uuAmplifierAmount = calculateBaseOutputFluid1()
        uuMatterAmount = calculateBaseOutputFluid2()
        recalculateItemCache()
        recalculateAstralParallel()
    }

    private fun recalculateItemCache() {
        val itemStack = machineStorage.storage.getStackInSlot(0)
        val item = itemStack.item
        val isAstralArray = ASTRAL_ARRAY_ITEM.`is`(item)

        val slotBonus = when {
            ENDERIUM_ITEM.`is`(item) -> ENDERIUM_BONUS * itemStack.count
            DRACONIUM_ITEM.`is`(item) -> DRACONIUM_BONUS * itemStack.count
            SPACETIME_ITEM.`is`(item) -> SPACETIME_BONUS * itemStack.count
            ETERNITY_ITEM.`is`(item) -> ETERNITY_BONUS * itemStack.count
            else -> 0.0
        }

        if (astralArrayCount <= 0) {
            maxParallel = (PARALLEL_BASE * PARALLEL_POWER_BASE.pow(coilType.coilTemperature / PARALLEL_TEMP_DIVISOR)).roundToLong()
        }

        successRateA = if (isAstralArray) {
            MAX_SUCCESS_RATE
        } else {
            MAX_SUCCESS_RATE / (
                1 + exp(
                    -SUCCESS_A_FACTOR *
                        (
                            frameA / SUCCESS_A_DIVISOR_A + frameB / SUCCESS_A_DIVISOR_B +
                                height / SUCCESS_A_HEIGHT_DIVISOR
                            )
                )
                ) + slotBonus
        }

        successRateB = if (isAstralArray) {
            MAX_SUCCESS_RATE
        } else {
            MAX_SUCCESS_RATE * (
                1 - exp(
                    -SUCCESS_B_FACTOR *
                        (
                            (frameA + frameB) / SUCCESS_B_DIVISOR +
                                cbrt(height.toDouble()) * tier / SUCCESS_B_HEIGHT_FACTOR
                            )
                )
                ) + slotBonus
        }
    }

    private fun recalculateAstralParallel() {
        if (astralArrayCount <= 0) return
        maxParallel = (ASTRAL_BASE_PARALLEL * astralArrayCount.toDouble().pow(ASTRAL_PARALLEL_EXPONENT)).roundToLong()
    }

    private fun calculateFrameA(): Double = FRAME_A_BASE * (FRAME_A_TIER_POWER.pow(casingTier.toDouble()) - 1) *
        sqrt((GTValues.ALL_TIERS[tier] + 1).toDouble())

    private fun calculateFrameB(): Double {
        val coilIndex = coil.value.indexOf(coilType.coilTemperature) + 1
        return FRAME_B_BASE * FRAME_B_POWER_BASE.pow(coilIndex.toDouble()) *
            (coilType.coilTemperature / FRAME_B_TEMP_DIVISOR).pow(FRAME_B_TEMP_POWER)
    }

    private fun calculateBaseOutputFluid1(): Long = (
        OUTPUT_1_BASE * tanh(
            OUTPUT_1_FACTOR *
                (
                    frameA * height / OUTPUT_1_HEIGHT_DIVISOR +
                        sqrt(frameB) * ln((tier + 2).toDouble())
                    )
        )
        ).toLong()

    private fun calculateBaseOutputFluid2(): Long = (
        OUTPUT_2_BASE * tanh(
            sqrt(frameA * frameB) *
                (height + tier) * OUTPUT_2_FACTOR / OUTPUT_2_DIVISOR
        )
        ).toLong()

    companion object {

        // Constants - Frame calculations
        private const val FRAME_A_BASE = 8.0
        private const val FRAME_A_TIER_POWER = 2.0
        private const val FRAME_B_BASE = 3.8
        private const val FRAME_B_POWER_BASE = 1.3
        private const val FRAME_B_TEMP_DIVISOR = 36000.0
        private const val FRAME_B_TEMP_POWER = 0.7

        // Constants - Success rate calculations
        private const val MAX_SUCCESS_RATE = 100.0
        private const val SUCCESS_A_FACTOR = 0.1
        private const val SUCCESS_A_DIVISOR_A = 50.0
        private const val SUCCESS_A_DIVISOR_B = 100.0
        private const val SUCCESS_A_HEIGHT_DIVISOR = 3.0
        private const val SUCCESS_B_FACTOR = 0.02
        private const val SUCCESS_B_DIVISOR = 20.0
        private const val SUCCESS_B_HEIGHT_FACTOR = 3.0

        // Constants - Output calculations
        private const val OUTPUT_1_BASE = 40960
        private const val OUTPUT_1_FACTOR = 0.007
        private const val OUTPUT_1_HEIGHT_DIVISOR = 9.0
        private const val OUTPUT_2_BASE = 22500
        private const val OUTPUT_2_FACTOR = 0.045
        private const val OUTPUT_2_DIVISOR = 200.0

        // Constants - Parallel calculations
        private const val PARALLEL_BASE = 4096.0
        private const val PARALLEL_POWER_BASE = 1.621
        private const val PARALLEL_TEMP_DIVISOR = 6400.0
        private const val ASTRAL_BASE_PARALLEL = 43046721L // 3^16
        private const val ASTRAL_PARALLEL_EXPONENT = 1.392

        // Constants - Slot bonuses
        private const val ENDERIUM_BONUS = 0.01
        private const val DRACONIUM_BONUS = 0.05
        private const val SPACETIME_BONUS = 0.1
        private const val ETERNITY_BONUS = 0.2

        // Constants - Recipe
        private const val RECIPE_DURATION = 100
        private const val RECIPE_EU_MULTIPLIER = 524288L

        private val coil: Lazy<Array<Int>> = lazyOf(
            GTCEuAPI.HEATING_COILS.keys
                .map { it.coilTemperature }
                .sorted()
                .toTypedArray()
        )

        private val ENDERIUM_ITEM: ItemStack = Registries.getItemStack("gtceu:enderium_nanoswarm", 64)
        private val DRACONIUM_ITEM: ItemStack = Registries.getItemStack("gtceu:draconium_nanoswarm", 64)
        private val SPACETIME_ITEM: ItemStack = Registries.getItemStack("gtceu:spacetime_nanoswarm", 64)
        private val ETERNITY_ITEM: ItemStack = Registries.getItemStack("gtceu:eternity_nanoswarm", 64)
        private val ASTRAL_ARRAY_ITEM: ItemStack = GTLAddItems.ASTRAL_ARRAY.asStack()
        private const val MAX_ASTRAL_ARRAY_COUNT = 2048

        private val VALID_ITEMS: Set<Item> = setOf(
            ENDERIUM_ITEM.item,
            DRACONIUM_ITEM.item,
            SPACETIME_ITEM.item,
            ETERNITY_ITEM.item,
            ASTRAL_ARRAY_ITEM.item
        )

        // ==================== Recipe Modifier ====================
        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe): GTRecipe? {
            if (machine !is TaixuTurbidArray) return null

            val maxParallel = IParallelLogic.getMaxParallel(machine, recipe, machine.maxParallel)
            if (maxParallel <= 0) return null

            return parallelWithFixedEutModifier(machine, outputWithoutEutModifier(machine, recipe), maxParallel)
        }

        private fun outputWithoutEutModifier(machine: TaixuTurbidArray, recipe: GTRecipe): GTRecipe {
            val modifiedRecipe = recipe.copy()

            // Build fluid outputs based on success rates
            val builder = GTRecipeBuilder(GTLAdditions.id("uu"), GTRecipeTypes.DUMMY_RECIPES.get())
            modifiedRecipe.outputs.put(FluidRecipeCapability.CAP, ObjectArrayList())

            val random = ThreadLocalRandom.current()

            // UU Amplifier output (UXV+)
            if (machine.tier >= GTValues.UXV &&
                random.nextDouble(MAX_SUCCESS_RATE) <= machine.successRateA
            ) {
                builder.outputFluids(org.gtlcore.gtlcore.common.recipe.GTLRecipeContents.fluid(GTLMaterials.UuAmplifier, machine.uuAmplifierAmount))
            }

            // UU Matter output (MAX+)
            if (machine.tier >= GTValues.MAX &&
                random.nextDouble(MAX_SUCCESS_RATE) <= machine.successRateB
            ) {
                builder.outputFluids(org.gtlcore.gtlcore.common.recipe.GTLRecipeContents.fluid(GTMaterials.UUMatter, machine.uuMatterAmount))
            }

            // Add generated outputs to recipe
            val generatedOutputs = builder.build().outputs[FluidRecipeCapability.CAP]
            if (generatedOutputs != null && generatedOutputs.isNotEmpty()) {
                modifiedRecipe.outputs[FluidRecipeCapability.CAP]!!.addAll(generatedOutputs)
            }

            // For fixed RecipeEut
            modifiedRecipe.tickInputs.clear()

            return modifiedRecipe
        }

        private fun parallelWithFixedEutModifier(machine: TaixuTurbidArray, modifiedRecipe: GTRecipe, maxParallel: Long): GTRecipe? {
            // Apply parallel limit based on output merging
            val minParallel = IParallelLogic.getMinParallel(machine, modifiedRecipe, maxParallel)
            if (minParallel <= 0) return null

            // Apply parallel multiplier, duration and eut
            val parallelRecipe = modifiedRecipe.copy(ContentModifier.multiplier(minParallel.toDouble()), false)
            parallelRecipe.duration = RECIPE_DURATION
            parallelRecipe.tickInputs[EURecipeCapability.CAP] =
                listOf(Content(RECIPE_EU_MULTIPLIER * GTValues.V[machine.tier], 10000, 10000))
            IGTRecipe.of(parallelRecipe).setHasTick(true)

            return parallelRecipe
        }

        class TaixuTurbidArrayLogic : AddMutableRecipesLogic<TaixuTurbidArray>() {

            override fun lookupRecipeSet(): Set<GTRecipe> {
                val taixu = getMachine()
                return if (isLock) {
                    when {
                        lockRecipe == null -> {
                            lockRecipe = machine.recipeType.searchRecipe(machine, this::matchesSourceRecipe).asSequence().firstOrNull()
                            lockRecipe?.let { Collections.singleton(outputWithoutEutModifier(taixu, it)) } ?: emptySet()
                        }
                        matchesSourceRecipe(lockRecipe) -> Collections.singleton(outputWithoutEutModifier(taixu, lockRecipe))
                        else -> emptySet()
                    }
                } else {
                    machine.recipeType.searchRecipe(machine, this::matchesSourceRecipe)
                        .asSequence()
                        .map { outputWithoutEutModifier(taixu, it) }
                        .toCollection(ObjectOpenHashSet())
                }
            }

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeSet()
                val totalParallel = NumberUtils.saturatedMultiply(getMachine().maxParallel, getMultipleThreads().toLong())

                return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
                    recipes,
                    totalParallel
                ) { recipe ->
                    LongBooleanPair.of(calculateParallel(machine, recipe, totalParallel).firstLong(), true)
                }
            }

            override fun buildFinalNormalRecipe(parallelData: ParallelData): GTRecipe? {
                val maxEUt = getMachine().overclockVoltage
                val (itemOutputs, fluidOutputs) = RecipeCalculationHelper.processParallelDataNormal(
                    parallelData,
                    machine,
                    maxEUt,
                    euMultiplier,
                    { getRecipeEut(it).toDouble() * it.duration }
                )

                if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
                    if (recipeStatus == null || recipeStatus.isSuccess) {
                        RecipeResult.of(
                            this.machine,
                            RecipeResult.FAIL_FIND
                        )
                    }
                    return null
                }

                val totalEu = RECIPE_EU_MULTIPLIER * GTValues.V[getMachine().tier].toDouble() * RECIPE_DURATION
                return RecipeCalculationHelper.buildNormalRecipe(
                    itemOutputs,
                    fluidOutputs,
                    if (isMultipleRecipeMode()) totalEu * 16 else totalEu,
                    maxEUt,
                    20
                )
            }

            override fun buildFinalWirelessRecipe(
                parallelData: ParallelData,
                wirelessTrait: IWirelessNetworkEnergyHandler
            ): WirelessGTRecipe? {
                if (!wirelessTrait.isOnline) return null

                val (itemOutputs, fluidOutputs) = RecipeCalculationHelper.processParallelDataWireless(
                    parallelData,
                    machine,
                    wirelessTrait.maxAvailableEnergy,
                    euMultiplier,
                    this::getRecipeEut
                )

                if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
                    if (recipeStatus == null || recipeStatus.isSuccess) {
                        RecipeResult.of(
                            this.machine,
                            RecipeResult.FAIL_FIND
                        )
                    }
                    return null
                }

                val totalEu = RECIPE_EU_MULTIPLIER * GTValues.V[getMachine().tier] * RECIPE_DURATION
                return RecipeCalculationHelper.buildWirelessRecipe(
                    itemOutputs,
                    fluidOutputs,
                    20,
                    if (isMultipleRecipeMode()) BigInteger.valueOf(totalEu * 16) else BigInteger.valueOf(totalEu)
                )
            }

            override fun getMultipleThreads(): Int = 128
        }
    }
}