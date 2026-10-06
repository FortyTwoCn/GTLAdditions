package com.gtladd.gtladditions.mixin.gtceu.api.machine;

import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Cross-type recipe machines use their own recipe policy instead of the normal mode selector. */
@Mixin(MachineUIPanelBuilder.class)
public abstract class MachineUIRecipeModesMixin {
    @Redirect(method = "build", at = @At(value = "INVOKE", target =
            "Lcom/gregtechceu/gtceu/common/mui/GTMuiWidgets;createRecipeTypeButton(Lcom/gregtechceu/gtceu/api/machine/multiblock/WorkableElectricMultiblockMachine;Lbrachy/modularui/value/sync/PanelSyncManager;)Lbrachy/modularui/widgets/ButtonWidget;"), remap = false, require = 1)
    private ButtonWidget<?> gtladditions$recipeMode(WorkableElectricMultiblockMachine machine, PanelSyncManager sync) {
        return GTMuiWidgets.createRecipeTypeButton(machine, sync)
                .setEnabledIf(widget -> !GTLAddRecipesTypes.isMultiRecipeType(machine.getRecipeType()));
    }
}
