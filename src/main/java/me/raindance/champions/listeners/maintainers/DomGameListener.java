package me.raindance.champions.listeners.maintainers;

import com.podcrash.api.mc.damage.Cause;
import com.podcrash.api.mc.events.DamageApplyEvent;
import com.podcrash.api.mc.events.game.*;
import com.podcrash.api.mc.game.Game;
import com.podcrash.api.mc.game.GameManager;
import com.podcrash.api.mc.game.TeamEnum;
import com.podcrash.api.mc.game.objects.IObjective;
import com.podcrash.api.mc.game.objects.ItemObjective;
import com.podcrash.api.mc.game.objects.objectives.CapturePoint;
import com.podcrash.api.mc.game.objects.objectives.Emerald;
import com.podcrash.api.mc.game.objects.objectives.Restock;
import com.podcrash.api.mc.game.resources.ItemObjectiveSpawner;
import com.podcrash.api.mc.game.resources.ScoreboardRepeater;
import com.podcrash.api.mc.game.scoreboard.GameScoreboard;
import com.podcrash.api.mc.listeners.ListenerBase;
import com.podcrash.api.mc.util.VectorUtil;
import com.podcrash.api.redis.Communicator;
import me.raindance.champions.Main;
import me.raindance.champions.game.DomGame;
import me.raindance.champions.game.map.DominateMap;
import me.raindance.champions.game.resource.CapturePointDetector;
import me.raindance.champions.game.resource.CapturePointScorer;
import me.raindance.champions.game.scoreboard.DomScoreboard;
import me.raindance.champions.kits.ChampionsPlayer;
import me.raindance.champions.kits.ChampionsPlayerManager;
import me.raindance.champions.kits.Skill;
import me.raindance.champions.kits.classes.Assassin;
import me.raindance.champions.kits.classes.Brute;
import me.raindance.champions.kits.classes.Mage;
import me.raindance.champions.kits.skilltypes.TogglePassive;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;

public class DomGameListener extends ListenerBase {
    public DomGameListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void regDamage(DamageApplyEvent event) {
        if(!(event.getAttacker() instanceof Player) && !(event.getVictim() instanceof Player)) return;
        ChampionsPlayer championsVictim = ChampionsPlayerManager.getInstance().getChampionsPlayer((Player) event.getVictim());
        ChampionsPlayer championsAttacker = ChampionsPlayerManager.getInstance().getChampionsPlayer((Player) event.getAttacker());

        event.setArmorValue(championsVictim.getArmorValue());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void mapLoad(GameMapLoadEvent event) {
        if(!(event.getMap() instanceof DominateMap) || !(event.getGame() instanceof DomGame)) return;
        DomGame game = (DomGame) event.getGame();
        DominateMap domMap = (DominateMap) event.getMap();
        World world = event.getWorld();
        domMap.setGameWorld(world);
        game.setCapturePoints(domMap.getCapturePoints());
        game.setEmeralds(domMap.getEmeralds());
        game.setRestocks(domMap.getRestocks());

        GameScoreboard gameScoreboard;
        if((gameScoreboard = game.getGameScoreboard()) instanceof DomScoreboard)
            ((DomScoreboard) gameScoreboard).setup(game.getCapturePoints());
    }

    @EventHandler
    public void onStart(GameStartEvent e) {
        Game game = e.getGame();
        game.broadcast(game.toString());
        Main.getInstance().getLogger().info("game is " + game);
        if (e.getGame().getPlayerCount() < 1) {
            Main.instance.getLogger().info(String.format("Can't start game %d, not enough players!", game.getId()));
        }
        String startingMsg = String.format("Game %d is starting up with map %s", e.getGame().getId(), e.getGame().getMapName());
        for(Player p : e.getGame().getBukkitPlayers()) p.sendMessage(startingMsg);

        game.sendColorTab(false);
        CapturePointDetector capture = new CapturePointDetector(game.getId());
        game.registerResources(
                new ScoreboardRepeater(game.getId()),
                new ItemObjectiveSpawner(game.getId()),
                capture,
                new CapturePointScorer(capture)
        );
        game.broadcast(e.getMessage());
    }


    @EventHandler
    public void onEnd(GameEndEvent e) {
        Communicator.publishLobby(Communicator.getCode() + " close");
        DomGame game1 = new DomGame(GameManager.getCurrentID(), Long.toString(System.currentTimeMillis()));
        GameManager.createGame(game1);
    }


    @EventHandler
    public void onGameDeath(GameDeathEvent e) {
        TeamEnum victimTeam = e.getGame().getTeamEnum(e.getWho());
        TeamEnum enemyTeam = null;
        Player victim = e.getWho();
        e.getGame().increment(victimTeam, 50);

        ChampionsPlayer victimPlayer;
        if((victimPlayer = ChampionsPlayerManager.getInstance().getChampionsPlayer(victim)) == null) return;
        victimPlayer.getSkills().forEach(skill -> {
            if(skill instanceof TogglePassive)
                if(((TogglePassive) skill).isToggled())
                    ((TogglePassive) skill).toggle();
        });
    }

    @EventHandler
    public void ressurect(GameResurrectEvent e) {
        ChampionsPlayerManager.getInstance().getChampionsPlayer(e.getWho()).respawn();
    }

    @EventHandler
    public void onCapture(GameCaptureEvent e){
        Game game = e.getGame();
        IObjective objective = e.getObjective();
        if(objective instanceof CapturePoint){
            String teamColor = ((CapturePoint) objective).getColor();
            TeamEnum team = TeamEnum.getByColor(teamColor);
            StringBuilder builder = new StringBuilder();
            builder.append(team.getChatColor());
            builder.append(ChatColor.BOLD);
            builder.append(team.getName());
            builder.append(" has captured ");
            builder.append(objective.getName());
            builder.append("!");
            e.getGame().broadcast(builder.toString());
            DomScoreboard scoreboard = (DomScoreboard) e.getGame().getGameScoreboard();
            scoreboard.updateCapturePoint(team, objective.getName());
            objective.spawnFirework();
        }else if(objective instanceof Emerald) {

        }else e.getWho().sendMessage(e.getMessage());
    }

    @EventHandler
    public void pickUp(GamePickUpEvent event) {
        ItemObjective itemObjective = event.getItem();
        Player player = event.getWho();
        Game game = event.getGame();
        TeamEnum team = game.getTeamEnum(player);
        if(itemObjective instanceof Emerald) {
            game.increment(team, 300);
            StringBuilder builder = new StringBuilder();
            //builder.append(team.getChatColor());
            builder.append(ChatColor.DARK_GREEN);
            builder.append(ChatColor.BOLD);
            builder.append(team.getName());
            //builder.append(ChatColor.GREEN);
            builder.append(" has gained 300 points!");
            game.broadcast(builder.toString());
        }else if(itemObjective instanceof Restock) {
            ChampionsPlayer cPlayer = ChampionsPlayerManager.getInstance().getChampionsPlayer(player);
            cPlayer.restockInventory();
            if(cPlayer instanceof Mage) {
                cPlayer.getEnergyBar().setEnergy(cPlayer.getEnergyBar().getMaxEnergy());
                player.sendMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "You restored energy!");
            }
            player.sendMessage(ChatColor.YELLOW + ChatColor.BOLD.toString() + "You recieved supplies!");
        }
        itemObjective.spawnFirework();
        game.getGameResources().forEach(resource -> {
            if(resource instanceof ItemObjectiveSpawner){
                ((ItemObjectiveSpawner) resource).setItemTime(itemObjective, System.currentTimeMillis());
            }
        });
    }

    @EventHandler
    public void death(GameDeathEvent e){
        List<Skill> skills = ChampionsPlayerManager.getInstance().getChampionsPlayer(e.getWho()).getSkills();
        for(Skill skill : skills) {
            if(!(skill instanceof TogglePassive)) continue;
            if (((TogglePassive) skill).isToggled())
                ((TogglePassive) skill).toggle();
        }
    }
}