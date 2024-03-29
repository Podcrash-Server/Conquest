package me.raindance.champions.kits.itemskill;

import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.callback.helpers.TrapSetter;
import com.podcrash.api.mc.callback.sources.AwaitTime;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.events.DeathApplyEvent;
import com.podcrash.api.mc.events.TrapPrimeEvent;
import com.podcrash.api.mc.events.TrapSnareEvent;
import com.podcrash.api.mc.game.GameManager;
import com.podcrash.api.mc.util.PacketUtil;
import me.raindance.champions.events.ApplyKitEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.*;
import java.util.function.Consumer;

public abstract class TrapItem implements IItem, Listener {
    private Map<Integer, String> itemOwners;
    private long delay;

    protected long despawnDelay;
    public TrapItem(long delay) {
        this.itemOwners = new HashMap<>();
        this.delay = delay;

        this.despawnDelay = 20L * 1000L;
    }

    protected abstract Item throwItem(Player player, Action action);
    protected abstract void primeTrap(Item item);
    protected abstract void snareTrap(Player owner, Player player, Item item);

    @Override
    public void useItem(Player player, Action action) {
        Item item = throwItem(player, action);
        if(item == null) return;
        itemOwners.put(item.getEntityId(), player.getName());
        TrapSetter.spawnTrap(item, delay);
    }

    @EventHandler
    public void trapPrime(TrapPrimeEvent e) {
        Item item = e.getItem();
        if(!itemOwners.containsKey(item.getEntityId())) return;
        primeTrap(item);
        AwaitTime time = new AwaitTime(despawnDelay);
        time.then(() -> {
            if(TrapSetter.destroyTrap(item)) {
                WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(
                    item.getLocation().clone().add(0, 1, 0).toVector(), EnumWrappers.Particle.EXPLOSION_NORMAL, 5, 0, 0, 0);
                    PacketUtil.syncSend(packet, item.getWorld().getPlayers());
            }
            itemOwners.remove(item.getEntityId());
        }).runAsync(5, 0);
    }

    @EventHandler
    public void trapSnare(TrapSnareEvent e) {
        String ownerName = itemOwners.get(e.getItem().getEntityId());
        if(ownerName == null) return;
        Player owner = Bukkit.getPlayer(ownerName);
        Item item = e.getItem();
        Player snared = e.getPlayer();
        if(GameManager.getGame() != null && GameManager.getGame().isRespawning(snared)) {
            e.setCancelled(true);
        }else {
            snareTrap(owner, snared, item);
            TrapSetter.destroyTrap(item);
            itemOwners.remove(item.getEntityId());
        }
    }

    @EventHandler
    public void death(DeathApplyEvent e) {
        consumeAllTrapItems(e.getPlayer(), TrapSetter::destroyTrap);
    }

    @EventHandler
    public void apply(ApplyKitEvent e) {
        Player player = e.getChampionsPlayer().getPlayer();
        consumeAllTrapItems(player, TrapSetter::destroyTrap);
    }

    public void consumeAllTrapItems(Player player, Consumer<Item> consumer) {
        World world = player.getWorld();
        Collection<Item> items = world.getEntitiesByClass(Item.class);
        List<Item> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : itemOwners.entrySet()) {
            int id = entry.getKey();
            String ownerName = entry.getValue();
            if (!ownerName.equalsIgnoreCase(player.getName())) continue;

            Item i = null;
            for (Item item : items) {
                if (item.getEntityId() == id) {
                    toRemove.add(item);
                    i = item;
                    break;
                }
            }
            if(i == null) break;
            consumer.accept(i);
            WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(
                    i.getLocation().clone().add(0, 1, 0).toVector(), EnumWrappers.Particle.EXPLOSION_NORMAL, 1, 0, 0, 0);
            PacketUtil.syncSend(packet, i.getWorld().getPlayers());
        }
        for(Item i : toRemove) {
            itemOwners.remove(i.getEntityId());
        }
    }
}
