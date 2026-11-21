package com.sorrowmist.useless.mixin.exdeorum;

import com.sorrowmist.useless.compat.exdeorum.MechanicalSievePages;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.menu.MechanicalSieveMenu", remap = false)
public abstract class MechanicalSieveMenuMixin extends AbstractContainerMenu implements MechanicalSievePages {
    @Unique private final DataSlot useless$outputPage = DataSlot.standalone();
    @Unique private final DataSlot useless$outputPages = DataSlot.standalone();

    protected MechanicalSieveMenuMixin() {
        super(null, 0);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lthedarkcolour/exdeorum/blockentity/MechanicalSieveBlockEntity;)V",
            at = @At("RETURN"), require = 1)
    private void useless$paginateOutputs(int id, Inventory playerInventory, @Coerce BlockEntity machine, CallbackInfo ci) {
        addDataSlot(useless$outputPage);
        addDataSlot(useless$outputPages);
        if (playerInventory.player.level().isClientSide) return;
        if (!(getSlot(2) instanceof SlotItemHandler output)
                || !(output.getItemHandler() instanceof IItemHandlerModifiable inventory)) return;

        useless$outputPages.set(Math.max(1, (inventory.getSlots() - 2 + PAGE_SIZE - 1) / PAGE_SIZE));
        for (int index = 2; index < 2 + PAGE_SIZE; index++) {
            var original = getSlot(index);
            Slot replacement = new com.sorrowmist.useless.compat.exdeorum.PagedSieveOutputSlot(
                    inventory, index, original.x, original.y, useless$outputPage::get, machine::setChanged);
            replacement.index = index;
            slots.set(index, replacement);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != PREVIOUS && id != NEXT) return super.clickMenuButton(player, id);
        int target = useless$page() + (id == PREVIOUS ? -1 : 1);
        if (!stillValid(player) || target < 0 || target >= useless$pageCount()) return false;
        useless$outputPage.set(target);
        broadcastFullState();
        return true;
    }

    @Override public int useless$page() { return useless$outputPage.get(); }
    @Override public int useless$pageCount() { return Math.max(1, useless$outputPages.get()); }
}
