package me.raindance.champions.kits.itemskill.item;

import com.abstractpackets.packetwrapper.AbstractPacket;
import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.damage.DamageApplier;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.item.ItemManipulationManager;
import com.podcrash.api.mc.sound.SoundPlayer;
import com.podcrash.api.mc.util.VectorUtil;
import me.raindance.champions.kits.annotation.ItemMetaData;
import me.raindance.champions.kits.itemskill.TrapItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.List;

@ItemMetaData(mat = Material.TNT, actions = {Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK, Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK})
public class Landmine extends TrapItem {
    public Landmine() {
        super(5000);
    }

    @Override
    protected Item throwItem(Player player, Action action) {
        Location location = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        Vector vector = new Vector(0, 0, 0);
        if(isLeft(action)) vector = throwVector(direction);
        return ItemManipulationManager.regular(Material.TNT, location, vector);
    }

    @Override
    protected void primeTrap(Item item) {
        AbstractPacket packet2 = ParticleGenerator.createBlockEffect(item.getLocation().toVector(), Material.OBSIDIAN.getId());
        for(Player p : item.getWorld().getPlayers()) {
            packet2.sendPacket(p);
        }
    }

    @Override
    protected void snareTrap(Player owner, Player player, Item item) {
        Location location = item.getLocation();
        WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(EnumWrappers.Particle.EXPLOSION_HUGE, 3);
        packet.setLocation(location);

        List<LivingEntity> entities = location.getWorld().getLivingEntities();

        final Vector up = new Vector(0, .75, 0);
        for(LivingEntity entity : entities) {
            double distanceSquared = location.distanceSquared(entity.getLocation());
            if(distanceSquared > 36) continue;
            double percentage = (36D - distanceSquared)/36D + 0.25D;
            if(percentage > 1D) percentage = 1D;
            Vector vector = VectorUtil.fromAtoB(location, entity.getLocation()).add(up).normalize();
            vector.multiply(3 * percentage);
            entity.setVelocity(vector);
            DamageApplier.damage(entity, owner, 8 * percentage, this, false);
            if(entity instanceof Player)
                packet.sendPacket((Player) entity);
        }

        SoundPlayer.sendSound(location, "random.explode", 0.85F, 126);
    }

    @Override
    public String getName() {
        return "Landmine";
    }
}
