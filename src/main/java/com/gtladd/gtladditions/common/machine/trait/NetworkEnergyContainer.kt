package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.capability.IEnergyContainer
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableRecipeHandlerTrait
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack
import com.gtladd.gtladditions.common.machine.multiblock.part.WirelessEnergyNetworkTerminalPartMachineBase
import com.gtladd.gtladditions.api.machine.wireless.NativeWirelessEnergyAccount
import net.minecraft.core.Direction
import org.gtlcore.gtlcore.api.capability.IInt128EnergyContainer
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.datastructure.Int128
import java.math.BigInteger

class NetworkEnergyContainer(private val handlerIO: IO) : NotifiableRecipeHandlerTrait<EnergyStack>(),
    IEnergyContainer,
    IInt128EnergyContainer {

    private val energyCapacity: Long = Long.MAX_VALUE
    private val inputVoltage: Long
    private val inputAmperage: Long
    private val outputVoltage: Long
    private val outputAmperage: Long

    private var updateSubs: TickableSubscription? = null
    private var lastEnergyInputPerSec = Int128.ZERO()
    private var lastEnergyOutputPerSec = Int128.ZERO()
    private var energyInputPerSec = Int128.ZERO()
    private var energyOutputPerSec = Int128.ZERO()

    init {
        when (handlerIO) {
            IO.IN -> {
                inputVoltage = Long.MAX_VALUE
                inputAmperage = 1
                outputVoltage = 0
                outputAmperage = 0
            }
            IO.OUT -> {
                inputVoltage = 0
                inputAmperage = 0
                outputVoltage = Long.MAX_VALUE
                outputAmperage = 1
            }
            IO.BOTH -> {
                inputVoltage = Long.MAX_VALUE
                inputAmperage = 1
                outputVoltage = Long.MAX_VALUE
                outputAmperage = 1
            }
            else -> {
                inputVoltage = 0
                inputAmperage = 0
                outputVoltage = 0
                outputAmperage = 0
            }
        }
    }

    override fun getMachine(): WirelessEnergyNetworkTerminalPartMachineBase = super.getMachine() as WirelessEnergyNetworkTerminalPartMachineBase

    override fun onMachineLoad() {
        super.onMachineLoad()
        updateSubs = machine.subscribeServerTick(updateSubs, ::updateTick)
    }

    override fun onMachineUnload() {
        super.onMachineUnload()
        updateSubs?.unsubscribe()
        updateSubs = null
    }

    private fun updateTick() {
        if (machine.offsetTimer % 20 == 0L) {
            lastEnergyOutputPerSec = energyOutputPerSec.copy()
            lastEnergyInputPerSec = energyInputPerSec.copy()
            energyOutputPerSec.set(0, 0)
            energyInputPerSec.set(0, 0)
        }
    }

    override fun changeEnergy(energyToAdd: Long): Long {
        val result = if (getMachine().getUUID() != null) {
            if (NativeWirelessEnergyAccount.transfer(getMachine().getUUID(), energyToAdd, getMachine())) {
                energyToAdd
            } else {
                0
            }
        } else {
            0
        }
        addEnergyPerSec(result)
        return result
    }

    override fun handleRecipeInner(
        io: IO,
        recipe: GTRecipe?,
        left: MutableList<EnergyStack>,
        simulate: Boolean
    ): MutableList<EnergyStack> {
        if (!handlerIO.support(io) || (io != IO.IN && io != IO.OUT)) return left
        val account = NativeWirelessEnergyAccount.find(getMachine().getUUID()) ?: return left
        val rate = NativeWirelessEnergyAccount.transferLimit(account)
        var budget = if (io == IO.IN) account.storage else left.fold(BigInteger.ZERO) { sum, stack -> sum + exactEU(stack) }
        if (rate != null) budget = budget.min(rate)
        val remaining = ArrayList<EnergyStack>()
        for (stack in left) {
            val required = exactEU(stack)
            var accepted = required.min(budget)
            if (!simulate && accepted.signum() > 0) {
                val signed = if (io == IO.IN) accepted.negate() else accepted
                if (NativeWirelessEnergyAccount.transfer(getMachine().getUUID(), signed, getMachine())) {
                    addEnergyPerSecExact(signed)
                    notifyListeners()
                } else accepted = BigInteger.ZERO
            }
            budget -= accepted
            val rest = required - accepted
            if (rest.signum() > 0) {
                val parts = rest.divideAndRemainder(BigInteger.valueOf(stack.voltage()))
                if (parts[0].signum() > 0) remaining.add(EnergyStack(stack.voltage(), parts[0].longValueExact()))
                if (parts[1].signum() > 0) remaining.add(EnergyStack(parts[1].longValueExact()))
            }
        }
        return remaining
    }

    private fun exactEU(stack: EnergyStack): BigInteger = BigInteger.valueOf(stack.voltage()).multiply(BigInteger.valueOf(stack.amperage()))

    private fun addEnergyPerSecExact(energy: BigInteger) {
        val target = if (energy.signum() < 0) energyInputPerSec else energyOutputPerSec
        val value = energy.abs()
        target.add(Int128(value.shiftRight(64).toLong(), value.toLong()))
    }

    override fun getEnergyStored(): Long = if (handlerIO == IO.IN) {
        getMachine().getUUID()?.let { uuid ->
            NumberUtils.getLongValue(NativeWirelessEnergyAccount.balance(uuid))
        } ?: 0
    } else {
        0
    }

    override fun getEnergyCapacity(): Long = energyCapacity

    override fun getInputAmperage(): Long = inputAmperage

    override fun getInputVoltage(): Long = inputVoltage

    override fun getOutputAmperage(): Long = outputVoltage

    override fun getOutputVoltage(): Long = outputVoltage

    override fun getContents(): List<Any> = listOf(EnergyStack(energyStored))

    override fun getTotalContentAmount(): Double = energyStored.toDouble()

    override fun getCapability(): RecipeCapability<EnergyStack> = EURecipeCapability.CAP

    override fun getInt128InputPerSec(): Int128 = lastEnergyInputPerSec.copy()

    override fun getInt128OutputPerSec(): Int128 = lastEnergyOutputPerSec.copy()

    override fun addEnergyPerSec(energy: Long) {
        if (energy != 0L) addEnergyPerSecExact(BigInteger.valueOf(energy))
    }

    override fun getInt128EnergyStored(): Int128 = Int128(energyStored)

    override fun getInt128EnergyCapacity(): Int128 = Int128(energyCapacity)

    override fun getInputPerSec(): Long = lastEnergyInputPerSec.toLong()

    override fun getOutputPerSec(): Long = lastEnergyOutputPerSec.toLong()

    override fun acceptEnergyFromNetwork(side: Direction, voltage: Long, amperage: Long): Long = 0

    override fun inputsEnergy(direction: Direction): Boolean = false

    override fun outputsEnergy(side: Direction): Boolean = false

    override fun getHandlerIO(): IO = handlerIO
}
