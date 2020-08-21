package me.raindance.champions.kits.skills.sorcerer;

import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.callback.helpers.TrapSetter;
import com.podcrash.api.mc.callback.sources.CollideBeforeHitGround;
import com.podcrash.api.mc.damage.DamageApplier;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.effect.status.Status;
import com.podcrash.api.mc.effect.status.StatusApplier;
import com.podcrash.api.mc.events.ItemCollideEvent;
import com.podcrash.api.mc.events.TrapPrimeEvent;
import com.podcrash.api.mc.item.ItemManipulationManager;
import com.podcrash.api.mc.sound.SoundPlayer;
import com.podcrash.api.mc.time.TimeHandler;
import com.podcrash.api.mc.time.resources.TimeResource;
import com.podcrash.api.mc.util.PacketUtil;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.action.IConstruct;
import me.raindance.champions.kits.iskilltypes.action.ICooldown;
import me.raindance.champions.kits.iskilltypes.action.IEnergy;
import me.raindance.champions.kits.skilltypes.Instant;
import com.podcrash.api.mc.sound.SoundWrapper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SkillMetadata(id = 1009, skillType = SkillType.Sorcerer, invType = InvType.SHOVEL)
public class ThunderBomb extends Instant implements IEnergy, ICooldown, IConstruct {
    private WrapperPlayServerWorldParticles particles;
    private String NAME;
    private int energy = 55;
    private float distance = 16;
    private int damage = 6;

    private int currentItemID;

    // These are all blocks that we need to avoid at all costs to spare the player's ears!
    private Set<Material> avoid = new HashSet<>(Arrays.asList(Material.WATER, Material.STATIONARY_WATER, Material.LAVA, Material.STATIONARY_LAVA, Material.WEB));

    @Override
    public void afterConstruction() {
        this.NAME = getPlayer().getName()  + getName();
        this.particles = ParticleGenerator.createParticle(null, EnumWrappers.Particle.SNOW_SHOVEL, 1, 0, 0, 0);

    }

    @Override
    public float getCooldown() {
        return 6;
    }

    @Override
    public String getName() {
        return "Thunder Bomb";
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SHOVEL;
    }

    @Override
    public int getEnergyUsage() {
        return energy;
    }

    @Override
    protected void doSkill(PlayerEvent event, Action action) {
        if(!rightClickCheck(action) || onCooldown()) return;
        if(!hasEnergy()) {
            getPlayer().sendMessage(getNoEnergyMessage());
            return;
        }
        this.setLastUsed(System.currentTimeMillis());
        Location location = getPlayer().getEyeLocation();
        Vector vector = location.getDirection();
        vector.normalize().multiply(1.15D);
        useEnergy(energy);
        Item spawnItem = ItemManipulationManager.regular(Material.DIAMOND_BLOCK, location, vector);
        Item item = ItemManipulationManager.intercept(spawnItem, 1.1,(item1, entity, land) -> {
            if(entity == null) TrapSetter.spawnTrap(item1, 500);
            else collide(item1, land);
        });
        item.setCustomName("RITB");
        ItemMeta meta = item.getItemStack().getItemMeta();
        meta.setDisplayName(NAME + item.getEntityId());
        item.getItemStack().setItemMeta(meta);
        ParticleGenerator.generateEntity(item, particles, new SoundWrapper("random.fizz", 0.6F, 88));
        SoundPlayer.sendSound(item.getLocation(), "mob.silverfish.hit", 1F, 90);

        this.currentItemID = item.getEntityId();

        badLandingCheck(item);

        getPlayer().sendMessage(getUsedMessage());
    }

    @EventHandler
    public void trapPrime(TrapPrimeEvent event) {
        Item item = event.getItem();
        if(item.getEntityId() != currentItemID) return;
        collide(item, item.getLocation());
    }

    @EventHandler
    public void collideItem(ItemCollideEvent e) {
        if(e.isCancelled()) return;
        //identity check + owner of item check = cancel collision
        if((e.getCollisionVictim() == getPlayer() || isAlly(e.getCollisionVictim())) && e.getItem().getEntityId() == currentItemID)
            e.setCancelled(true);
    }


    private void collide(Item item, Location location) {
        for(Player player : getPlayers()) {
            if(player == getPlayer() && isAlly(player)) continue;
            Location playerLocation = player.getLocation();
            if(location.distanceSquared(playerLocation) > distance) continue;
            playerLocation.getWorld().strikeLightningEffect(playerLocation);
            StatusApplier.getOrNew(player).applyStatus(Status.SLOW, 4, 1);
            StatusApplier.getOrNew(player).applyStatus(Status.SHOCK, 4, 1);
            DamageApplier.damage(player, getPlayer(), damage, this, false);
        }

        WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(
                location.clone().add(0, 1, 0).toVector(), EnumWrappers.Particle.EXPLOSION_NORMAL, 5, 0, 0, 0);
        PacketUtil.syncSend(packet, getPlayers());
        TrapSetter.deleteTrap(item);
        item.remove();
    }

    private void badLandingCheck(Item item) {
        TimeHandler.repeatedTime(20, 0, new TimeResource() {
            Block curBlock = item.getLocation().getBlock();
            Block underBlock = item.getLocation().subtract(0, 1, 0).getBlock();
            @Override
            public void task() {
                curBlock = item.getLocation().getBlock();
                underBlock = item.getLocation().subtract(0, 1, 0).getBlock();
            }

            @Override
            public boolean cancel() {
                return avoid.contains(curBlock.getType()) || item.isDead()
                        || (underBlock.getType().equals(Material.LAVA) || underBlock.getType().equals(Material.STATIONARY_LAVA));
            }

            @Override
            public void cleanup() {
                collide(item, item.getLocation());
                TrapSetter.destroyTrap(item);
            }
        });
    }

}
