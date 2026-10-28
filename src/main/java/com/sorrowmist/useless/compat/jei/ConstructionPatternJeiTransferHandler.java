package com.sorrowmist.useless.compat.jei;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.sorrowmist.useless.init.ModNetwork;
import com.sorrowmist.useless.network.EncodeConstructionPatternPacket;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import java.util.Optional;

/** Prevent a generic AE transfer from truncating a building bill into nine one-item slots. */
public final class ConstructionPatternJeiTransferHandler<T extends PatternEncodingTermMenu>
        implements IRecipeTransferHandler<T,OmniversalStructureCategory.StructureRecipe> {
    private final Class<T> menuClass;
    private final MenuType<T> menuType;
    public ConstructionPatternJeiTransferHandler(Class<T> menuClass,MenuType<T> menuType){this.menuClass=menuClass;this.menuType=menuType;}
    public Class<? extends T> getContainerClass(){return menuClass;}
    public Optional<MenuType<T>> getMenuType(){return Optional.of(menuType);}
    public RecipeType<OmniversalStructureCategory.StructureRecipe> getRecipeType(){return OmniversalStructureCategory.TYPE;}
    public IRecipeTransferError transferRecipe(T menu,OmniversalStructureCategory.StructureRecipe recipe,
            IRecipeSlotsView slots,Player player,boolean maxTransfer,boolean doTransfer) {
        if(doTransfer)ModNetwork.CHANNEL.sendToServer(new EncodeConstructionPatternPacket(menu.containerId,recipe.preview().tier()));
        return null;
    }
}
