package me.raindance.champions.kits.skills.hunter;

import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import com.podcrash.api.mc.events.DamageApplyEvent;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.skilltypes.Continuous;
import org.bukkit.event.EventHandler;

import java.util.Random;

@SkillMetadata(id = 409, skillType = SkillType.Hunter, invType = InvType.SWORD)
public class Rest extends Continuous {
    private final long duration = 3000L;
    private boolean active;
    private Random rand;
    private StatusApplier applier;
    private long start;
    private boolean effectActive;
    @Override
    protected void doContinuousSkill() {
        rand = new Random();
        active = true;
        applier = StatusApplier.getOrNew(getPlayer());
        start = System.currentTimeMillis();
        effectActive = false;
        startContinuousAction();
    }

    @Override
    public void task() {
        sendParticles();

        //if not enough time has passed, just wait until there is enough time
        if(!effectActive && System.currentTimeMillis() - start < duration) return;
        applier.applyStatus(Status.REGENERATION, Integer.MAX_VALUE, 0);
        applier.applyStatus(Status.CLOAK, Integer.MAX_VALUE, 0);
        getPlayer().sendMessage(getUsedMessage().replace("used", "activated"));
        effectActive = true;
    }

    private void sendParticles() {
        if(applier.has(Status.CLOAK)) return;
        WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(getPlayer().getLocation().toVector(), EnumWrappers.Particle.HEART,
                3, rand.nextFloat(), 0.9f, rand.nextFloat());
        getPlayer().getWorld().getPlayers().forEach(p -> ParticleGenerator.generate(p, packet));
    }
    @Override
    public boolean cancel() {
        return !getPlayer().isBlocking() || !active;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        StatusApplier.getOrNew(getPlayer()).removeStatus(Status.REGENERATION, Status.CLOAK);
    }

    @Override
    public String getName() {
        return "Rest";
    }

    @EventHandler
    public void damage(DamageApplyEvent event) {
        if(!active || event.getVictim() != getPlayer()) return;
        active = false;
        getPlayer().sendMessage("Skill> " + event.getVictim() + " cancelled " + getName() + ".");
    }

}
