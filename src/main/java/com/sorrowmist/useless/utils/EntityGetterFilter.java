package com.sorrowmist.useless.utils;

import com.sorrowmist.useless.event.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import java.util.ArrayList;
import java.util.List;

/** Shared query filter kept outside interface mixins for Mixin 0.8.5. */
public final class EntityGetterFilter {
    private EntityGetterFilter() {}
    public static List<? extends Player> players(EntityGetter getter) {
        List<? extends Player> players = getter.players();
        if (!EventHandler.hasAnyBeefAdvancedStealthPlayers()) {
            return players;
        }

        List<Player> filtered = null;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (EventHandler.hasBeefAdvancedStealthItem(player)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(players.size() - 1);
                    filtered.addAll(players.subList(0, i));
                }
                continue;
            }

            if (filtered != null) {
                filtered.add(player);
            }
        }
        return filtered == null ? players : filtered;
    }
}
