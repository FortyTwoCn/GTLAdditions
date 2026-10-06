package com.gtladd.gtladditions.common.machine.multiblock.part

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.machine.wireless.NativeWirelessEnergyAccount
import java.math.BigInteger

open class WirelessEnergyNetworkTerminalPartMachine(holder: BlockEntityCreationInfo, io: IO) : WirelessEnergyNetworkTerminalPartMachineBase(holder, io) {
    protected val wirelessNetworkEnergyHandler: IWirelessNetworkEnergyHandler

    init {
        this.wirelessNetworkEnergyHandler = attachTrait(WirelessNetworkEnergyHandler())
    }

    override fun addedToController(controller: MultiblockControllerMachine, substructureName: String) {
        super.addedToController(controller, substructureName)
        if (controller is IWirelessElectricMultiblockMachine) {
            controller.setWirelessNetworkEnergyHandler(
                wirelessNetworkEnergyHandler
            )
        }
    }

    protected inner class WirelessNetworkEnergyHandler :
        MachineTrait(),
        IWirelessNetworkEnergyHandler {
        override fun getMachine(): WirelessEnergyNetworkTerminalPartMachine = super.getMachine() as WirelessEnergyNetworkTerminalPartMachine

        override fun consumeEnergy(energy: Int): Boolean = uuid != null && NativeWirelessEnergyAccount.transfer(uuid, energy, machine)

        override fun consumeEnergy(energy: Long): Boolean = uuid != null && NativeWirelessEnergyAccount.transfer(uuid, energy, machine)

        override fun consumeEnergy(energy: BigInteger): Boolean = uuid != null && NativeWirelessEnergyAccount.transfer(uuid, energy, machine)

        override val maxAvailableEnergy: BigInteger
            get() = if (uuid != null) NativeWirelessEnergyAccount.balance(uuid) else BigInteger.ZERO

        override val isOnline: Boolean
            get() = uuid != null && (io == IO.OUT || NativeWirelessEnergyAccount.balance(uuid).signum() > 0)

    }
}
