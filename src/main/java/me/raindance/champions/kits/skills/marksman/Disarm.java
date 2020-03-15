package me.raindance.champions.kits.skills.marksman;
import com.podcrash.api.mc.damage.DamageApplier;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import com.podcrash.api.mc.sound.SoundPlayer;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.skilltypes.Interaction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;

@SkillMetadata(id = 509, skillType = SkillType.Marksman, invType = InvType.SWORD)
public class Disarm extends Interaction {
    private final float duration = 1.5F;
    private final double damage = 4;

    @Override
    public float getCooldown() {
        return 10;
    }

    @Override
    public String getName() {
        return "Disarm";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SWORD;
    }

    @Override
    public void doSkill(LivingEntity victim) {
        if (onCooldown()) return;
        if(isAlly(victim)) return;
        getPlayer().sendMessage(getUsedMessage(victim));
        StatusApplier.getOrNew(victim).applyStatus(Status.WEAKNESS, duration, 99);
        DamageApplier.damage(victim, getPlayer(), damage, this, false);
        this.setLastUsed(System.currentTimeMillis());
        SoundPlayer.sendSound(getPlayer().getLocation(), "mob.spider.death", 0.9F, 77);
    }
}