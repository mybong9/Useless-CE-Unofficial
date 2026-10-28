package com.sorrowmist.useless.mixin;

import com.sorrowmist.useless.event.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import java.util.function.Predicate;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityGetter.class)
public interface EntityGetterMixin {
    /** @author Useless @reason Filter stealthed players with Forge Mixin's interface default-method support. */
    @Overwrite
    default Player getNearestPlayer(double x, double y, double z, double range, Predicate<Entity> predicate) {
        Player nearest = null;
        double best = -1;
        for (Player player : com.sorrowmist.useless.utils.EntityGetterFilter.players((EntityGetter) (Object) this)) {
            if (predicate != null && !predicate.test(player)) continue;
            double distance = player.distanceToSqr(x, y, z);
            if ((range < 0 || distance < range * range) && (best < 0 || distance < best)) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }
    /** @author Useless @reason Apply the visibility filter to targeting queries. */
    @Overwrite
    default Player getNearestPlayer(TargetingConditions conditions, LivingEntity entity) {
        return ((EntityGetter) (Object) this).getNearestEntity(com.sorrowmist.useless.utils.EntityGetterFilter.players((EntityGetter) (Object) this), conditions, entity, entity.getX(), entity.getY(), entity.getZ());
    }
    /** @author Useless @reason Apply the visibility filter to targeting queries. */
    @Overwrite
    default Player getNearestPlayer(TargetingConditions conditions, LivingEntity entity, double x, double y, double z) {
        return ((EntityGetter) (Object) this).getNearestEntity(com.sorrowmist.useless.utils.EntityGetterFilter.players((EntityGetter) (Object) this), conditions, entity, x, y, z);
    }
    /** @author Useless @reason Apply the visibility filter to targeting queries. */
    @Overwrite
    default Player getNearestPlayer(TargetingConditions conditions, double x, double y, double z) {
        return ((EntityGetter) (Object) this).getNearestEntity(com.sorrowmist.useless.utils.EntityGetterFilter.players((EntityGetter) (Object) this), conditions, null, x, y, z);
    }
    /** @author Useless @reason Apply the visibility filter to area queries. */
    @Overwrite
    default List<Player> getNearbyPlayers(TargetingConditions conditions, LivingEntity entity, AABB area) {
        List<Player> result = new ArrayList<>();
        for (Player player : com.sorrowmist.useless.utils.EntityGetterFilter.players((EntityGetter) (Object) this)) {
            if (area.contains(player.getX(), player.getY(), player.getZ()) && conditions.test(entity, player)) result.add(player);
        }
        return result;
    }
    /** @author Useless @reason Apply the visibility filter to proximity checks. */
    @Overwrite
    default boolean hasNearbyAlivePlayer(double x, double y, double z, double range) {
        for (Player player : com.sorrowmist.useless.utils.EntityGetterFilter.players((EntityGetter) (Object) this)) {
            if (!player.isSpectator() && player.isAlive() && (range < 0 || player.distanceToSqr(x, y, z) < range * range)) return true;
        }
        return false;
    }
}
