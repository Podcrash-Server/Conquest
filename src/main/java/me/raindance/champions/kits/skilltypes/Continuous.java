package me.raindance.champions.kits.skilltypes;

import me.raindance.champions.Main;
import me.raindance.champions.kits.enums.ItemType;
import com.podcrash.api.mc.time.TimeHandler;
import com.podcrash.api.mc.time.resources.TimeResource;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;

public abstract class Continuous extends Instant implements TimeResource {
    private boolean useOnce = true;

    @Override
    public ItemType getItemType() {
        return ItemType.SWORD;
    }

    public Continuous() {
        super();
    }

    @Override
    protected void doSkill(PlayerEvent event, Action action) {
        if(rightClickCheck(action)){
            doContinuousSkill();
            useOnce = false;
            getPlayer().sendMessage(getUsedMessage());
        }
    }

    protected abstract void doContinuousSkill();

    protected void startContinuousAction(){
        TimeHandler.repeatedTime(1,0, this);
    }

    protected void asyncStart(){
        TimeHandler.repeatedTimeAsync(1,0, this);
    }

    protected void forceStop(){
        TimeHandler.unregister(this);
    }

    @Override
    public void cleanup() {
        TimeHandler.unregister(this);
        useOnce = true;
    }
}
