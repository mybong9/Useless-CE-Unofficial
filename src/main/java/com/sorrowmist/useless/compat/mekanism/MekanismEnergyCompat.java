package com.sorrowmist.useless.compat.mekanism;

import com.sorrowmist.useless.energy.IEnergyManager;
import com.sorrowmist.useless.init.ModBlockEntities;
import mekanism.api.Action;
import mekanism.api.energy.IEnergyConversion;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import com.sorrowmist.uselesscore.api.capability.CoreCapabilityRegistration;

public final class MekanismEnergyCompat {
    public static final String MOD_ID = "mekanism";

    private MekanismEnergyCompat() {
    }

    public static void registerCapabilities(CoreCapabilityRegistration event) {
        event.registerBlockEntity(
                Capabilities.STRICT_ENERGY,
                ModBlockEntities.ADVANCED_ALLOY_FURNACE.get(),
                (blockEntity, side) -> new FurnaceStrictEnergyHandler(blockEntity.getEnergyManager())
        );
        event.registerBlockEntity(
                Capabilities.STRICT_ENERGY,
                ModBlockEntities.MULTIBLOCK_ALLOY_FURNACE_CORE.get(),
                (blockEntity, side) -> new FurnaceStrictEnergyHandler(blockEntity.getEnergyManager())
        );
    }

    static final class FurnaceStrictEnergyHandler implements IStrictEnergyHandler {
        private final IEnergyManager energy;
        private final IEnergyConversion converter;
        FurnaceStrictEnergyHandler(IEnergyManager energy) { this(energy, EnergyUnit.FORGE_ENERGY); }
        FurnaceStrictEnergyHandler(IEnergyManager energy, IEnergyConversion converter) {
            this.energy = energy;
            this.converter = converter;
        }
        @Override public int getEnergyContainerCount() { return 1; }
        @Override public mekanism.api.math.FloatingLong getEnergy(int container) {
            return container == 0 ? converter.convertFrom(energy.getEnergyStoredLong()) : mekanism.api.math.FloatingLong.ZERO;
        }
        @Override public void setEnergy(int container, mekanism.api.math.FloatingLong amount) {
            if (container == 0) energy.setEnergyStored(Math.max(0L, converter.convertToAsLong(amount)));
        }
        @Override public mekanism.api.math.FloatingLong getMaxEnergy(int container) {
            return container == 0 ? converter.convertFrom(energy.getMaxEnergyStoredLong()) : mekanism.api.math.FloatingLong.ZERO;
        }
        @Override public mekanism.api.math.FloatingLong getNeededEnergy(int container) {
            return container == 0 ? converter.convertFrom(Math.max(0L,
                    energy.getMaxEnergyStoredLong() - energy.getEnergyStoredLong())) : mekanism.api.math.FloatingLong.ZERO;
        }
        @Override public mekanism.api.math.FloatingLong insertEnergy(int container,
                mekanism.api.math.FloatingLong amount, Action action) {
            if (container != 0 || amount.isZero() || !energy.canReceive()) return amount;
            long requested = Math.max(0L, converter.convertToAsLong(amount));
            long accepted = energy.receiveEnergy(requested, true);
            if (accepted <= 0L) return amount;
            var cost = converter.convertFrom(accepted);
            if (cost.isZero() || cost.greaterThan(amount)) return amount;
            long inserted = action.simulate() ? accepted : energy.receiveEnergy(accepted, false);
            return amount.subtract(converter.convertFrom(inserted).min(amount));
        }
        @Override public mekanism.api.math.FloatingLong extractEnergy(int container,
                mekanism.api.math.FloatingLong amount, Action action) {
            if (container != 0 || amount.isZero() || !energy.canExtract()) return mekanism.api.math.FloatingLong.ZERO;
            long requested = Math.max(0L, converter.convertToAsLong(amount));
            long available = energy.extractEnergy(requested, true);
            if (available <= 0L) return mekanism.api.math.FloatingLong.ZERO;
            var delivered = converter.convertFrom(available);
            if (delivered.isZero() || delivered.greaterThan(amount)) return mekanism.api.math.FloatingLong.ZERO;
            long extracted = action.simulate() ? available : energy.extractEnergy(available, false);
            return converter.convertFrom(extracted).min(amount);
        }
    }
}