package me.raindance.champions.kits.skills.druid;

import com.podcrash.api.mc.damage.Cause;
import com.podcrash.api.mc.events.DamageApplyEvent;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.action.IEnergy;
import me.raindance.champions.kits.skilltypes.Passive;
import org.bukkit.event.EventHandler;

@SkillMetadata(id = 202, skillType = SkillType.Druid, invType = InvType.INNATE)
public class LivingMana extends Passive implements IEnergy {
    @Override
    public String getName() {
        return "Living Mana";
    }

    @Override
    public int getEnergyUsage() {
        return 0;
    }

    @EventHandler
    public void hit(DamageApplyEvent event) {
        if(event.isCancelled()) return;
        if(event.getCause() == Cause.CUSTOM) return;
        if(isAlly(event.getVictim())) return;
        if(event.getAttacker() == getPlayer()) {
            getEnergyBar().incrementEnergy(10);
        }
    }
}
