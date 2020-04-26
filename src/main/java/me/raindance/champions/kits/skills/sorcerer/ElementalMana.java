package me.raindance.champions.kits.skills.sorcerer;

import com.podcrash.api.events.DeathApplyEvent;
import com.podcrash.api.kits.EnergyBar;
import me.raindance.champions.annotation.kits.SkillMetadata;
import com.podcrash.api.kits.enums.InvType;
import me.raindance.champions.kits.SkillType;
import com.podcrash.api.kits.skilltypes.Passive;
import org.bukkit.event.EventHandler;

/**
 * This is a placeholder, this doesn't actually do anything
 */
@SkillMetadata(id = 1002, skillType = SkillType.Sorcerer, invType = InvType.INNATE)
public class ElementalMana extends Passive {
    @Override
    public String getName() {
        return "Elemental Mana";
    }


    @EventHandler
    public void kill(DeathApplyEvent event) {
        if(event.getAttacker() != getPlayer()) return;
        EnergyBar eBar = getChampionsPlayer().getEnergyBar();
        eBar.incrementEnergy(50);
    }
}
