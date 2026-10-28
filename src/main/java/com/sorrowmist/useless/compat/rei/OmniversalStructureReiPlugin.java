package com.sorrowmist.useless.compat.rei;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.client.gui.structure.StructurePreview;
import com.sorrowmist.useless.content.multiblock.OmniversalFurnaceMaterials;
import com.sorrowmist.useless.init.ModBlocks;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static com.sorrowmist.useless.client.gui.structure.StructurePreview.text;

/** Loaded by REI's annotation scanner only; recipe viewers remain optional. */
@REIPluginClient
public final class OmniversalStructureReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<StructureDisplay> TYPE=CategoryIdentifier.of(UselessMod.id("omniversal_structure"));
    @Override public void registerCategories(CategoryRegistry registry) {
        registry.add(new Category());
        registry.addWorkstations(TYPE,EntryStacks.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get()));
        registry.removePlusButton(TYPE);
    }
    @Override public void registerDisplays(DisplayRegistry registry) {
        registry.add(new StructureDisplay());
    }
    private static void open(ItemStack stack,boolean usage) {
        var search=ViewSearchBuilder.builder();
        if(usage)search.addUsagesFor(EntryStacks.of(stack));else search.addRecipesFor(EntryStacks.of(stack));
        search.open();
    }
    private static final class StructureDisplay implements Display {
        private final StructurePreview preview=new StructurePreview(1,OmniversalStructureReiPlugin::open);
        private final List<EntryIngredient> inputs;
        StructureDisplay() {
            inputs=OmniversalFurnaceMaterials.usageItems().stream()
                    .map(s->EntryIngredient.of(EntryStacks.of(s))).toList();
        }
        public List<EntryIngredient> getInputEntries(){return inputs;}
        public List<EntryIngredient> getOutputEntries(){return List.of(EntryIngredient.of(EntryStacks.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get())));}
        public CategoryIdentifier<?> getCategoryIdentifier(){return TYPE;}
        public Optional<ResourceLocation> getDisplayLocation(){return Optional.of(UselessMod.id("structure/omniversal_furnace"));}
    }
    private static final class Category implements DisplayCategory<StructureDisplay> {
        public CategoryIdentifier<? extends StructureDisplay> getCategoryIdentifier(){return TYPE;}
        public Component getTitle(){return text("title");}
        public Renderer getIcon(){return EntryStacks.of(ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get());}
        public int getDisplayWidth(StructureDisplay display){return StructurePreview.viewerWidth()+8;}
        public int getDisplayHeight(){return StructurePreview.viewerHeight()+8;}
        public int getMaximumDisplaysPerPage(){return 1;}
        public List<Widget> setupDisplay(StructureDisplay display,Rectangle bounds) {
            var preview=display.preview;
            int ox=bounds.x+4,oy=bounds.y+4;
            float scale=StructurePreview.viewerScale();
            var widgets=new ArrayList<Widget>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(new Widget() {
                public List<? extends GuiEventListener> children(){return List.of();}
                public boolean containsMouse(double x,double y){return x>=ox&&x<ox+StructurePreview.WIDTH*scale&&y>=oy&&y<oy+StructurePreview.HEIGHT*scale;}
                public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta) {
                    graphics.pose().pushPose();graphics.pose().translate(ox,oy,0);graphics.pose().scale(scale,scale,1);
                    preview.draw(graphics,(mouseX-ox)/scale,(mouseY-oy)/scale);graphics.pose().popPose();
                    var tooltip=preview.tooltip((mouseX-ox)/scale,(mouseY-oy)/scale);
                    if(!tooltip.isEmpty()&&containsMouse(mouseX,mouseY))Tooltip.create(new Point(mouseX,mouseY),tooltip).queue();
                }
                public boolean mouseClicked(double x,double y,int button){return preview.mouseClicked((x-ox)/scale,(y-oy)/scale,button);}
                public boolean mouseReleased(double x,double y,int button){return preview.mouseReleased((x-ox)/scale,(y-oy)/scale,button);}
                public boolean mouseDragged(double x,double y,int button,double dx,double dy){return preview.mouseDragged((x-ox)/scale,(y-oy)/scale,button,dx/scale,dy/scale);}
                public boolean mouseScrolled(double x,double y,double delta){return preview.mouseScrolled((x-ox)/scale,(y-oy)/scale,delta);}
            });
            return widgets;
        }
    }
}
