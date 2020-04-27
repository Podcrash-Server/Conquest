package me.raindance.champions.kits.skills.warden;

import com.podcrash.api.damage.DamageApplier;
import com.podcrash.api.effect.particle.ParticleGenerator;
import com.podcrash.api.util.EntityUtil;
import com.podcrash.api.util.VectorUtil;
import me.raindance.champions.annotation.kits.SkillMetadata;
import com.podcrash.api.kits.enums.InvType;
import com.podcrash.api.kits.enums.ItemType;
import me.raindance.champions.kits.SkillType;
import com.podcrash.api.kits.iskilltypes.action.ICooldown;
import com.podcrash.api.kits.skilltypes.Instant;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import java.util.List;

@SkillMetadata(id = 905, skillType = SkillType.Warden, invType = InvType.SWORD)
public class EarthSmash extends Instant implements ICooldown {
    private double radiusSquared = 4.5 * 4.5;
    @Override
    public float getCooldown() {
        return 10;
    }

    @Override
    public void doSkill(PlayerEvent event, Action action) {
        if(!rightClickCheck(action) || onCooldown()) return;
        if(!EntityUtil.onGround(getPlayer())) {
            getPlayer().sendMessage(getMustAirborneMessage());
            return;
        }
        setLastUsed(System.currentTimeMillis());
        Location location = getPlayer().getLocation();
        List<LivingEntity> players = location.getWorld().getLivingEntities();
        for(LivingEntity enemy : players) {
            if(getPlayer() == enemy) continue;
            double dist = location.distanceSquared(enemy.getLocation());
            if(dist > radiusSquared) continue;
            pound(location, enemy, 1.33333D - ((16D - dist)/16D));
        }
        ParticleGenerator.generateRangeParticles(location, 8, true, 4);

        getPlayer().sendMessage(getUsedMessage());
    }

    private void pound(Location currentLoc, LivingEntity entity, double multiplier) {
        if(multiplier > 1) multiplier = 1;
        if(!isAlly(entity)) DamageApplier.damage(entity, getPlayer(), multiplier * 5D, this, false);
        Vector vector = VectorUtil.fromAtoB(currentLoc, entity.getLocation()).normalize();
        vector.multiply(multiplier * 1.25D).setY(vector.getY() + 1);
        if(vector.getY() > 1D) vector.setY(1D);
        entity.setVelocity(vector);
    }

    @Override
    public String getName() {
        return "Earth Smash";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SWORD;
    }
}
