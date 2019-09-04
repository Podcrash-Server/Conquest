package me.raindance.champions.kits;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.raindance.champions.Main;
import me.raindance.champions.effect.status.Status;
import me.raindance.champions.effect.status.StatusApplier;
import me.raindance.champions.events.ApplyKitEvent;
import me.raindance.champions.inventory.BookFormatter;
import me.raindance.champions.inventory.ChampionsItem;
import me.raindance.champions.inventory.InventoryData;
import me.raindance.champions.kits.classes.Knight;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.SkillType;
import me.raindance.champions.kits.iskilltypes.IConstruct;
import me.raindance.champions.kits.iskilltypes.IInjector;
import me.raindance.champions.kits.iskilltypes.IPassiveTimer;
import me.raindance.champions.time.TimeHandler;
import me.raindance.champions.time.resources.TimeResource;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ChampionsPlayerManager {
    private static volatile ChampionsPlayerManager cpm;
    private JavaPlugin plugin = Main.getInstance();
    private HashMap<String, ChampionsPlayer> championsPlayers = new HashMap<>();
    private Map<ChampionsPlayer, List<PacketListener>> injectors = new HashMap<>();
    private Set<Integer> assassins = new HashSet<>();

    private void boost(Skill skill, ChampionsPlayer championsPlayer) {
        boolean boosted = false;
        if(skill.getInvType().equals(InvType.SWORD)) {
            if(championsPlayer.hotBarContains(Material.GOLD_SWORD)) boosted = true;
        }else if(skill.getInvType().equals(InvType.AXE)) {
            if(championsPlayer.hotBarContains(Material.GOLD_AXE)) boosted = true;
        }
        skill.setBoosted(boosted);
    }
    private void register(Skill skill) {
        if(skill.isValid()) {
            plugin.getServer().getPluginManager().registerEvents(skill, plugin);
            if (skill instanceof IPassiveTimer) ((IPassiveTimer) skill).start();
            if (skill instanceof IConstruct) ((IConstruct) skill).doConstruct();
            if (skill instanceof IInjector) addPacketListener(getChampionsPlayer(skill.getPlayer()), ((IInjector) skill).inject());
        }
    }

    public void addChampionsPlayer(ChampionsPlayer cplayer) {
        if (cplayer == null) return;
        ChampionsPlayer oldPlayer = getChampionsPlayer(cplayer.getPlayer());
        removeChampionsPlayer(oldPlayer);

        championsPlayers.putIfAbsent(cplayer.getPlayer().getName(), cplayer);

        ChampionsPlayer cp = getChampionsPlayer(cplayer.getPlayer());
        StatusApplier.getOrNew(cp.getPlayer()).removeStatus(Status.values());
        ApplyKitEvent apply = new ApplyKitEvent(cp);
        Bukkit.getPluginManager().callEvent(apply);

        cp.equip();
        if(oldPlayer != null && oldPlayer.getSpawnLocation() != null)
            cp.setSpawnLocation(oldPlayer.getSpawnLocation());
        cp.heal(20);
        cp.getPlayer().setFoodLevel(20);
        cp.effects();
        for(Skill skill : cp.getSkills()) {
            boost(skill, cp);
            register(skill);
        }

        if(!apply.isKeepInventory())
            cp.restockInventory();
        cp.getPlayer().sendMessage(cp.skillsRead());
    }
    public void removeChampionsPlayer(ChampionsPlayer cplayer) {
        Main.getInstance().log.info(cplayer + "");
        if (cplayer == null ||
                !championsPlayers.containsKey(cplayer.getPlayer().getName())) return;
        List<Skill> skills = cplayer.getSkills();
        Iterator<Skill> skillIterator = skills.iterator();
        Main.getInstance().getLogger().info(String.format("%s Unregistering.", cplayer.getPlayer().getName()));
        while (skillIterator.hasNext()) {
            final Skill skill = skillIterator.next();
            HandlerList.unregisterAll(skill);
            Main.getInstance().getLogger().info(String.format("%s unregistered from %s", skill.getName(), skill.getPlayer()));
            skill.setValid(false);
            if (skill instanceof TimeResource) TimeHandler.unregister((TimeResource) skill);
        }
        clearPacketListeners(cplayer);
        championsPlayers.remove(cplayer.getPlayer().getName());
    }
    public void removeChampionsPlayer(Player player) {
        ChampionsPlayer championsPlayer = championsPlayers.getOrDefault(player.getName(), null);
        if(championsPlayer != null)
            removeChampionsPlayer(championsPlayer);
    }

    public ChampionsPlayer getChampionsPlayer(Player player) {
        return championsPlayers.getOrDefault(player.getName(), null);
    }

    private void addPacketListener(ChampionsPlayer cPlayer, PacketListener listener) {
        List<PacketListener> packetListeners = injectors.getOrDefault(cPlayer, new ArrayList<>());
        packetListeners.add(listener);
    }
    private void clearPacketListeners(ChampionsPlayer cPlayer) {
        List<PacketListener> packetListeners = injectors.getOrDefault(cPlayer, new ArrayList<>());
        if(packetListeners.size() == 0) return;
        ProtocolManager manager = Main.instance.getProtocolManager();
        for(PacketListener listener : packetListeners) {
            manager.removePacketListener(listener);
        }
    }
    public List<String> readSkills(String jsonStr) {
        List<String> skillWord = new ArrayList<>();
        JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();

        JsonObject skillsJson = json.getAsJsonObject("skills");
        for (String idKey : skillsJson.keySet()) {
            Skill skill = InventoryData.getSkillById(Integer.parseInt(idKey));
            skillWord.add(skill.getName() + ": " + skillsJson.get(idKey).getAsInt());
        }
        return skillWord;
    }
    public JsonObject deserialize(String jsonStr) {
        return new JsonParser().parse(jsonStr).getAsJsonObject();
    }
    public ChampionsPlayer deserialize(Player owner, String jsonStr) {
        JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
        SkillType skillType = SkillType.getByName(json.get("skilltype").getAsString());

        JsonObject skillsJson = json.getAsJsonObject("skills");
        List<Skill> skills = new ArrayList<>();

        for (String idKey : skillsJson.keySet()) {
            Skill skill = InventoryData.getSkillById(Integer.parseInt(idKey));
            BookFormatter book = InventoryData.getSkillFormatter(skill);
            Skill newSkill = book.newInstance(owner, skillsJson.get(idKey).getAsInt());
            skills.add(newSkill);
        }

        JsonObject itemsJson = json.getAsJsonObject("items");
        ItemStack[] items = new ItemStack[9];
        for(String slotKey : itemsJson.keySet()) {
            int itemID = itemsJson.get(slotKey).getAsInt();
            if(itemID == -1) continue;
            ChampionsItem championsItem = ChampionsItem.getBy(itemID, skillType);
            items[Integer.parseInt(slotKey)] = championsItem.toItemStack();
        }

        ChampionsPlayer championsPlayer = newObj(owner, skills, skillType);
        championsPlayer.setDefaultHotbar(items);
        return championsPlayer; //oh god
    }

    private static final Map<SkillType, Constructor> constructors = new HashMap<>(); //reflection is expensive
    private Constructor getConstructor(SkillType skillType) {
        if(skillType == SkillType.Global) throw new IllegalArgumentException("Global is not allowed");
        if(!constructors.containsKey(skillType)) {
            try {
                Class<ChampionsPlayer> clazz = (Class<ChampionsPlayer>) Class.forName("me.raindance.champions.kits.classes." + skillType.getName());
                Constructor cons = clazz.getDeclaredConstructor(Player.class, List.class);
                constructors.put(skillType, cons);
            }catch (ClassNotFoundException|NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return constructors.get(skillType);
    }

    private ChampionsPlayer newObj(Player owner, List<Skill> skills, SkillType skillType) {
        try {
            return (ChampionsPlayer) getConstructor(skillType).newInstance(owner, skills);
        }catch (InstantiationException|IllegalAccessException|InvocationTargetException e){
            e.printStackTrace();
        }
        throw new IllegalArgumentException("something went wrong ya");
    }

    public void clear(){
        Iterator iterator = championsPlayers.keySet().iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public ChampionsPlayer defaultBuild(Player player) {
        Knight knight = new Knight(player, new ArrayList<>());
        return knight;
    }

    public HashMap getChampionsPlayers() {
        return championsPlayers;
    }

    public static ChampionsPlayerManager getInstance() {
        if (cpm == null) {
            synchronized (ChampionsPlayerManager.class) {
                if (cpm == null) {
                    cpm = new ChampionsPlayerManager();
                }
            }

        }
        return cpm;
    }


    private ChampionsPlayerManager() {

    }
}
