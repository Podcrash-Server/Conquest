package me.raindance.champions.kits.skills.rogue;

import com.podcrash.api.mc.damage.DamageApplier;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import com.podcrash.api.mc.sound.SoundPlayer;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.skilltypes.Interaction;
import org.bukkit.entity.LivingEntity;

/**
 * Vital Stab
 * Class: Rogue
 * Type: Sword Ability
 * Cooldown: 9 seconds
 * Description: Right click your sword on an enemy within 4 blocks to activate. Deal 6 damage them and inflict Bleed for 5 seconds.
 * Details:
 * When activated, plays the sound of a TNT block being primed.
 * Vital Stab can miss.
 */
@SkillMetadata(id = 609, skillType = SkillType.Rogue, invType = InvType.SWORD)
public class VitalStab extends Interaction {
    @Override
    public void doSkill(LivingEntity clickedEntity) {
        if(onCooldown()) return;
        if(isAlly(clickedEntity)) {return;}
        setLastUsed(System.currentTimeMillis());
        DamageApplier.damage(clickedEntity, getPlayer(), 6, this, false);
        StatusApplier.getOrNew(clickedEntity).applyStatus(Status.BLEED, 5, 1);
        //TODO:
        SoundPlayer.sendSound(clickedEntity.getLocation(), "random.anvil_land", 0.9F, 110);

        landed();
    }

    @Override
    public float getCooldown() {
        return 9;
    }

    @Override
    public String getName() {
        return "Vital Stab";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SWORD;
    }

}
