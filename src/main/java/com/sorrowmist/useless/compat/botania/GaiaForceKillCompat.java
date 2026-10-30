package com.sorrowmist.useless.compat.botania;

import com.sorrowmist.useless.UselessMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import java.util.List;
import java.util.UUID;

/** Prepare native Gaia death bookkeeping; never generate a second set of loot. */
public final class GaiaForceKillCompat {
    private GaiaForceKillCompat() {}
    public static void prepare(LivingEntity target, Player player) {
        if (target.level().isClientSide || !(player instanceof ServerPlayer) || player instanceof FakePlayer) return;
        Class<?> gaia = target.getClass();
        while (gaia != null && !gaia.getName().equals("vazkii.botania.common.entity.GaiaGuardianEntity"))
            gaia = gaia.getSuperclass();
        if (gaia == null) return;
        try {
            var attackersField = gaia.getDeclaredField("playersWhoAttacked");
            var waveField = gaia.getDeclaredField("mobSpawnTicks");
            var invulnerability = gaia.getMethod("setInvulTime", int.class);
            attackersField.setAccessible(true);
            waveField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<UUID> attackers = (List<UUID>) attackersField.get(target);
            if (!attackers.contains(player.getUUID())) attackers.add(player.getUUID());
            // During waves native getDefaultLootTable returns EMPTY. Retain hardMode unchanged.
            waveField.setInt(target, 0);
            invulnerability.invoke(target, 0);
            target.setLastHurtByPlayer(player);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            UselessMod.LOGGER.warn("Cannot prepare native Gaia rewards for staff force kill", exception);
        }
    }
}
