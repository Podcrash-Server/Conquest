package me.raindance.champions.damage;

import com.comphenix.packetwrapper.WrapperPlayServerEntityStatus;
import me.raindance.champions.Main;
import me.raindance.champions.effect.status.StatusApplier;
import me.raindance.champions.events.DamageApplyEvent;
import me.raindance.champions.events.DeathApplyEvent;
import me.raindance.champions.kits.ChampionsPlayer;
import me.raindance.champions.kits.ChampionsPlayerManager;
import me.raindance.champions.kits.Skill;
import me.raindance.champions.kits.classes.Assassin;
import me.raindance.champions.kits.classes.Brute;
import me.raindance.champions.sound.SoundPlayer;
import net.minecraft.server.v1_8_R3.ItemArmor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class DamageQueue implements Runnable {
    public static boolean active = false;
    private static final Deque<Damage> damages = new ArrayDeque<>();
    //String = victim, stack damage - past damage instances -> used to find last damage cause for death events
    private static final Map<String, ArrayDeque<Damage>> damageHistory = new HashMap<>();
    /**
     * Processes the damages, without
     * @see Thread#run()
     */
    @Override
    public void run() {
        while (damages.peek() != null) processDamage(damages.poll());
    }

    /**
     * This method gets a name as usage for a key for damageHistory. {@link DamageQueue#damageHistory}
     * If the entity is a player, just return its player name.
     * If it's a mob, return its type and concat its id.
     * @param entity - the entity whose name will be extracted
     * @return the name of the entity as usage for a key
     */
    private String getNameFor(Entity entity) {
        if(entity instanceof Player) return entity.getName();
        else {
            String name = entity.getName();
            if(name == null) name = entity.getCustomName();
            if(name == null) name = entity.getType().name() + entity.getEntityId();
            return name;
        }
    }

    /**
     * Adds as a cache to the victim, used for player deaths.
     * @param victim - the entity that is getting attacked
     * @param damageWrapper - the damage dataclass concerning the victim
     */
    private void addHistory(Entity victim, Damage damageWrapper) {
        String name = getNameFor(victim);
        if(!damageHistory.containsKey(name)) {
            damageHistory.put(name, new ArrayDeque<>());
        }
        damageHistory.get(name).add(damageWrapper);
    }

    /**
     * Self-explanatory, clear the victim's history (presumably after the victim dies)
     * @param victimName the key for the damageHistory {@link DamageQueue#damageHistory)
     */
    private void clearHistory(String victimName) {
        if(damageHistory.containsKey(victimName))
            damageHistory.get(victimName).clear();
    }

    /**
     * See {@link DamageQueue#clearHistory(String)}
     * @param victim
     */
    private void clearHistory(Entity victim) {
        clearHistory(getNameFor(victim));
    }

    /**
     * Calculate the armor value of the entity.
     * Very important in finding how much damage an entity should recieve.
     * @param entity the entity
     * @return armor value of the entity
     */
    private int armorValue(LivingEntity entity) {
        int i = 0;
        for(ItemStack armor : entity.getEquipment().getArmorContents()) {
            net.minecraft.server.v1_8_R3.ItemStack nmsArmor = CraftItemStack.asNMSCopy(armor);
            if(nmsArmor != null && nmsArmor.getItem() instanceof ItemArmor) {
                i += ((ItemArmor) nmsArmor.getItem()).c;
            }
        }
        return i;
    }

    /**
     * Damage the entity with an amount of damage specified.
     * This uses setHealth, instead of damage, so that some stuff are skipped by the Bukkit API.
     * Because of that, a lot of this stuff is experimental.
     * This method also handles the damaging part synchronously, which is required.
     * @param entity the entity that will be damaged
     * @param damage the amount of damage
     */
    private void damageEntity(LivingEntity entity, double damage) {
        final double future;
        boolean a = false;
        if(entity instanceof Player) {
            double health = entity.getHealth() - damage;
            if (health > entity.getMaxHealth()) {
                health = entity.getMaxHealth(); //this will never happen, but just in case
                a = false;
            } else if (health < 0) {
                health = 0;
                a = true;
            }
            future = health;
        }else future = damage;

        final boolean b = a;
        Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
            if(entity instanceof Player) {
                if(b) {
                    PlayerInventory inventory = ((Player) entity).getInventory();
                    inventory.clear();
                    inventory.setArmorContents(new ItemStack[]{null, null, null, null});

                }
                entity.setHealth(future);
                if(entity.isDead() || entity.getHealth() <= 0) die(entity);
            } else entity.damage(future);
        }, 0L);
    }

    /**
     * Custom strength/resistance/weakness handling.
     * Where x is the potency of each potion:
     * attacker's strength = + x
     * attacker's weakness = - x
     * victim's resistance = - x
     * @param victim self-explanatory
     * @param attacker self-explanatory
     * @return the bonus for dealing more or less damage.
     */
    private int findPotionBonus(LivingEntity victim, LivingEntity attacker) {
        int bonus = 0;
        for (PotionEffect potion : attacker.getActivePotionEffects()) {
            if (potion.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                bonus += potion.getAmplifier() + 1;
            }
            if (potion.getType().equals(PotionEffectType.WEAKNESS)) {
                bonus -= potion.getAmplifier() + 1;
            }
        }
        for (PotionEffect potion : victim.getActivePotionEffects()) {
            if (potion.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
                bonus -= potion.getAmplifier() + 1;
            }
        }
        return bonus;
    }

    /**
     * Damage calculations for {@link DamageQueue#damageEntity(LivingEntity, double)}
     * @param entity the victim
     * @param damage the unfiltered damage
     * @param armorValue armor value of the victim, see {@link DamageQueue#armorValue(LivingEntity)}
     */
    private void damage(LivingEntity entity, double damage, int armorValue) {
        if(entity instanceof Player) {
            ChampionsPlayer victim = ChampionsPlayerManager.getInstance().getChampionsPlayer((Player) entity);
            if(victim instanceof Brute) {
                damage += 8D;
            }
        }
        double damageFormula = damage * (1D - 0.04D * armorValue);
        //Bukkit.broadcastMessage("AV: " + armorValue  + " " + damage + " --> " + damageFormula);
        damageEntity(entity, damageFormula);
    }

    /**
     * On the offchance that an entity dies right after they get damaged.
     * Note: this does not cover fire damage, wither damage, and the like.
     * @param victim
     */
    private void die(LivingEntity victim) {
        String name = getNameFor(victim);
        ArrayDeque<Damage> history = damageHistory.get(name);
        if(history.size() == 0) return;
        Damage damage = history.removeLast();
        Bukkit.getPluginManager().callEvent(new DeathApplyEvent(damage));
        clearHistory(name);
    }

    /**
     * Plays a sound for the victim.
     * If the attacker shot the victim with a bow, play a satisfying ding sound.
     * @param victim
     * @param attacker
     * @param cause
     */
    private void playSound(LivingEntity victim, LivingEntity attacker, Cause cause) {
        final World world = victim.getWorld();
        if(victim instanceof Player) {
            ChampionsPlayer championVictim = ChampionsPlayerManager.getInstance().getChampionsPlayer((Player) victim);
            if(championVictim != null)
                SoundPlayer.sendSound(victim.getLocation(), championVictim.getSound());
        }
        if(attacker instanceof Player && cause == Cause.PROJECTILE) {
            SoundPlayer.sendSound((Player) attacker, "random.successful_hit", 0.8F, 20);
        }
    }

    /**
     * See {@link DamageApplier#nativeApplyKnockback(LivingEntity, LivingEntity, double[])}
     * @param victim
     * @param attacker
     * @param velocityModifiers
     */
    private void applyKnockback(LivingEntity victim, LivingEntity attacker, double[] velocityModifiers) {
        if(velocityModifiers == null) velocityModifiers = new double[] {1D, 1D, 1D};
        DamageApplier.nativeApplyKnockback(victim, attacker, velocityModifiers);
    }

    /**
     * Send the attack packets
     */
    private void sendUsePacket(LivingEntity victim){
        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus();
        packet.setEntityId(victim.getEntityId());
        packet.setEntityStatus(WrapperPlayServerEntityStatus.Status.ENTITY_HURT);
        for(Player player : victim.getWorld().getPlayers()) packet.sendPacket(player);
    }

    /**
     * The main method of this runnable. It does all of the above.
     * @param damageWrapper
     */
    private void processDamage(Damage damageWrapper) {
        LivingEntity victim = damageWrapper.getVictim();
        LivingEntity attacker = damageWrapper.getAttacker();
        Cause cause = damageWrapper.getCause();

        if(victim instanceof Player && cause == Cause.MELEE && StatusApplier.getOrNew((Player) victim).isCloaked()) return;
        DamageApplyEvent damageEvent = new DamageApplyEvent(victim, attacker, damageWrapper.getDamage(), cause,
                damageWrapper.getArrow(), damageWrapper.getSkills(), damageWrapper.isApplyKnockback());
        double damage = damageEvent.getDamage();
        Bukkit.getPluginManager().callEvent(damageEvent);

        if(damageEvent.isCancelled() || damageEvent.getAttacker() == damageEvent.getVictim()) return;
        if(damageEvent.isModified()) damage = damageEvent.getDamage();
        if((cause == Cause.MELEE || cause == Cause.MELEESKILL) && damageWrapper.getArrow() == null)
            damage += findPotionBonus(victim, attacker);

        addHistory(victim, damageWrapper);
        int armorValue = armorValue(victim);
        playSound(victim, attacker, cause);
        if(attacker instanceof Player) ((Player) attacker).setLevel((int) damage);
        sendUsePacket(victim);
        damage(victim, damage, armorValue);
        if(!damageEvent.isDoKnockback()) return;
        double[] velocity = damageEvent.getVelocityModifiers();
        if(cause == Cause.PROJECTILE && damageWrapper.getArrow() != null) {
            if(damage < 1) damage = 1;
            double multiplier = (Math.log(damage) / 3d);
            if(multiplier < 0) multiplier = 1; //uhs
            velocity[0] *= multiplier;
            velocity[2] *= multiplier;
        }
        if((cause == Cause.MELEE || cause == Cause.MELEESKILL) && attacker instanceof Player) {
            if(ChampionsPlayerManager.getInstance().getChampionsPlayer((Player) attacker) instanceof Assassin)
                return;
            if(velocity[0] != 1 && velocity[1] != 1 && velocity[2] != 1) {
                double multiplier = .05 * damage + 0.65;
                velocity[0] *= multiplier;
                velocity[1] *= multiplier;
                velocity[2] *= multiplier;
            }
        }
        applyKnockback(victim, attacker, velocity);
    }

    /**
     * For PlayerDeathEvents.
     * @param player - the player you want to do it for
     */
    public static void artificialDie(Player player) {
        if(player.isDead()) return;
        ArrayDeque<Damage> damages = damageHistory.get(player.getName());
        player.getInventory().clear();
        Damage damage;
        final Damage nullDamage = new Damage(player, player, -99, Cause.NULL, null, (Skill) null, false);
        if(damages == null) damage = nullDamage;
        else {
            if(damages.size() != 0) {

                damage = damages.removeLast();
                damages.clear();
            }else {
                damage = nullDamage;
            }//ie this dude hasn't done anything
        }
        Bukkit.getPluginManager().callEvent(new DeathApplyEvent(damage));

    }

    public static Deque<Damage> getDamages() {
        return damages;
    }
}
