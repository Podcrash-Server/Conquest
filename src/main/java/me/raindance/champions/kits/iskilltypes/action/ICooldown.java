package me.raindance.champions.kits.iskilltypes.action;

import me.raindance.champions.events.skill.SkillCooldownEvent;
import me.raindance.champions.kits.ChampionsPlayerManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public interface ICooldown {
    String getName();
    Player getPlayer();

    float getCooldown();

    default boolean hasCooldown() {
        return getCooldown() != -1;
    }
    default boolean onCooldown() {
        return (System.currentTimeMillis() - getLastUsed()) < getCooldown() * 1000L;
    }
    default double cooldown() {
        return (getCooldown() - ((System.currentTimeMillis() - getLastUsed())) / 1000D);
    }

    long getLastUsed();


    default String getCooldownMessage() {
        return String.format(
                "%s%s> %s%s %scannot be used for %s%.2f %sseconds",
                ChatColor.BLUE,
                ChampionsPlayerManager.getInstance().getChampionsPlayer(getPlayer()).getName(),
                ChatColor.GREEN,
                getName(),
                ChatColor.GRAY,
                ChatColor.GREEN,
                cooldown(),
                ChatColor.GRAY);
    }

    default String getCanUseMessage() {
        return String.format(
                "%sRecharge> %sYou can use %s%s%s.",
                ChatColor.BLUE, ChatColor.GRAY, ChatColor.GREEN, getName(), ChatColor.GRAY);
    }
}
