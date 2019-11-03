package me.raindance.champions.listeners;

import com.podcrash.api.db.DataTableType;
import com.podcrash.api.db.PlayerPermissionsTable;
import com.podcrash.api.db.TableOrganizer;
import com.podcrash.api.mc.listeners.ListenerBase;
import com.podcrash.api.permissions.Perm;
import me.raindance.champions.game.map.MapManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class MapListener extends ListenerBase {
    public MapListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTouch(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        PlayerPermissionsTable table = TableOrganizer.getTable(DataTableType.PERMISSIONS);
        if (table.hasRole(e.getPlayer().getUniqueId(), Perm.BUILD)) { //Switch out for permissions
            Player p = e.getPlayer();
            MapManager mapper = MapManager.getInstance();
            Block block = e.getClickedBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                if (mapper.registerObjective(sign)) p.sendMessage("Success");
                else p.sendMessage("Failure");
            } else if (block.getType().equals(Material.EMERALD_BLOCK)) {
                if (mapper.registerEmerald(block)) p.sendMessage("Success");
                else p.sendMessage("Failure");
            } else if (block.getType().equals(Material.GOLD_BLOCK)) {
                if (mapper.registerRestock(block)) p.sendMessage("Success");
                else p.sendMessage("Failure");
            } else if (block.getType().equals(Material.WOOL)) {
                Wool wool = (Wool) block.getState().getData();
                if (mapper.registerSpawn(p, block, wool)) p.sendMessage("Success");
                else p.sendMessage("Failure to set a spawn");
            }
        }

    }
}
