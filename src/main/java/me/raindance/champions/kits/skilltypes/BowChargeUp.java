package me.raindance.champions.kits.skilltypes;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.raindance.champions.damage.Cause;
import me.raindance.champions.events.DamageApplyEvent;
import me.raindance.champions.events.skill.SkillUseEvent;
import me.raindance.champions.kits.Skill;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.sound.SoundPlayer;
import me.raindance.champions.time.TimeHandler;
import me.raindance.champions.time.resources.TimeResource;
import me.raindance.champions.util.TitleSender;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumAnimation;
import net.minecraft.server.v1_8_R3.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public abstract class BowChargeUp extends Skill implements TimeResource {
    private Map<String, Long> times = new HashMap<>();
    protected boolean isUsing = false;
    private float power = 0;
    protected float rate = 0;
    private HashMap<Arrow, Float> charges;
    public BowChargeUp(Player player, String name, int level, SkillType type, InvType invType, float cooldown, float rate) {
        super(player, name, level, type, ItemType.BOW, invType, cooldown);
        this.rate = rate;
        charges = new HashMap<>();

    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void block(PlayerInteractEvent e){
        if(e.getPlayer() == this.getPlayer()){
            if(rightClickCheck(e.getAction()) && isHolding()){
                if(!onCooldown()) {
                    SkillUseEvent useEvent = new SkillUseEvent(this);
                    Bukkit.getPluginManager().callEvent(useEvent);
                    if(useEvent.isCancelled()) return;
                    if(!isInWater()) {
                        times.put(getPlayer().getName(), System.currentTimeMillis());
                        unregister();
                        TimeHandler.repeatedTime(1, 0, this);
                    } else getPlayer().sendMessage(getWaterMessage());
                }
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void shoot(EntityShootBowEvent e){
        if(e.getEntity() == getPlayer() && e.getProjectile() instanceof Arrow){
            Arrow a = (Arrow) e.getProjectile();
            charges.put(a, getCharge());
            doShoot(a, charges.get(a));
            resetCharge();
        }
    }


    public abstract void doShoot(Arrow arrow, float charge);

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void shoot(DamageApplyEvent e){
        if(e.getAttacker() == getPlayer() && charges.containsKey(e.getArrow()) && e.getCause() == Cause.PROJECTILE){
            shootPlayer(e.getArrow(), charges.get(e.getArrow()), e);
        }
    }

    public abstract void shootPlayer(Arrow arrow, float charge, DamageApplyEvent e);


    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void ground(ProjectileHitEvent e){
        Projectile proj = e.getEntity();
        if(proj instanceof Arrow){
            Arrow arrow = (Arrow) e.getEntity();
            if(proj.getShooter() == getPlayer() && charges.containsKey(arrow)){
                shootGround(arrow, charges.get(arrow));
            }
        }
    }
    public abstract void shootGround(Arrow arrow, float charge);

    @Override
    public void task() {
        if(System.currentTimeMillis() - times.get(getPlayer().getName()) >= 1500L) {
            charge();
            isUsing = true;
            WrappedChatComponent progress = TitleSender.chargeUpProgressBar(instance, getCharge());
            if (getCharge() < 1f)
                SoundPlayer.sendSound(getPlayer(), "note.harp", 0.75f, (int) (130 * getCharge()));
            TitleSender.sendTitle(getPlayer(), progress);
        }
    }

    @Override
    public boolean cancel() {
        boolean truth = true;
        if(getPlayer().getItemInHand() != null) {
            EntityPlayer ep = ((CraftPlayer) getPlayer()).getHandle();
            ItemStack itemStack = CraftItemStack.asNMSCopy(getPlayer().getItemInHand());
            if(itemStack == null) return true;
            truth = !(ep.bS() && itemStack.getItem().e(itemStack) == EnumAnimation.BOW);
        }
        return truth;
    }

    @Override
    public void cleanup() {
        TimeHandler.unregister(this);
        times.remove(getPlayer().getName());
        isUsing = false;
        this.resetCharge();
    }


    protected void charge(){
        power += rate;
        power = (power >= 1f) ? 1f : power;
    }
    protected void charge(double boost){
        power += boost;
    }

    protected float getCharge() {
        return power;
    }

    protected void resetCharge(){
        power = 0;
    }
}
