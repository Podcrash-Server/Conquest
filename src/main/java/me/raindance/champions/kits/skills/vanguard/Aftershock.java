package me.raindance.champions.kits.skills.vanguard;

import com.abstractpackets.packetwrapper.AbstractPacket;
import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.damage.DamageApplier;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import com.podcrash.api.mc.sound.SoundPlayer;
import com.podcrash.api.mc.time.TimeHandler;
import com.podcrash.api.mc.time.resources.TimeResource;
import com.podcrash.api.mc.util.PacketUtil;
import com.podcrash.api.mc.world.BlockUtil;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.action.ICooldown;
import me.raindance.champions.kits.skilltypes.Instant;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.Set;

//@SkillMetadata(id = 810, skillType = SkillType.Vanguard, invType = InvType.SHOVEL)
public class Aftershock extends Instant implements ICooldown, TimeResource {
    private float cooldown = 13;
    private double chargeTime = 3;          // How long it takes to charge, in seconds.
    private int radius = 5;
    private double launchYValue = 0.65;
    private double damage = 6;
    private float duration = 4f;

    private final Random random = new Random();
    private int i = 0;

    @Override
    public float getCooldown() {
        return cooldown;
    }

    @Override
    protected void doSkill(PlayerEvent event, Action action) {
        if(!rightClickCheck(action)) return;
        SoundPlayer.sendSound(getPlayer().getLocation(), "creeper.primed", 1.5F, 63);
        setLastUsed(System.currentTimeMillis());
        TimeHandler.repeatedTime(20, 0, this);
    }

    @Override
    public String getName() {
        return "Aftershock";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SHOVEL;
    }

    @Override
    public void task() {
        i += 20;
    }

    @Override
    public boolean cancel() {
        return (i >= (chargeTime * 20)) || getGame().isRespawning(getPlayer());
    }

    @Override
    public void cleanup() {
        if(i >= (chargeTime * 20)) {
            WrapperPlayServerWorldParticles explosion = ParticleGenerator.createParticle(null, EnumWrappers.Particle.EXPLOSION_HUGE, 1, 0,0,0);
            explosion.setLocation(getPlayer().getLocation());
            PacketUtil.syncSend(explosion, getPlayers());

            SoundPlayer.sendSound(getPlayer().getLocation(), "random.explode", 0.9F, 70);

            for(Player player : BlockUtil.getPlayersInArea(getPlayer().getLocation(), radius, getPlayers())) {
                if (player == getPlayer() || isAlly(player)) continue;
                player.setVelocity(player.getVelocity().setY(launchYValue));
                DamageApplier.damage(player, getPlayer(), damage, this, false);
                StatusApplier.getOrNew(player).applyStatus(Status.SHOCK, duration, 0);
            }
            getPlayer().sendMessage(getUsedMessage());
            i = 0;
        }
    }
}
