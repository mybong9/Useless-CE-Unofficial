package com.sorrowmist.useless.compat.gtceu;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blockentities.multiblock.MultiblockAlloyFurnaceCoreBlockEntity;
import com.sorrowmist.useless.content.machines.advanced_alloy_furnace.ae.CraftingTaskContext;
import com.sorrowmist.useless.content.recipe.AdvancedAlloyFurnaceRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.*;

import static com.sorrowmist.useless.compat.gtceu.GregTechParts.call;
import static com.sorrowmist.useless.compat.gtceu.GregTechParts.isA;

/** Runtime requirements use live GT hatches and the furnace's actual world; no research bypass. */
public final class GregTechEnvironment {
    private static final String STATUS = "gui.useless_mod.advanced_alloy_furnace.ae_task_status.waiting_conditions";
    private static final Set<String> CONDITIONS = Set.of("ResearchCondition", "CleanroomCondition",
            "DimensionCondition", "BiomeCondition", "PositionYCondition", "DaytimeCondition", "RainingCondition",
            "ThunderCondition", "AdjacentBlockCondition", "AdjacentFluidCondition", "GravityCondition");
    private GregTechEnvironment() {}

    public static boolean supports(Object condition) {
        String name = condition.getClass().getSimpleName();
        if (!CONDITIONS.contains(name)) return false;
        if (name.equals("AdjacentBlockCondition") || name.equals("AdjacentFluidCondition")) {
            String method = name.equals("AdjacentBlockCondition") ? "getOrInitBlocks" : "getOrInitFluids";
            return Arrays.stream(condition.getClass().getMethods()).anyMatch(m -> m.getName().equals(method) && m.getParameterCount() == 1);
        }
        return true;
    }

    public static CraftingTaskContext.TaskAvailability check(CraftingTaskContext context, AdvancedAlloyFurnaceRecipe converted) {
        if (converted == null || !ModList.get().isLoaded("gtceu")) return CraftingTaskContext.TaskAvailability.ready();
        Level level = context.getLevel();
        if (level == null || !converted.id().getPath().endsWith("_converted")) return CraftingTaskContext.TaskAvailability.ready();
        String path = converted.id().getPath();
        ResourceLocation id = new ResourceLocation(converted.id().getNamespace(), path.substring(0, path.length() - "_converted".length()));
        Recipe<?> source = level.getRecipeManager().byKey(id).orElse(null);
        if (source == null || !isA(source, "com.gregtechceu.gtceu.api.recipe.GTRecipe"))
            return CraftingTaskContext.TaskAvailability.ready();
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return waiting("GTM: server environment required");
        try {
            var power=checkPowerAndCoil(context,source);
            if(!power.available())return power;
            List<?> conditions = (List<?>) source.getClass().getField("conditions").get(source);
            if (conditions.isEmpty()) return CraftingTaskContext.TaskAvailability.ready();
            List<Object> parts = nativeParts(context);
            Map<Class<?>, Boolean> alternatives = new LinkedHashMap<>();
            Map<Class<?>, String> descriptions = new LinkedHashMap<>();
            for (Object condition : conditions) {
                if (!supports(condition)) return waiting(description(condition));
                boolean accepted = test(condition, source, context, parts);
                if (Boolean.TRUE.equals(call(condition, "isReverse"))) accepted = !accepted;
                if (Boolean.TRUE.equals(call(condition, "isOr"))) {
                    alternatives.merge(condition.getClass(), accepted, Boolean::logicalOr);
                    descriptions.putIfAbsent(condition.getClass(), description(condition));
                } else if (!accepted) return waiting(description(condition));
            }
            for (var entry : alternatives.entrySet()) if (!entry.getValue()) return waiting(descriptions.get(entry.getKey()));
            return CraftingTaskContext.TaskAvailability.ready();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            UselessMod.LOGGER.debug("Cannot evaluate GTM environment for {}", id, exception);
            return waiting("GTM: environment or research interface unavailable");
        }
    }

    private static CraftingTaskContext.TaskAvailability checkPowerAndCoil(CraftingTaskContext context,Object source)throws ReflectiveOperationException{
        if(!(context instanceof MultiblockAlloyFurnaceCoreBlockEntity core))return waiting("GT recipes require multiblock energy hatches");
        Object container=GregTechFurnacePower.container(core);
        long voltage=GregTechFurnacePower.maxVoltage(container),required=0;
        var ticks=(Map<?,?>)source.getClass().getField("tickInputs").get(source);
        for(var entry:ticks.entrySet())if(entry.getKey().getClass().getField("name").get(entry.getKey()).equals("eu")){
            for(Object content:(List<?>)entry.getValue()){
                Object value=content.getClass().getField("content").get(content);
                long v=value instanceof Number n?n.longValue():((Number)call(value,"voltage")).longValue();
                required=Math.max(required,v);
            }
        }
        if(required>voltage)return waiting("GT input voltage: "+voltage+" EU; required: "+required+" EU");
        CompoundTag data=(CompoundTag)source.getClass().getField("data").get(source);
        if(data.contains("ebf_temp")){
            int coil=0;
            for(var stack:core.getAvailableMoldMap().values())coil=Math.max(coil,GregTechFurnacePower.coilTemperature(stack));
            long[] tiers;
            try{tiers=(long[])Class.forName("com.gregtechceu.gtceu.api.GTValues").getField("V").get(null);}
            catch(ClassNotFoundException old){tiers=(long[])Class.forName("com.gregtechceu.gtceu.GTValues").getField("V").get(null);}
            int tier=0;while(tier<tiers.length-1&&voltage>tiers[tier])tier++;
            if(coil==0 || (long)coil+100L*Math.max(0,tier-2)<data.getInt("ebf_temp"))return waiting("GT heating coil temperature: "+coil+" K; required: "+data.getInt("ebf_temp")+" K");
        }
        return CraftingTaskContext.TaskAvailability.ready();
    }

    private static List<Object> nativeParts(CraftingTaskContext context) throws ReflectiveOperationException {
        if (!(context instanceof MultiblockAlloyFurnaceCoreBlockEntity core) || !core.isFormed()) return List.of();
        List<Object> result = new ArrayList<>();
        for (BlockPos pos : core.getGregTechParts()) {
            if (!core.getLevel().hasChunkAt(pos)) continue;
            Object machine = GregTechParts.machine(core.getLevel(), pos);
            if (machine != null && GregTechParts.isFurnacePart(machine)) result.add(machine);
        }
        return result;
    }

    private static boolean test(Object condition, Recipe<?> recipe, CraftingTaskContext context, List<Object> parts)
            throws ReflectiveOperationException {
        Level level = context.getLevel();
        BlockPos pos = context.getBlockPos();
        return switch (condition.getClass().getSimpleName()) {
            case "ResearchCondition" -> research(recipe, parts);
            case "CleanroomCondition" -> cleanroom(condition, level, pos, parts);
            case "GravityCondition" -> gravity(condition, level, pos, parts);
            case "DimensionCondition" -> level.dimension().location().equals(call(condition, "getDimension"));
            case "BiomeCondition" -> biome(condition, level, pos);
            case "PositionYCondition" -> pos.getY() >= ((Number) call(condition, "getMin")).intValue()
                    && pos.getY() <= ((Number) call(condition, "getMax")).intValue();
            case "DaytimeCondition" -> level.isDay();
            case "RainingCondition" -> level.getRainLevel(1F) >= ((Number) call(condition, "getLevel")).floatValue();
            case "ThunderCondition" -> level.getThunderLevel(1F) >= ((Number) call(condition, "getLevel")).floatValue();
            case "AdjacentBlockCondition" -> adjacent(condition, recipe, level, pos, false);
            case "AdjacentFluidCondition" -> adjacent(condition, recipe, level, pos, true);
            default -> false;
        };
    }

    private static boolean gravity(Object condition, Level level, BlockPos pos, List<Object> parts) throws ReflectiveOperationException {
        var settings = (com.google.gson.JsonObject) call(condition, "serialize");
        boolean zero = settings.get("gravity").getAsBoolean();
        for (Object part : parts) if (isA(part, "org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IGravityPartMachine")
                && ((Number) call(part, "getCurrentGravity")).intValue() == (zero ? 0 : 100)) return true;
        String dimension = level.dimension().location().toString();
        if (zero && dimension.contains("_orbit")) return true;
        Object holder = Class.forName("org.gtlcore.gtlcore.config.ConfigHolder").getField("INSTANCE").get(null);
        Object content = holder.getClass().getField("content").get(holder);
        if (!content.getClass().getField("enableSkyBlokeMode").getBoolean(content)) return false;
        int height = settings.has("height_threshold") ? settings.get("height_threshold").getAsInt() : 313;
        return zero && (dimension.equals("minecraft:overworld") && pos.getY() >= height
                || (dimension.equals("kubejs:void") || dimension.equals("kubejs:flat")) && pos.getY() >= 505);
    }

    private static boolean biome(Object condition, Level level, BlockPos pos) throws ReflectiveOperationException {
        Object required = call(condition, "getBiome");
        if (required instanceof ResourceLocation id)
            return level.getBiome(pos).unwrapKey().map(key -> key.location().equals(id)).orElse(false);
        return required instanceof ResourceKey<?> key && level.getBiome(pos).is((ResourceKey) key);
    }

    private static boolean research(Recipe<?> recipe, List<Object> parts) throws ReflectiveOperationException {
        Class<?> api = Class.forName("com.gregtechceu.gtceu.api.capability.IDataAccessHatch");
        // GT's modifyRecipe accepts creative hatches before consulting their (empty) research inventory.
        var creative = api.getMethod("isCreative");
        var check = api.getMethod("isRecipeAvailable", Class.forName("com.gregtechceu.gtceu.api.recipe.GTRecipe"), Collection.class);
        for (Object part : parts) if (api.isInstance(part)
                && (Boolean.TRUE.equals(creative.invoke(part))
                || Boolean.TRUE.equals(check.invoke(part, recipe, new HashSet<>())))) return true;
        return false;
    }

    private static boolean cleanroom(Object condition, Level level, BlockPos pos, List<Object> parts) throws ReflectiveOperationException {
        Object config = Class.forName("com.gregtechceu.gtceu.config.ConfigHolder").getField("INSTANCE").get(null);
        Object machines = config.getClass().getField("machines").get(config);
        if (!machines.getClass().getField("enableCleanroom").getBoolean(machines)
                || machines.getClass().getField("cleanMultiblocks").getBoolean(machines)) return true;
        Object required = call(condition, "getCleanroom");
        if (required == null) return true;
        for (Object part : parts) {
            if (providerHas(part, required)) return true;
            if (!GregTechParts.isMaintenancePart(part)) continue;
            // Read the actual provider assigned by native attachment, including its type set and clean state.
            for (Class<?> type = part.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                    if (!field.getType().getName().equals("com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider")) continue;
                    if (field.trySetAccessible() && providerHas(field.get(part), required)) return true;
                }
            }
        }
        // Also accept a real, formed and clean GT cleanroom enclosing the core's position.
        if (level instanceof ServerLevel server) {
            Class<?> savedType = Class.forName("com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData");
            Object saved = savedType.getMethod("getOrCreate", ServerLevel.class).invoke(null, server);
            Object controllers;
            try { controllers = savedType.getMethod("getControllersInChunk", ChunkPos.class).invoke(saved, new ChunkPos(pos)); }
            catch (NoSuchMethodException legacy) {
                Object[] states = (Object[]) savedType.getMethod("getControllerInChunk", ChunkPos.class).invoke(saved, new ChunkPos(pos));
                controllers = states == null ? List.of() : Arrays.asList(states);
            }
            if (controllers instanceof Collection<?> states) for (Object state : states) {
                if (!((Collection<?>) call(state, "getCache")).contains(pos)) continue;
                Object controller = call(state, "getController");
                if (controller != null && Boolean.TRUE.equals(call(controller, "isFormed")) && providerHas(controller, required)) return true;
            }
        }
        return false;
    }

    private static boolean providerHas(Object provider, Object required) throws ReflectiveOperationException {
        if (!isA(provider, "com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider")) return false;
        Class<?> api = Class.forName("com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider");
        return Boolean.TRUE.equals(api.getMethod("isClean").invoke(provider))
                && ((Set<?>) api.getMethod("getTypes").invoke(provider)).contains(required);
    }

    @SuppressWarnings("unchecked")
    private static boolean adjacent(Object condition, Recipe<?> recipe, Level level, BlockPos pos, boolean fluid)
            throws ReflectiveOperationException {
        String method = fluid ? "getOrInitFluids" : "getOrInitBlocks";
        List<HolderSet<?>> choices = (List<HolderSet<?>>) condition.getClass().getMethod(method, recipe.getClass()).invoke(condition, recipe);
        // Native conditions require each declared set to have a matching adjacent face.
        for (HolderSet<?> set : choices) {
            boolean found = false;
            for (Direction side : Direction.values()) {
                BlockPos other = pos.relative(side);
                if (!level.hasChunkAt(other)) continue;
                Object holder = fluid ? level.getFluidState(other).getType().builtInRegistryHolder()
                        : level.getBlockState(other).getBlock().builtInRegistryHolder();
                if (((HolderSet) set).contains((net.minecraft.core.Holder) holder)) { found = true; break; }
            }
            if (!found) return false;
        }
        return !choices.isEmpty();
    }
    private static String description(Object condition) {
        try { return ((Component) call(condition, "getTooltips")).getString(); }
        catch (ReflectiveOperationException | RuntimeException unavailable) { return "GTM: " + condition.getClass().getSimpleName(); }
    }
    private static CraftingTaskContext.TaskAvailability waiting(String detail) {
        return CraftingTaskContext.TaskAvailability.unavailable(STATUS, detail);
    }
}
