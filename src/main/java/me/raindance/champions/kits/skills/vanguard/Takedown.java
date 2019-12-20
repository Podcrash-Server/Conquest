package me.raindance.champions.kits.skills.vanguard;

import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.callback.sources.CollideBeforeHitGround;
import com.podcrash.api.mc.damage.DamageApplier;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import me.raindance.champions.events.skill.SkillUseEvent;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.action.IConstruct;
import me.raindance.champions.kits.iskilltypes.action.ICooldown;
import me.raindance.champions.kits.skilltypes.Instant;
import com.podcrash.api.mc.util.EntityUtil;
import com.podcrash.api.mc.util.PacketUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

@SkillMetadata(skillType = SkillType.Vanguard, invType = InvType.AXE)
public class Takedown extends Instant implements ICooldown, IConstruct {
    private final float hitbox = 0.55f;
    private CollideBeforeHitGround hitGround;

    @Override
    public float getCooldown() {
        return 19;
    }

    @Override
    public String getName() {
        return "Takedown";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.AXE;
    }

    @Override
    public void afterConstruction() {

        WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(EnumWrappers.Particle.CRIT, 2);
        this.hitGround = new CollideBeforeHitGround(getPlayer())
                .changeEvaluation(() -> (getPlayer().getNearbyEntities(hitbox, hitbox, hitbox).size() > 0) || EntityUtil.onGround(getPlayer()))
                .then(() -> {
                    SkillUseEvent event = new SkillUseEvent(this);
                    Bukkit.getPluginManager().callEvent(event);
                    if(event.isCancelled()) return;
                    List<Entity> entities = getPlayer().getNearbyEntities(hitbox, hitbox, hitbox);
                    if (entities.size() == 0) return;
                    getPlayer().setVelocity(new Vector(0, 0, 0));
                    for (Entity entity : entities) {
                        if (entity instanceof Player && entity != getPlayer()) {
                            getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.ZOMBIE_WOOD, 2f, 0.2f);
                            DamageApplier.damage((LivingEntity) entity, getPlayer(), 5, this, false);
                            /*
                            StatusApplier.getOrNew((Player) entity).applyStatus(Status.SLOW, effect, 3);
                            StatusApplier.getOrNew(getPlayer()).applyStatus(Status.SLOW, effect, 3);

                             */

                            StatusApplier.getOrNew((Player) entity).applyStatus(Status.GROUND, 2, 3);
                            StatusApplier.getOrNew(getPlayer()).applyStatus(Status.GROUND, 2, 3);

                            getPlayer().sendMessage(String.format("%s%s> You used %sTakedown%s on %s", ChatColor.BLUE, getChampionsPlayer().getName(), ChatColor.GREEN, ChatColor.GRAY, entity.getName()));

                            break; //only attack one player
                        }
                    }
                }).doWhile(() -> {
                    packet.setLocation(getPlayer().getLocation());
                    PacketUtil.asyncSend(packet, getPlayers());
                });
    }

    @Override
    protected void doSkill(PlayerInteractEvent event, Action action) {
        if (!rightClickCheck(action) || EntityUtil.onGround(getPlayer())) {
            if(!onCooldown()) {
                getPlayer().sendMessage(String.format("%s%s> %sYou cannot use %sTakedown%s while grounded.",
                        ChatColor.BLUE, getChampionsPlayer().getName() , ChatColor.GRAY, ChatColor.GREEN, ChatColor.GRAY));
            }
            return;
        }
        if (!onCooldown()) {
            this.setLastUsed(System.currentTimeMillis());
            Vector vector = getPlayer().getLocation().getDirection().normalize().multiply(1.1d).setY(0f);
            getPlayer().setVelocity(vector);
            getPlayer().setFallDistance(0);
            hitGround.run();
        }
    }
}