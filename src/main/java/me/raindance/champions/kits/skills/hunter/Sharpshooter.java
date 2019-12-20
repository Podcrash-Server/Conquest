package me.raindance.champions.kits.skills.hunter;

import com.podcrash.api.mc.damage.Cause;
import com.podcrash.api.mc.events.DamageApplyEvent;
import me.raindance.champions.Main;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.action.ICharge;
import me.raindance.champions.kits.skilltypes.Passive;
import com.podcrash.api.mc.sound.SoundPlayer;
import com.podcrash.api.mc.time.TimeHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Arrays;
import java.util.HashMap;

@SkillMetadata(skillType = SkillType.Hunter, invType = InvType.INNATE)
public class Sharpshooter extends Passive implements ICharge {
    private final HashMap<Integer, Float> forceMap = new HashMap<>();
    private final int MAX_CHARGES = 4;
    private int charges = 0;
    private int miss = 0;
    private long time;
    private boolean justMissed;

    public Sharpshooter() {
        super();
    }

    @Override
    public String getName() {
        return "Sharpshooter";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.NULL;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void shoot(EntityShootBowEvent event){
        if(event.getEntity() == getPlayer() && event.getProjectile() instanceof Arrow) {
            forceMap.put(event.getProjectile().getEntityId(), event.getForce());
        }
    }

    private boolean checkIfValidShooter(DamageApplyEvent e){
        return !e.isCancelled() && e.getVictim() != getPlayer() && e.getAttacker() == getPlayer()
                && e.getArrow() != null && e.getCause() == Cause.PROJECTILE;
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void shoot(DamageApplyEvent e) {
        if (checkIfValidShooter(e)) {
            justMissed = false;
            time = System.currentTimeMillis();
            e.setModified(true);
            e.setDamage(e.getDamage() + getCurrentCharges() * 2);
            int id = e.getArrow().getEntityId();
            if(forceMap.get(id) >= 0.9F) addCharge();
            forceMap.remove(id);
            getPlayer().sendMessage(String.format("%s bonus: %d", getName(), getCurrentCharges()));
            e.addSource(this);
            playSound();
            miss = 0;
            start();
            Bukkit.getScheduler().runTaskLater(Main.instance, () -> justMissed = true, 3L);
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void hit(ProjectileHitEvent e) {
        if (e.getEntity().getShooter() == getPlayer()) {
            Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
                if (justMissed) {
                    miss++;
                    if (miss >= 2) {
                        miss = 0;
                        if (charges != 0) {
                            resetCharge();
                            playSound();
                            getPlayer().sendMessage(String.format("%s bonus: %d", getName(), getCurrentCharges()));
                        }
                    }
                }
            }, 1L);
        }
    }

    @Override
    public void addCharge() {
        if (charges < MAX_CHARGES) charges++;
    }

    @Override
    public int getCurrentCharges() {
        return charges;
    }

    @Override
    public int getMaxCharges() {
        return MAX_CHARGES;
    }

    public void resetCharge() {
        charges = 0;
    }

    private void start() {
        stop();
        TimeHandler.repeatedTime(1, 0, this);
    }

    private void stop() {
        TimeHandler.unregister(this);
    }

    @Override
    public void task() {

    }

    @Override
    public boolean cancel() {
        return System.currentTimeMillis() - time >= 5000L;
    }

    @Override
    public void cleanup() {
        resetCharge();
        getPlayer().sendMessage(String.format("%s%s bonus: %s%d", ChatColor.GRAY, getName(), ChatColor.GREEN, getCurrentCharges()));
        playSound();
    }

    private void playSound() {
        float i = (((float) getCurrentCharges()) / ((float) getMaxCharges()));
        SoundPlayer.sendSound(this.getPlayer(), "note.harp", 0.75f, (int) (130 * i));
    }
}
