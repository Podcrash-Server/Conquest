package me.raindance.champions.kits;


import com.google.gson.JsonObject;
import me.raindance.champions.effect.status.Status;
import me.raindance.champions.effect.status.StatusApplier;
import me.raindance.champions.game.Game;
import me.raindance.champions.game.GameManager;
import me.raindance.champions.inventory.ChampionsItem;
import me.raindance.champions.inventory.InventoryData;
import me.raindance.champions.kits.classes.Assassin;
import me.raindance.champions.kits.classes.Mage;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.sound.SoundWrapper;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.ItemArmor;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class ChampionsPlayer implements Listener {
    private Player player;
    private ItemStack[] defaultHotbar;
    private double fallDamage = 0;
    private SoundWrapper sound; // sound when hit
    private EnergyBar ebar = null;
    private Location spawnLocation = null;
    private JsonObject jsonObject;
    protected List<Skill> skills = null;
    protected Material[] armor;

    public ChampionsPlayer(Player player) {
        this.player = player;
        this.spawnLocation = player.getWorld().getSpawnLocation(); // so that stuff doesn't crash
        getDefaultHotbar();
    }

    public boolean equip(){
        if(armor[0] == null) return false;
        ItemStack[] armors = new ItemStack[4];
        for(int i = 0; i < armors.length; i++){
            net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(new ItemStack(armor[i]));
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("Unbreakable", true);
            nmsStack.setTag(tag);

            armors[i] = new ItemStack(CraftItemStack.asBukkitCopy(nmsStack));
        }
        player.getEquipment().setArmorContents(armors);
        return true;
    }
    public void resetCooldowns() {
        for(Skill skill : skills) {
            skill.setLastUsed(0);
        }

    }

    public abstract String getName();
    public abstract SkillType getType();

    public boolean isInGame() {
        return GameManager.hasPlayer(this.player);
    }
    public Game getGame() {
        return GameManager.getGame(player);
    }
    public String getTeam() {
        if (isInGame()) return GameManager.getGame(this.player).getTeamColor(player);
        else return null;
    }

    /**
     * Check if the player is allied with the player.
     * Both players must be in a game.
     * @param player the player to check
     * @return true/false
     */
    public boolean isAlly(Player player) {
        return isInGame() && getGame().getTeamColor(player).equalsIgnoreCase(getTeam());
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public void respawn(){//TODO: Respawn with the hotbar.
        player.setFallDistance(0);
        player.teleport(this.spawnLocation);
        this.restockInventory();
        this.equip();
        this.resetCooldowns();
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFallDistance(0);
        for(Player player : getGame().getPlayers()){
            if(player != getPlayer()) player.showPlayer(getPlayer());
        }
        if (this instanceof Assassin)
            StatusApplier.getOrNew(player).applyStatus(Status.SPEED, Integer.MAX_VALUE, 1, true, true);
        else if (this instanceof Mage)
            getEnergyBar().setEnergy(getEnergyBar().getMaxEnergy() * (3D/4D));
        //StatusApplier.getOrNew(player).removeStatus(Status.INEPTITUDE);
    }


    public void heal(double health){
        Player player = getPlayer();
        double current = player.getHealth();
        double expected = current + health;
        if(expected >= player.getMaxHealth()){
            player.setHealth(player.getMaxHealth());
        }else player.setHealth(expected);
    }

    public Player getPlayer() {
        return player;
    }
    public CraftPlayer getCraftPlayer() {
        return (CraftPlayer) player;
    }
    public EntityPlayer getEntityCraftPlayer() {
        return this.getCraftPlayer().getHandle();
    }

    public ItemStack[] getArmor() {
        return this.player.getEquipment().getArmorContents();
    }
    public int getArmorValue() {
        //Check getEntityCraftPlayer().bq(); might be the exact same
        net.minecraft.server.v1_8_R3.ItemStack[] itemStack = getEntityCraftPlayer().inventory.armor;
        int i = 0;
        for (net.minecraft.server.v1_8_R3.ItemStack item : itemStack) {
            if (item != null) {
                if (item.getItem() instanceof ItemArmor) {
                    ItemArmor itemArmor = (ItemArmor) item.getItem();
                    i += itemArmor.c;
                }
            }
        }
        return i;
    }
    public Inventory getInventory() {
        return this.player.getInventory();
    }

    public ItemStack[] getDefaultHotbar() {
        if(this.defaultHotbar == null) {
            this.defaultHotbar = new ItemStack[] {
                    new ItemStack(Material.IRON_SWORD),
                    new ItemStack(Material.IRON_AXE),
                    new ItemStack(Material.MUSHROOM_SOUP),
                    new ItemStack(Material.MUSHROOM_SOUP),
                    new ItemStack(Material.MUSHROOM_SOUP),
                    new ItemStack(Material.MUSHROOM_SOUP),
                    new ItemStack(Material.MUSHROOM_SOUP),
                    new ItemStack(Material.MUSHROOM_SOUP),
                    new ItemStack(Material.MUSHROOM_SOUP)
            };
        }
        return this.defaultHotbar;
    }
    public void setDefaultHotbar(ItemStack[] items) {
        this.defaultHotbar = items;
    }
    public void setDefaultHotbar() {
        Inventory inventory = player.getInventory();
        ItemStack[] hotbar = new ItemStack[9];
        for(int i = 0; i < 9; i++) {
            ItemStack item;
            if((item = inventory.getItem(i)) != null) hotbar[i] = item.clone();
        }
        setDefaultHotbar(hotbar);
    }
    public ItemStack[] getHotBar() {
        ItemStack[] hotbar = new ItemStack[9];
        for (int i = 0; i <= 8; i++) {
            hotbar[i] = this.getInventory().getItem(i);
        }
        return hotbar;
    }
    public void restockInventory() {
        for (int i = 0; i < getDefaultHotbar().length; i++) {
            ItemStack item = this.defaultHotbar[i];
            if(item != null) this.getInventory().setItem(i, item.clone());
            else this.getInventory().setItem(i, null);
        }
    }

    public double getFallDamage() {
        return fallDamage;
    }
    public void setFallDamage(double fallDamage) {
        this.fallDamage = fallDamage;
    }

    public void setUsesEnergy(boolean usesEnergy){
        setUsesEnergy(usesEnergy, 180);
    }
    public void setUsesEnergy(boolean usesEnergy, double maxEnergy){
        if(usesEnergy){
            ebar = new EnergyBar(player, maxEnergy);
        } else {
            ebar.stop();
            ebar = null;
        }
    }
    public EnergyBar getEnergyBar(){
        return ebar;
    }

    public SoundWrapper getSound() {
        return sound;
    }
    public void setSound(SoundWrapper sound) {
        this.sound = sound;
    }

    public boolean isCloaked() {
        return StatusApplier.getOrNew(this.player).isCloaked();
    }
    public boolean isMarked() {
        return StatusApplier.getOrNew(this.player).isMarked();
    }
    public boolean isSilenced() {
        return StatusApplier.getOrNew(this.player).isSilenced();
    }
    public boolean isShocked() {
        return StatusApplier.getOrNew(this.player).isShocked();
    }

    public List<Skill> getSkills() {
        return skills;
    }
    public Skill getCurrentSkillInHand() {
        final Material material = player.getItemInHand().getType();
        for(Skill skill : skills) {
            if(skill.getItype() == null) continue;
            if(material.name().contains(skill.getItype().getName())) return skill;
        }
        return null;
    }

    public JsonObject serialize() {
        if(jsonObject != null) return jsonObject;
        JsonObject championsObject = new JsonObject();

        championsObject.addProperty("skilltype", this.getType().getName().toLowerCase());

        JsonObject skillsSerial = new JsonObject();

        for(Skill skill : skills) {
            int id = InventoryData.getSkillId(skill.getName());
            skillsSerial.addProperty(Integer.toString(id), skill.getLevel());
        }

        JsonObject itemsSerial = new JsonObject();
        for(int i = 0; i < defaultHotbar.length; i++) {
            ItemStack item = defaultHotbar[i];
            if(item == null || item.getType() == Material.AIR) continue;
            ChampionsItem championsItem = ChampionsItem.getByName(item.getItemMeta().getDisplayName());
            int slotID = (championsItem == null) ? -1 : championsItem.getSlotID();
            itemsSerial.addProperty(Integer.toString(i), slotID);
        }

        championsObject.add("skills", skillsSerial);
        championsObject.add("items", itemsSerial);
        this.jsonObject = championsObject;
        return championsObject;
    }
}

