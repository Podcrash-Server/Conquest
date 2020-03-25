package me.raindance.champions.kits;
import com.podcrash.api.mc.time.TimeHandler;
import com.podcrash.api.mc.time.resources.TimeResource;
import com.podcrash.api.mc.util.ExpUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;


public class EnergyBar implements TimeResource {
    private double energy;
    private double MAX_ENERGY;
    private String ownerName;
    private long lastTimeUsed;
    private boolean cancel = false;
    private boolean enabled = true;
    private double naturalRegenRate;

    public EnergyBar(Player p1, double eMax) {
        ownerName = p1.getName();
        MAX_ENERGY = eMax;
        incrementEnergy(MAX_ENERGY);
        this.naturalRegenRate = 0.5D;
        TimeHandler.repeatedTimeAsync(1,0, this);
    }

    // this is called whenever the player switches kits, and essentially cleans things up by removing the xp bar and canceling the mana regen
    public void stop() {
        setExp(0);
        cancel = true;
    }

    // getters and setters
    public void setEnergy(double energy) {
        if(energy > MAX_ENERGY) energy = MAX_ENERGY;

        float xp = (float) (energy / MAX_ENERGY);
        if(xp >= 1F) xp = .99999999F;
        setExp(xp);
        this.energy = energy;
        lastTimeUsed = System.currentTimeMillis();
    }

    public void incrementEnergy(double value) {
        // Add the value to current energy; make sure it is between MAX and zero
        this.energy += value;
        energy = Math.min(energy, MAX_ENERGY);
        energy = Math.max(energy, 0);

        // Calculate the ratio of energy to max, then setting the XP bar to reflect that ratio.
        float xp = (float) (energy / MAX_ENERGY);
        if(xp >= 1F) xp = .99999999F;
        setExp(xp);

        // Tell the system that we just used energy.
        lastTimeUsed = System.currentTimeMillis();
    }

    private void setExp(float xp) {
        ExpUtil.updateExp(getPlayer(), xp);
    }
    public void setMaxEnergy(double MAX_ENERGY) {
        this.MAX_ENERGY = MAX_ENERGY;
        incrementEnergy(MAX_ENERGY);
    }

    public double getEnergy()
    {
        return energy;
    }
    public double getMaxEnergy() {
        return MAX_ENERGY;
    }

    public void toggleRegen(boolean bool) {
        enabled = bool;
    }

    public double getNaturalRegenRate() {
        return naturalRegenRate;
    }

    public void setNaturalRegenRate(double naturalRegenRate) {
        this.naturalRegenRate = naturalRegenRate;
    }

    // timeHandler methods
    public void task() {
        if(System.currentTimeMillis() - lastTimeUsed >= 50 && energy <= MAX_ENERGY && enabled)
            incrementEnergy(naturalRegenRate);
    }

    public boolean cancel() {
        return cancel;
    }

    public void cleanup(){}
    
    private Player getPlayer() {
        return Bukkit.getPlayer(ownerName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnergyBar)) return false;

        EnergyBar energyBar = (EnergyBar) o;

        return Objects.equals(ownerName, energyBar.ownerName);
    }

    @Override
    public int hashCode() {
        return ownerName != null ? ownerName.hashCode() : 0;
    }
}
