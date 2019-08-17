package me.raindance.champions.effect.status.custom;

import me.raindance.champions.effect.status.Status;
import me.raindance.champions.effect.status.StatusApplier;
import me.raindance.champions.time.resources.TimeResource;
import org.bukkit.entity.Player;

/*
    Base class for
 */
public abstract class CustomStatus implements TimeResource {
    private Status status;
    private StatusApplier applier;
    private Player player;

    protected StatusApplier getApplier() {
        return applier;
    }

    protected Player getPlayer() {
        return player;
    }

    public String getName() {
        return status.getName();
    }

    public Status getStatus() {
        return status;
    }

    CustomStatus(Player player, Status status) {
        if (status.isVanilla()) throw new IllegalArgumentException("Status cannot be vanilla");
        this.player = player;
        this.status = status;
        this.applier = StatusApplier.getOrNew(player);
    }

    protected abstract void doWhileAffected();

    protected abstract boolean isInflicted();

    protected abstract void removeEffect();

    @Override
    public void task() {
        doWhileAffected();
    }

    @Override
    public boolean cancel() {
        if (isInflicted()) {
            return !(applier.getRemainingDuration(status) > 0);
        } else return true;
    }

    @Override
    public void cleanup() {
        removeEffect();
        if(status != Status.INEPTITUDE) player.sendMessage(String.format("You have been cleared of %s", status.getName()));
    }

    @Override
    public String toString(){
        return String.format("%s{%s:%.2f}", this.getClass().getSimpleName(), player.getName(), applier.getRemainingDuration(status));
    }

}
