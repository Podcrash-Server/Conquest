package me.raindance.champions.kits.skills.hunter;

import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import com.podcrash.api.mc.events.DamageApplyEvent;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.action.ICooldown;
import me.raindance.champions.kits.skilltypes.Instant;
import com.podcrash.api.mc.time.TimeHandler;
import com.podcrash.api.mc.time.resources.TimeResource;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SkillMetadata(id = 406, skillType = SkillType.Hunter, invType = InvType.AXE)
public class Swiftness extends Instant implements TimeResource, ICooldown {
    private final int selfEffect = 4;
    private final float selfReduction = 0.4F;
    private boolean _active;
    private final Random rand = new Random();

    public Swiftness() {
        super();
    }

    @Override
    public float getCooldown() {
        return 14;
    }

    @Override
    public String getName() {
        return "Swiftness";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.AXE;
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void leftclickBlock(PlayerInteractEvent e) {
        if (e.getPlayer() == getPlayer() && _active &&
                (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            _active = false;
        }
    }

    @Override
    protected void doSkill(PlayerInteractEvent event, Action action) {
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (_active) _active = false;
        } else if (!onCooldown() && action != Action.PHYSICAL) {
            StatusApplier.getOrNew(getPlayer()).applyStatus(Status.SPEED, selfEffect, 1, false);
            _active = true;
            getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.NOTE_PLING, 0.5f, 0.5f);
            this.setLastUsed(System.currentTimeMillis());
            TimeHandler.repeatedTime(1, 0, this);
            getPlayer().sendMessage(getUsedMessage());
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void hit(DamageApplyEvent e) {
        if (_active && getPlayer() == e.getVictim() && getPlayer().isSprinting()) {
            e.setModified(true);
            e.setDoKnockback(false);
            e.setDamage(e.getDamage() * (1D - selfReduction));
            getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.BLAZE_BREATH, 0.5f, 2);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void hit(EntityDamageEvent event) {
        final List<EntityDamageEvent.DamageCause> causes = Arrays.asList(
                EntityDamageEvent.DamageCause.FALL, EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK);
        if (_active && event.getEntity() == getPlayer() && getPlayer().isSprinting() && causes.contains(event.getCause())) {
            event.setDamage(event.getDamage() * (1D - selfReduction));
            getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.BLAZE_BREATH, 0.5f, 2);
        }
    }

    @Override
    public void task() {
        if (System.currentTimeMillis() - getLastUsed() >= selfEffect * 1000L) _active = false;
        if (!getPlayer().isSprinting()) return;
        WrapperPlayServerWorldParticles particle = ParticleGenerator.createParticle(getPlayer().getLocation().toVector(),
                EnumWrappers.Particle.SPELL, new int[]{255, 255, 255, 0}, 9,
                rand.nextFloat() / 2f, 0.25f + (rand.nextFloat() - 0.15f), rand.nextFloat() / 2f);
        getPlayer().getWorld().getPlayers().forEach(player -> ParticleGenerator.generate(player, particle));
        //TODO: PARTICLE GENERATOR
    }

    @Override
    public boolean cancel() {
        return !_active;
    }

    @Override
    public void cleanup() {
        _active = false;
        StatusApplier.getOrNew(getPlayer()).removeVanilla(Status.SPEED);
    }


}
