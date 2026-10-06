package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeOutput
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipeOutput
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers
import org.gtlcore.gtlcore.utils.Registries.getItem
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.BiPredicate

class BiologicalSimulationLaboratory(holder: BlockEntityCreationInfo) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, BiologicalSimulationLaboratoryLogic()) {

    @field:SaveField
    val machineStorage: NotifiableItemStackHandler = attachTrait(createMachineStorage()).also {
        it.addChangedListener(::recalculateParameters)
    }

    private var reductionEUt = 1.0
    private var reductionDuration = 1.0
    private var maxParallels = 64
    private var isMultiRecipe = false

    fun createMachineStorage(): NotifiableItemStackHandler {
        val handler = NotifiableItemStackHandler(1, IO.NONE, IO.BOTH) { _ ->
            object : com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler(1) {
                override fun getSlotLimit(slot: Int): Int = 1
            }
        }
        handler.setFilter(::filter)
        return handler
    }


    override fun getRecipeLogic(): BiologicalSimulationLaboratoryLogic = super.getRecipeLogic() as BiologicalSimulationLaboratoryLogic

    override fun formStructure(substructureName: String) {
        super.formStructure(substructureName)
        if (substructureName != DEFAULT_STRUCTURE || !isFormed) return
        recalculateParameters()
    }

    fun filter(itemStack: ItemStack): Boolean {
        val item = itemStack.item
        return NAN_CERTIFICATE.`is`(item) ||
            INFUSCOLIUM_NANOSWARM.`is`(item) ||
            ORICHALCUM_NANOSWARM.`is`(item) ||
            RHENIUM_NANOSWARM.`is`(
                item
            )
    }

    fun isMultiRecipeEnabled(): Boolean = isMultiRecipe

    override fun buildMainUI(main: brachy.modularui.widget.ParentWidget<*>,
                             data: brachy.modularui.factory.PosGuiData,
                             manager: brachy.modularui.value.sync.PanelSyncManager,
                             settings: brachy.modularui.screen.UISettings) {
        super.buildMainUI(main, data, manager, settings)
        val group = brachy.modularui.widgets.slot.SlotGroup("biologicalCatalyst", 1)
        manager.registerSlotGroup(group)
        main.child(brachy.modularui.widgets.slot.ItemSlot()
            .slot(brachy.modularui.widgets.slot.ModularSlot(machineStorage.storage, 0).slotGroup(group))
            .right(10).bottom(10))
    }

    override fun getWidgetsForDisplay(
        syncManager: brachy.modularui.value.sync.PanelSyncManager
    ): List<brachy.modularui.api.widget.IWidget> = super.getWidgetsForDisplay(syncManager) + listOf(
        com.gtladd.gtladditions.api.gui.NativeHatchUI.componentList("BiologicalSimulationLaboratoryStatus", syncManager, {
            mutableListOf<Component>().also(::collectBiologicalSimulationLaboratoryStatusText)
        }, 130)
    )

    private fun collectBiologicalSimulationLaboratoryStatusText(textList: MutableList<Component>) {
        
        if (this.isFormed) {
            textList.add(("gtceu.machine.biological_simulation_laboratory.gui.tooltip." + if (isMultiRecipe) 1 else 0).toComponent)
            textList.add(
                "gtceu.machine.eut_multiplier.tooltip".toComponent(FormattingUtil.formatNumbers(reductionEUt).toComponent)
            )
            textList.add(
                "gtceu.machine.duration_multiplier.tooltip".toComponent(FormattingUtil.formatNumbers(reductionDuration).toComponent)
            )
        }
    }

    private fun recalculateParameters() {
        val item = machineStorage.storage.getStackInSlot(0).item
        when {
            RHENIUM_NANOSWARM.`is`(item) -> setMachineParameters(false, 2048, 0.9, 0.9)
            ORICHALCUM_NANOSWARM.`is`(item) -> setMachineParameters(false, 16384, 0.8, 0.6)
            INFUSCOLIUM_NANOSWARM.`is`(item) -> setMachineParameters(false, 262144, 0.6, 0.4)
            NAN_CERTIFICATE.`is`(item) -> setMachineParameters(true, 4194304, 0.25, 0.1)
            else -> setMachineParameters(false, 64, 1.0, 1.0)
        }
    }

    private fun setMachineParameters(isMultiRecipe: Boolean, maxParallel: Int, reductionEut: Double, reductionDuration: Double) {
        this@BiologicalSimulationLaboratory.isMultiRecipe = isMultiRecipe
        maxParallels = maxParallel
        reductionEUt = reductionEut
        this@BiologicalSimulationLaboratory.reductionDuration = reductionDuration
    }


    override fun getMaxParallel(): Int = maxParallels

    class BiologicalSimulationLaboratoryLogic() : GTLAddMultipleRecipesLogic(BEFORE_RECIPE) {

        override fun getMachine(): BiologicalSimulationLaboratory = super.getMachine() as BiologicalSimulationLaboratory

        override fun findAndHandleRecipe() {
            lastRecipe = null
            recipeStatus = null
            val match = if (this.isNanCertificate) {
                getGTRecipe()
            } else {
                this.oneRecipe
            }
            if (match != null && matchRecipeOutput(this.machine, match)) {
                setupRecipe(match)
            }
        }

        val isNanCertificate: Boolean
            get() {
                val item = getMachine().machineStorage.storage.getStackInSlot(0)
                return item.item == getItem("gtceu:nan_certificate")
            }

        val oneRecipe: GTRecipe?
            get() {
                if (!machine.hasCapabilityProxies()) return null
                val lab = getMachine()
                var recipe = machine.recipeType.searchRecipe(machine, this::matchesSourceRecipe).asSequence().firstOrNull() ?: return null
                recipe = org.gtlcore.gtlcore.api.recipe.CoreRecipeModifiers.accurateParallel(
                    lab,
                    recipe,
                    parallel.maxParallel,
                    false
                ).first ?: return null
                return GTLRecipeModifiers.reduction(
                    lab,
                    recipe,
                    lab.reductionEUt,
                    lab.reductionDuration
                )
            }

        override fun onRecipeFinish() {
            machine.afterWorking()
            lastRecipe?.let { handleRecipeOutput(this.machine, it) }

            val suspendableMachine = machine as? ISuspendableMachine
            if (suspendableMachine?.`gtlcore$isSuspendAfterFinish`() == true) {
                status = Status.SUSPEND
                suspendableMachine.`gtlcore$setSuspendAfterFinish`(false)
            } else {
                (if (this.isNanCertificate) getGTRecipe() else this.oneRecipe)?.takeIf { matchRecipeOutput(machine, it) }?.let {
                    setupRecipe(it)
                    return
                }
                status = Status.IDLE
            }
            progress = 0
            duration = 0
        }
    }

    companion object {
        private val RHENIUM_NANOSWARM: ItemStack = getItemStack("gtceu:rhenium_nanoswarm")
        private val ORICHALCUM_NANOSWARM: ItemStack = getItemStack("gtceu:orichalcum_nanoswarm")
        private val INFUSCOLIUM_NANOSWARM: ItemStack = getItemStack("gtceu:infuscolium_nanoswarm")
        private val NAN_CERTIFICATE: ItemStack = GTItems.NAN_CERTIFICATE.asStack()

        private val BEFORE_RECIPE: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe: GTRecipe, machine: IRecipeLogicMachine ->
                val lab = machine as? BiologicalSimulationLaboratory ?: return@BiPredicate false
                val input = RecipeHelper.getInputItems(recipe)
                for (stack in input) {
                    if (stack.item == getItem("avaritia:infinity_sword") && !lab.isMultiRecipeEnabled()) {
                        RecipeResult.of(
                            machine,
                            RecipeResult.fail(
                                "gtceu.machine.biological_simulation_laboratory.recipe.tooltip.0".toComponent
                            )
                        )
                        return@BiPredicate false
                    }
                }
                true
            }

    }
}