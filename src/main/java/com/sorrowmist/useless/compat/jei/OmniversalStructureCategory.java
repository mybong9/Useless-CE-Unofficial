package com.sorrowmist.useless.compat.jei;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.client.gui.structure.StructurePreview;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import com.sorrowmist.useless.init.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import static com.sorrowmist.useless.client.gui.structure.StructurePreview.text;

/** Native JEI usage indexing around one shared, responsive interactive structure view. */
public final class OmniversalStructureCategory implements IRecipeCategory<OmniversalStructureCategory.StructureRecipe> {
    public static final RecipeType<StructureRecipe> TYPE=RecipeType.create(UselessMod.MODID,"omniversal_structure",StructureRecipe.class);
    private final IDrawable background,icon;
    public record StructureRecipe(StructurePreview preview) {}
    public OmniversalStructureCategory(IGuiHelper helper) {
        background=new IDrawable() {
            public int getWidth(){return StructurePreview.viewerWidth();}
            public int getHeight(){return StructurePreview.viewerHeight();}
            public void draw(GuiGraphics graphics,int x,int y){}
        };
        icon=helper.createDrawableItemStack(new ItemStack(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get()));
    }
    public static List<StructureRecipe> recipes() {
        return List.of(new StructureRecipe(new StructurePreview(1,OmniversalStructureCategory::open)));
    }
    private StructurePreview view(StructureRecipe recipe) {
        return recipe.preview();
    }
    private static void open(ItemStack stack,boolean usage) {
        var runtime=JEIPlugin.getRuntime();if(runtime==null)return;
        var focus=runtime.getJeiHelpers().getFocusFactory().createFocus(usage?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack);
        runtime.getRecipesGui().show(List.of(focus));
    }
    @Override public @NotNull RecipeType<StructureRecipe> getRecipeType(){return TYPE;}
    @Override public @NotNull Component getTitle(){return text("title");}
    @Override public IDrawable getIcon(){return icon;}
    @Override @SuppressWarnings("removal") public IDrawable getBackground(){return background;}
    @Override public int getWidth(){return StructurePreview.viewerWidth();}
    @Override public int getHeight(){return StructurePreview.viewerHeight();}
    @Override public ResourceLocation getRegistryName(StructureRecipe recipe){return UselessMod.id("structure/omniversal_furnace");}

    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder,@NotNull StructureRecipe recipe,@NotNull IFocusGroup focuses) {
        // Manual material slots stay in sync with the live tier selector. Invisible
        // ingredients index every legal block, including all ten coils, on one recipe.
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(OmniversalFurnaceMaterials.usageItems());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(new ItemStack(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get()));
    }

    // Available since JEI 15.9.0; the existing 15.12.3 minimum is unchanged.
    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder,StructureRecipe recipe,IFocusGroup focuses) {
        var preview=view(recipe);
        builder.addGuiEventListener(new IJeiGuiEventListener() {
            public ScreenRectangle getArea(){return new ScreenRectangle(0,0,StructurePreview.viewerWidth(),StructurePreview.viewerHeight());}
            public boolean mouseClicked(double x,double y,int button){float s=StructurePreview.viewerScale();return preview.mouseClicked(x/s,y/s,button);}
            public boolean mouseReleased(double x,double y,int button){float s=StructurePreview.viewerScale();return preview.mouseReleased(x/s,y/s,button);}
            public boolean mouseDragged(double x,double y,int button,double dx,double dy){float s=StructurePreview.viewerScale();return preview.mouseDragged(x/s,y/s,button,dx/s,dy/s);}
            public boolean mouseScrolled(double x,double y,double delta){float s=StructurePreview.viewerScale();return preview.mouseScrolled(x/s,y/s,delta);}
        });
    }
    @Override public void draw(@NotNull StructureRecipe recipe,@NotNull IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        float scale=StructurePreview.viewerScale();
        graphics.pose().pushPose();graphics.pose().scale(scale,scale,1);
        view(recipe).draw(graphics,mouseX/scale,mouseY/scale);graphics.pose().popPose();
    }
    @Override public @NotNull List<Component> getTooltipStrings(StructureRecipe recipe,IRecipeSlotsView slots,double mouseX,double mouseY) {
        float scale=StructurePreview.viewerScale();return view(recipe).tooltip(mouseX/scale,mouseY/scale);
    }
}

