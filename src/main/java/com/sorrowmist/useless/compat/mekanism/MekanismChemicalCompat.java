package com.sorrowmist.useless.compat.mekanism;

import appeng.api.stacks.GenericStack;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.chemical.*;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.io.FurnaceFaceAccessor;
import com.sorrowmist.useless.init.ModBlockEntities;
import com.sorrowmist.uselesscore.api.capability.CoreCapabilityRegistration;
import com.sorrowmist.uselesscore.api.enums.FurnaceFace;
import com.sorrowmist.uselesscore.api.enums.FurnaceFaceMode;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.*;
import mekanism.api.chemical.infuse.*;
import mekanism.api.chemical.pigment.*;
import mekanism.api.chemical.slurry.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.util.ChemicalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Four native Mekanism 10.4 capability domains backed by the shared furnace storage. */
public final class MekanismChemicalCompat {
    public static final String MOD_ID = "mekanism";
    public static final int TANK_COUNT = 3;
    private MekanismChemicalCompat() {}

    public static FurnaceChemicalStorage createStorage(long capacity, Runnable onChanged) {
        return new MekanismChemicalStorage(TANK_COUNT, capacity, onChanged);
    }
    public static @Nullable GenericStack toGenericStack(ChemicalStack<?> stack) {
        if (stack == null || stack.isEmpty() || stack.getAmount() <= 0) return null;
        return ChemicalKeyProviders.get().toGenericStack(new MekanismChemicalStackView(stack));
    }
    public static void registerCapabilities(CoreCapabilityRegistration event) {
        ChemicalCompatProviders.register(new Provider());
        var type = ModBlockEntities.ADVANCED_ALLOY_FURNACE.get();
        event.registerBlockEntity(Capabilities.GAS_HANDLER, type, (be, side) ->
                new GasHandler(be.getInputChemicalStorage(), be.getOutputChemicalStorage(), side, be));
        event.registerBlockEntity(Capabilities.INFUSION_HANDLER, type, (be, side) ->
                new InfusionHandler(be.getInputChemicalStorage(), be.getOutputChemicalStorage(), side, be));
        event.registerBlockEntity(Capabilities.PIGMENT_HANDLER, type, (be, side) ->
                new PigmentHandler(be.getInputChemicalStorage(), be.getOutputChemicalStorage(), side, be));
        event.registerBlockEntity(Capabilities.SLURRY_HANDLER, type, (be, side) ->
                new SlurryHandler(be.getInputChemicalStorage(), be.getOutputChemicalStorage(), side, be));
    }
    private static final class Provider implements ChemicalCompatProvider {
        @Override public FurnaceChemicalStorage createStorage(long capacity, Runnable changed) {
            return MekanismChemicalCompat.createStorage(capacity, changed);
        }
        @Override public ChemicalHandlerView getAdjacentHandler(Level level, BlockPos pos, BlockState state,
                BlockEntity entity, Direction side) {
            return MekanismChemicalCompat.getAdjacentHandler(level, pos, state, entity, side);
        }
        @Override public ChemicalStackView chemicalInItem(ItemStack stack) {
            return MekanismChemicalCompat.chemicalInItem(stack);
        }
        @Override public ItemStack markerForChemical(Object ingredient) {
            return MekanismChemicalCompat.markerForChemical(ingredient);
        }
        @Override public ResourceLocation chemicalIdOf(Object key) { return MekanismChemicalCompat.chemicalIdOf(key); }
    }
    public static @Nullable ResourceLocation chemicalIdOf(Object typeKey) {
        return typeKey instanceof Chemical<?> chemical ? chemical.getRegistryName() : null;
    }
    public static @Nullable ChemicalStackView chemicalInItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (Endpoint endpoint : endpoints(stack, null)) {
            for (int tank = 0; tank < endpoint.handler.getTanks(); tank++) {
                ChemicalStack<?> chemical = endpoint.handler.getChemicalInTank(tank);
                if (!chemical.isEmpty()) return new MekanismChemicalStackView(chemical);
            }
        }
        return null;
    }
    public static ItemStack markerForChemical(Object ingredient) {
        return ingredient instanceof ChemicalStack<?> stack && !stack.isEmpty()
                ? ChemicalUtil.getFullChemicalTank(ChemicalTankTier.BASIC, stack.getType()) : ItemStack.EMPTY;
    }
    public static @Nullable ChemicalHandlerView getAdjacentHandler(Level level, BlockPos pos, BlockState state,
            BlockEntity entity, Direction side) {
        if (level == null || entity == null) return null;
        List<Endpoint> endpoints = endpoints(entity, side);
        return endpoints.isEmpty() ? null : new HandlerView(endpoints);
    }
    private record Endpoint(Class<? extends ChemicalStack> stackType, IChemicalHandler handler) {}
    private static List<Endpoint> endpoints(ICapabilityProvider provider, Direction side) {
        List<Endpoint> result = new ArrayList<>(4);
        add(result, provider, side, Capabilities.GAS_HANDLER, GasStack.class);
        add(result, provider, side, Capabilities.INFUSION_HANDLER, InfusionStack.class);
        add(result, provider, side, Capabilities.PIGMENT_HANDLER, PigmentStack.class);
        add(result, provider, side, Capabilities.SLURRY_HANDLER, SlurryStack.class);
        return result;
    }
    private static <H extends IChemicalHandler<?, ?>> void add(List<Endpoint> endpoints,
            ICapabilityProvider provider, Direction side, Capability<H> capability, Class<? extends ChemicalStack> type) {
        provider.getCapability(capability, side).ifPresent(handler -> endpoints.add(new Endpoint(type, handler)));
    }
    private abstract static class FurnaceHandler<C extends Chemical<C>, S extends ChemicalStack<C>>
            implements IChemicalHandler<C, S> {
        private final FurnaceChemicalStorage input, output;
        private final Direction side;
        private final FurnaceFaceAccessor owner;
        private final Class<S> type;
        private final S empty;
        FurnaceHandler(FurnaceChemicalStorage input, FurnaceChemicalStorage output, Direction side,
                FurnaceFaceAccessor owner, Class<S> type, S empty) {
            this.input = input; this.output = output; this.side = side;
            this.owner = owner; this.type = type; this.empty = empty;
        }
        @Override public S getEmptyStack() { return empty; }
        @Override public int getTanks() { return input.size() + output.size(); }
        @Override public S getChemicalInTank(int tank) {
            FurnaceChemicalStorage storage = storage(tank);
            return storage == null ? empty : nativeStack(storage.getStackInSlot(local(tank)));
        }
        @Override public void setChemicalInTank(int tank, S stack) {
            FurnaceChemicalStorage storage = storage(tank);
            if (storage != null && type.isInstance(stack)) storage.setStackInSlot(local(tank), new MekanismChemicalStackView(stack));
        }
        @Override public long getTankCapacity(int tank) {
            FurnaceChemicalStorage storage = storage(tank);
            return storage == null ? 0 : storage.capacity(local(tank));
        }
        @Override public boolean isValid(int tank, S stack) {
            return tank >= 0 && tank < input.size() && stack != null && type.isInstance(stack) && !stack.isEmpty();
        }
        @Override public S insertChemical(int tank, S stack, Action action) {
            if (!isValid(tank, stack) || !allowsInput()) return stack;
            return nativeStack(input.insertChemical(tank, new MekanismChemicalStackView(stack), action.simulate()));
        }
        @Override public S extractChemical(int tank, long amount, Action action) {
            if (tank < input.size() || tank >= getTanks() || amount <= 0 || !allowsOutput()
                    || getChemicalInTank(tank).isEmpty()) return empty;
            return nativeStack(output.extractChemical(local(tank), amount, action.simulate()));
        }
        private S nativeStack(ChemicalStackView view) {
            if (view instanceof MekanismChemicalStackView mek && !mek.isEmpty() && type.isInstance(mek.stack())) {
                return type.cast(mek.stack().copy());
            }
            return empty;
        }
        private FurnaceChemicalStorage storage(int tank) {
            return tank >= 0 && tank < input.size() ? input : tank >= input.size() && tank < getTanks() ? output : null;
        }
        private int local(int tank) { return tank < input.size() ? tank : tank - input.size(); }
        private FurnaceFaceMode faceMode() {
            if (side == null || owner == null) return null;
            FurnaceFace face = FurnaceFace.fromDirection(side, owner.getFacing());
            return face == null ? FurnaceFaceMode.DISABLED : owner.getFaceMode(face);
        }
        private boolean allowsInput() { var mode = faceMode(); return mode == null || mode.allowsMaterialInput(); }
        private boolean allowsOutput() { var mode = faceMode(); return mode == null || mode.allowsMaterialOutput(); }
    }
    private static final class GasHandler extends FurnaceHandler<Gas, GasStack> implements IGasHandler {
        GasHandler(FurnaceChemicalStorage in, FurnaceChemicalStorage out, Direction side, FurnaceFaceAccessor owner) {
            super(in, out, side, owner, GasStack.class, GasStack.EMPTY);
        }
    }
    private static final class InfusionHandler extends FurnaceHandler<InfuseType, InfusionStack> implements IInfusionHandler {
        InfusionHandler(FurnaceChemicalStorage in, FurnaceChemicalStorage out, Direction side, FurnaceFaceAccessor owner) {
            super(in, out, side, owner, InfusionStack.class, InfusionStack.EMPTY);
        }
    }
    private static final class PigmentHandler extends FurnaceHandler<Pigment, PigmentStack> implements IPigmentHandler {
        PigmentHandler(FurnaceChemicalStorage in, FurnaceChemicalStorage out, Direction side, FurnaceFaceAccessor owner) {
            super(in, out, side, owner, PigmentStack.class, PigmentStack.EMPTY);
        }
    }
    private static final class SlurryHandler extends FurnaceHandler<Slurry, SlurryStack> implements ISlurryHandler {
        SlurryHandler(FurnaceChemicalStorage in, FurnaceChemicalStorage out, Direction side, FurnaceFaceAccessor owner) {
            super(in, out, side, owner, SlurryStack.class, SlurryStack.EMPTY);
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private record HandlerView(List<Endpoint> endpoints) implements ChemicalHandlerView {
        @Override public ChemicalStackView insertChemical(ChemicalStackView stack, boolean simulate) {
            if (!(stack instanceof MekanismChemicalStackView mek) || stack.isEmpty()) return stack;
            for (Endpoint endpoint : endpoints) {
                if (!endpoint.stackType.isInstance(mek.stack())) continue;
                return view(endpoint.handler.insertChemical(mek.stack(), simulate ? Action.SIMULATE : Action.EXECUTE));
            }
            return stack;
        }
        @Override public ChemicalStackView extractChemical(long amount, boolean simulate) {
            if (amount <= 0) return ChemicalStackView.EMPTY;
            for (Endpoint endpoint : endpoints) {
                ChemicalStack<?> extracted = endpoint.handler.extractChemical(amount, simulate ? Action.SIMULATE : Action.EXECUTE);
                if (!extracted.isEmpty()) return view(extracted);
            }
            return ChemicalStackView.EMPTY;
        }
        @Override public ChemicalStackView extractChemical(ChemicalStackView stack, long amount, boolean simulate) {
            if (!(stack instanceof MekanismChemicalStackView mek) || stack.isEmpty() || amount <= 0) return ChemicalStackView.EMPTY;
            for (Endpoint endpoint : endpoints) {
                if (!endpoint.stackType.isInstance(mek.stack())) continue;
                return view(endpoint.handler.extractChemical(MekanismChemicalStacks.copyWithAmount(mek.stack(), amount),
                        simulate ? Action.SIMULATE : Action.EXECUTE));
            }
            return ChemicalStackView.EMPTY;
        }
        private static ChemicalStackView view(ChemicalStack<?> stack) {
            return stack.isEmpty() ? ChemicalStackView.EMPTY : new MekanismChemicalStackView(stack);
        }
    }
}