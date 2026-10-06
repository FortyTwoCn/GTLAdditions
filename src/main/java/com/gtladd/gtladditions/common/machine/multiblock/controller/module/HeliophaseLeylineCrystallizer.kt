package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist.Companion.MAX_EFFICIENCY_SEC
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import org.gtlcore.gtlcore.api.recipe.RecipeResult

class HeliophaseLeylineCrystallizer(holder: BlockEntityCreationInfo, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(holder, HeliophaseLeylineCrystallizerLogic()) {


    override fun getRecipeLogic(): HeliophaseLeylineCrystallizerLogic = super<ForgeOfTheAntichristModuleBase>.getRecipeLogic() as HeliophaseLeylineCrystallizerLogic

    companion object {

        val FAIL_HOST_NOT_ACHIEVE_MAX_EFFICIENCY: RecipeResult = RecipeResult.fail(
            "gtladditions.recipe.fail.host_not_achieve_max_efficiency".toComponent
        )

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            val module = machine as ForgeOfTheAntichristModuleBase
            val host = module.host

            when {
                host == null -> {
                    RecipeResult.of(machine, FAIL_HOST_NOT_WORKING)
                    false
                }
                !host.isActiveOrStasisAnchored() -> {
                    RecipeResult.of(machine, FAIL_HOST_NOT_WORKING)
                    false
                }
                host.runningSecs < MAX_EFFICIENCY_SEC -> {
                    RecipeResult.of(machine, FAIL_HOST_NOT_ACHIEVE_MAX_EFFICIENCY)
                    false
                }
                else -> true
            }
        }

        class HeliophaseLeylineCrystallizerLogic(
            
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(BEFORE_WORKING) {
            init {
                this.setReduction(256.0, 1.0)
            }

            override fun getMachine(): HeliophaseLeylineCrystallizer = super.getMachine() as HeliophaseLeylineCrystallizer
        }
    }
}