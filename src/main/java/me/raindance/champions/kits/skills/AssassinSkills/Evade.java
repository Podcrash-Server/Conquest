package me.raindance.champions.kits.skills.AssassinSkills;

import me.raindance.champions.Main;
import me.raindance.champions.damage.Cause;
import me.raindance.champions.events.DamageApplyEvent;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.skilltypes.Instant;
import com.podcrash.api.mc.sound.SoundPlayer;
import com.podcrash.api.mc.time.TimeHandler;
import com.podcrash.api.mc.time.resources.TimeResource;
import com.podcrash.api.mc.util.MathUtil;
import com.podcrash.api.mc.util.TitleSender;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class Evade extends Instant implements TimeResource {
    private boolean isEvading = false;
    private final int MAX_LEVEL = 1;
    private final int SKILL_TOKEN_WEIGHT = 2;
    private long time;

    public Evade(Player player, int level) {
        super(
                player,
                "Evade",
                level,
                SkillType.Assassin,
                ItemType.SWORD,
                InvType.SWORD,
                15 - 5 * level);
        setDesc("Hold Block against melee attacks to Evade them and",
                "teleport behind the attacker.",
                "",
                "Crouch and Evade to teleport backwards.");
    }

    @Override
    public int getSkillTokenWeight() {
        return SKILL_TOKEN_WEIGHT;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    protected void doSkill(PlayerInteractEvent event, Action action) {
        if (!rightClickCheck(action)) return;
        if (!onCooldown()) {
            time = System.currentTimeMillis();
            isEvading = true;
            TimeHandler.repeatedTime(1, 0, this);
        } //else this.getEntity().sendMessage(getCooldownMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void hit(DamageApplyEvent event) {
        if (event.getCause() != Cause.MELEE || event.isCancelled()) return;
        LivingEntity player = event.getVictim();
        if (player == getPlayer() && isEvading) {
            LivingEntity damager = event.getAttacker();
            player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 5);
            isEvading = false;
            event.setCancelled(true);
            TimeHandler.unregister(this);

            Location tp = d((Player) player, damager);
            player.sendMessage(getUsedMessage());
            damager.sendMessage(String.format("%sSkill> %s%s %sused %s%s %d%s.",
                    ChatColor.BLUE, ChatColor.GREEN, player.getName(), ChatColor.GRAY, ChatColor.GREEN, getName(), getLevel(), ChatColor.GRAY));
            player.teleport(tp);
            SoundPlayer.sendSound(getPlayer(), "random.successful_hit", 0.8F, 20);

            player.setFallDistance(0);
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, () -> this.setLastUsed(0L), 1L);
        }
    }

    private Location d(Player victim, LivingEntity damager) {
        Location victimLocation = victim.getLocation();
        Location damagerLocation = damager.getLocation();
        return victim.isSneaking()
                ? shiftEvade(victimLocation, damagerLocation)
                : evade(victimLocation, damagerLocation);
    }

    private Location evade(Location victimLocation, Location damagerLocation) {
        // Based on the damager's location
        // End Location
        double constantY = damagerLocation.getY() + 0.01d;
        Location tempLocation = damagerLocation.clone();
        Vector vect = damagerLocation.getDirection().normalize().multiply(-2.0);
        Location behind = damagerLocation.add(vect);
        double d = 0.05;
        double inc = 0.05;
        double x2 = behind.getX(), y2 = behind.getY(), z2 = behind.getZ();
        // x1,y2,z1
        double x1 = tempLocation.getX(), y1 = tempLocation.getY(), z1 = tempLocation.getZ();
        // Possible points
        while (d < 1) {
            // x2,y2,z2

            double x = x1 + d * (x2 - x1);
            double y = constantY; // y1 + 0.05d * (y2 - y1); // this stays constant
            double z = z1 + d * (z2 - z1);
            Location testLocation = new Location(victimLocation.getWorld(), x, y, z);

            d += inc;

            if (!testLocation.getBlock().isEmpty()) {
                break;
            }
            tempLocation = testLocation;
            if (testLocation.distanceSquared(behind) < 0.20) {
                break;
            }
        }
        return tempLocation.setDirection(damagerLocation.getDirection());
    }
  /*
  Vector vect = damagerLocation.getDirection().normalize().multiply(-2.4);
  Location behind = damagerLocation.add(vect);
  int i = 0;
  while(!behind.getBlock().isEmpty()){
      if(behind.getBlock().isLiquid()) break;
      behind.add(damagerLocation.getDirection().add(new Vector(0, 0.01, 0)));
      i++;
      if(i > 10) {
          behind = damagerLocation;
      }
  }
  behind.setDirection(damagerLocation.getDirection());
  return behind;
  */

    private Location shiftEvade(Location victimLocation, Location damagerLocation) {
        // Based on the victim's location
        double constantY = victimLocation.getY() + 0.01d;
        Location tempLocation = victimLocation.clone();
        Vector vect = victimLocation.getDirection().normalize().multiply(-2.0);
        Location behind = victimLocation.add(vect);
        double d = 0.05;
        double inc = 0.05;
        double x2 = behind.getX(), y2 = behind.getY(), z2 = behind.getZ();
        // x1,y2,z1
        double x1 = tempLocation.getX(), y1 = tempLocation.getY(), z1 = tempLocation.getZ();
        // Possible points
        while (d < 1) {
            // x2,y2,z2

            double x = x1 + d * (x2 - x1);
            double y = constantY; // y1 + 0.05d * (y2 - y1); // this stays constant
            double z = z1 + d * (z2 - z1);

            Location testLocation = new Location(victimLocation.getWorld(), x, y, z);
            tempLocation = testLocation;
            d += inc;

            if (!testLocation.getBlock().isEmpty()) {
                break;
            }
            tempLocation = testLocation;
            if (testLocation.distanceSquared(behind) < 0.20) {
                break;
            }

        }
        return tempLocation.setDirection(victimLocation.getDirection());
    }

    @Override
    public void task() {
        long nextTime = time + 600L;
        double timeLeft = (nextTime - System.currentTimeMillis())/1000D;
        TitleSender.sendTitle(getPlayer(), TitleSender.simpleTime("Evade: ",
                Double.toString(MathUtil.round(timeLeft, 2)) + " s",timeLeft, 1D));
    }

    @Override
    public boolean cancel() {
        if (isEvading) {
            if (System.currentTimeMillis() - time <= 600L) {
                if (getPlayer().isBlocking()) {
                    return false;
                } else return true;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void cleanup() {
        getPlayer().sendMessage(String.format("%sSkill> %sYou failed evade.",
                ChatColor.BLUE, ChatColor.GRAY));
        SoundPlayer.sendSound(getPlayer(),"note.pling", 0.75F, 2);
        this.setLastUsed(System.currentTimeMillis());
        isEvading = false;
    }
}
