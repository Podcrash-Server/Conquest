package me.raindance.champions.kits.skills.vanguard;

import com.abstractpackets.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.podcrash.api.mc.damage.DamageApplier;
import me.raindance.champions.Main;
import com.podcrash.api.mc.effect.particle.ParticleGenerator;
import com.podcrash.api.mc.item.ItemManipulationManager;
import me.raindance.champions.kits.annotation.SkillMetadata;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.ItemType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.skilltypes.ChargeUp;
import com.podcrash.api.mc.sound.SoundPlayer;
import com.podcrash.api.mc.sound.SoundWrapper;
import com.podcrash.api.mc.util.VectorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Arrays;

@SkillMetadata(skillType = SkillType.Vanguard, invType = InvType.SWORD)
public class ChainedHook extends ChargeUp {
    private float vect;

    @Override
    public String getName() {
        return "Chained Hook";
    }

    @Override
    public float getRate() {
        return 0;
    }

    public ChainedHook() {
        super();
        this.vect = 1.1F + 0.3F * 4;
    }

    @Override
    public void release() {
        Vector vector = getPlayer().getLocation().getDirection();
        double charge = getCharge();
        Vector itemVector = vector.clone().normalize().multiply(this.vect/1.25F);
        Location oldLocation = getPlayer().getLocation();
        Item itemItem = ItemManipulationManager.intercept(getPlayer(), Material.TRIPWIRE_HOOK, getPlayer().getEyeLocation(),itemVector.setY(itemVector.getY() + 0.2).multiply(0.5F + 0.5F * charge),
                ((item, entity) -> {
                    item.remove();
                    if(entity == null) return;
                    double amnt = 0.20F + 0.8F * charge;
                    Location away = entity.getLocation();
                    Vector newVect = VectorUtil.fromAtoB(away, oldLocation);
                    newVect.normalize().multiply(this.vect).multiply(amnt);
                    double addY = newVect.getY() + 0.4F  + 0.4F * amnt;
                    if(addY > 1.35F) addY = 1.35F;
                    entity.setVelocity(newVect.setY(addY));
                    SoundPlayer.sendSound(getPlayer(), "random.successful_hit", 0.75F, 50);
                    if(entity instanceof Player) DamageApplier.damage(entity, getPlayer(), charge * 3, this, false);
                }));
        itemItem.setPickupDelay(1000);
        ItemMeta meta = itemItem.getItemStack().getItemMeta();
        meta.setDisplayName(Long.toString(System.currentTimeMillis()));
        itemItem.getItemStack().setItemMeta(meta);

        SoundWrapper sound = new SoundWrapper("fire.ignite", 0.8F, 70);
        WrapperPlayServerWorldParticles packet = ParticleGenerator.createParticle(EnumWrappers.Particle.CRIT, 2);

        Bukkit.getScheduler().runTaskLater(Main.instance, () -> ParticleGenerator.generateEntity(itemItem, packet, sound), 1L);
    }

    @Override
    public float getCooldown() {
        return 13;
    }

}
