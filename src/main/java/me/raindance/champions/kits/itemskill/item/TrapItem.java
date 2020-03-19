package me.raindance.champions.kits.itemskill.item;

import com.podcrash.api.mc.callback.helpers.TrapSetter;
import com.podcrash.api.mc.events.TrapPrimeEvent;
import com.podcrash.api.mc.events.TrapSnareEvent;
import me.raindance.champions.kits.itemskill.IItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.Map;

public abstract class TrapItem implements IItem {
    private Map<Integer, String> itemOwners;
    private long delay;
    public TrapItem(long delay) {
        this.itemOwners = new HashMap<>();
        this.delay = delay;
    }

    abstract Item throwItem(Player player, Action action);
    abstract void primeTrap(Item item);
    abstract void snareTrap(Player owner, Player player, Item item);

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
    }

    @EventHandler
    public void trapSnare(TrapSnareEvent e) {
        String ownerName = itemOwners.get(e.getItem().getEntityId());
        if(ownerName == null) return;
        Player owner = Bukkit.getPlayer(ownerName);
        Item item = e.getItem();
        Player snared = e.getPlayer();
        snareTrap(owner, snared, item);
    }
}
