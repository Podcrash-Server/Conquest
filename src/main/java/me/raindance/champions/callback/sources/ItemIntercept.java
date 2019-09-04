package me.raindance.champions.callback.sources;

import me.raindance.champions.callback.CallbackAction;
import me.raindance.champions.util.EntityUtil;
import me.raindance.champions.world.BlockUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ItemIntercept extends CallbackAction<ItemIntercept> {
    protected LivingEntity entity;
    protected Player owner;
    protected Item item;
    private ItemIntercept(long delay, long ticks) {
        super(delay, ticks);
    }

    public ItemIntercept(Player owner, Item item) {
        this(0, 1);
        this.owner = owner;
        this.item = item;
    }

    @Override
    public boolean cancel() {
        World world = item.getWorld();
        if(!item.isValid() || EntityUtil.onGround(item)) return true;
        for(LivingEntity living : world.getLivingEntities()){
            if(living.getEntityId() != owner.getEntityId()){
                Location location = living.getLocation();
                Vector dir = item.getLocation().getDirection().clone().normalize();
                Location itemClone = item.getLocation().clone();
                Location locationClone = location.clone();
                itemClone.setY(0);
                locationClone.setY(0);
                if((location.getY() <= item.getLocation().getY() && item.getLocation().getY() < location.getY() + 2) && itemClone.distanceSquared(locationClone) <= 1.1) {
                    this.entity = living;
                    return true;
                }else if(!BlockUtil.isPassable(item.getLocation().add(dir.multiply(1.5D)).getBlock())){
                    this.entity = null;
                    return true;
                }
            }
        }
        return false;
    }

    public LivingEntity getIntercepted() {
        return entity;
    }
}
