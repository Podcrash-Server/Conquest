package me.raindance.champions.kits.skills.RangerSkills;

import me.raindance.champions.damage.Cause;
import me.raindance.champions.events.DamageApplyEvent;
import me.raindance.champions.hologram.Hologram;
import me.raindance.champions.kits.ChampionsPlayer;
import me.raindance.champions.kits.ChampionsPlayerManager;
import me.raindance.champions.kits.Skill;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.skilltypes.Passive;
import me.raindance.champions.time.resources.TimeResource;
import me.raindance.champions.util.MathUtil;
import me.raindance.champions.util.TitleSender;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class LongshotRework extends Passive implements TimeResource {
    private List<Arrow> arrows = new ArrayList<>();
    private HashMap<String, LongshotDebuff> affected = new HashMap<>();
    private float damageMultiplier;
    private float distanceMultiplier;
    public LongshotRework(Player player, int level) {
        super(player, "Longshot Rework", level, SkillType.Ranger, InvType.PASSIVEB);
        this.damageMultiplier = 1 + 0.05F * level;
        this.distanceMultiplier = 5.5F - 0.5F * level;
        if(player != null) this.run(1, 0);
        setDesc(Arrays.asList(
                "You are a skilled tracker: ",
                "hitting an enemy with an arrow from ",
                "at least %%distance%% blocks away will ",
                "reveal important information about them. "
        ));
        addDescArg("distance", () ->  distanceMultiplier * 3);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @EventHandler(
            priority = EventPriority.LOW
    )
    protected void shotArrow(EntityShootBowEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() == getPlayer() && event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrows.add(arrow);
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    protected void shotPlayer(DamageApplyEvent event) {
        if(!(event.getVictim() instanceof Player)) return;
        if(affected.containsKey(event.getVictim().getName())){
            event.setDamage(event.getDamage() * damageMultiplier);


            event.setModified(true);
            event.addSkillCause(this);
        }
        if(event.getArrow() != null) {
            Arrow arr = event.getArrow();
            if (arrows.contains(arr) && event.getCause() == Cause.PROJECTILE && event.getAttacker() == getPlayer()) {
                Player victim = (Player) event.getVictim();
                Player damager = (Player) event.getAttacker();
                Location vLocation = victim.getLocation();
                Location dLocation = damager.getLocation();
                event.setDamage(event.getDamage() - 1);
                event.setModified(true);
                double distance = vLocation.distance(dLocation);
                int time = (int) ((distance/distanceMultiplier) + 0.5);
                if(time < 3) return;
                getPlayer().sendMessage("Longshot> You tracked " + victim.getName() + " for " + time + " seconds.");
                event.addSkillCause(this);
                long nextTime = System.currentTimeMillis() + (time * 1000L);
                LongshotDebuff debuff = affected.get(victim.getName());
                if(debuff != null) {
                    debuff.nextTime = nextTime;
                }else affected.put(victim.getName(), new LongshotDebuff(victim, nextTime));
                arrows.removeIf(arrow -> !arrow.isValid());
            }
        }
    }


    private class LongshotDebuff {
        private final Vector up = new Vector(0, 3, 0);
        private final Player player;
        private long nextTime;
        private final ChampionsPlayer cplayer;
        private Hologram hologram;
        private boolean valid = true;
        protected LongshotDebuff(Player player, long nextTime) {
            this.player = player;
            this.nextTime = nextTime;
            this.cplayer = ChampionsPlayerManager.getInstance().getChampionsPlayer(player);
            Skill current = cplayer.getCurrentSkillInHand();
            String cooldownBar = (current == null) ? "" : TitleSender.coolDownString(current);
            this.hologram = new Hologram(player.getLocation().add(up), true, String.valueOf(player.getHealth()), cooldownBar);
            render();
        }

        public boolean isValid(){
            return System.currentTimeMillis() < nextTime && valid;
        }

        public void render() {
            this.hologram.setLocation(player.getLocation().clone().add(up));
            final char heart = '\u2764';
            this.hologram.editLine(0, String.valueOf(MathUtil.round(player.getHealth(), 1)) + ChatColor.RED + heart);

            Skill current = cplayer.getCurrentSkillInHand();
            String secondLine;
            if(current == null) {
                secondLine = " ";
            }else {
                if(!current.onCooldown()) secondLine = ChatColor.GREEN + current.getName();
                else secondLine = TitleSender.coolDownString(current);
            }
            this.hologram.editLine(1, secondLine);
            this.hologram.update();

        }
    }

    @Override
    public void task() {
        if(affected.size() == 0) return;
        Iterator<Map.Entry<String, LongshotDebuff>> debuffs = affected.entrySet().iterator();
        while(debuffs.hasNext()) {
            Map.Entry<String, LongshotDebuff> debuffEntry = debuffs.next();
            debuffEntry.getValue().render();
            if(!debuffEntry.getValue().isValid()) {
                debuffEntry.getValue().hologram.destroy();
                debuffs.remove();
            }
        }

    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void cleanup() {

    }


}
