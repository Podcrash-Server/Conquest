package me.raindance.champions.game.resource;

import com.podcrash.api.mc.events.game.GameCaptureEvent;
import com.podcrash.api.mc.game.TeamEnum;
import com.podcrash.api.mc.game.objects.objectives.CapturePoint;
import com.podcrash.api.mc.game.resources.GameResource;
import me.raindance.champions.game.scoreboard.DomScoreboard;
import me.raindance.champions.game.DomGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.*;

public final class CapturePointDetector extends GameResource {
    private final CapturePoint[] capturePoints;
    private final boolean[] playersCurrentlyIn;
    private boolean bold;
    private List<Player> firstPlayerToCapture;
    private DomScoreboard scoreboard;

    private TeamEnum red;
    private TeamEnum blue;
    /**
     * > 0 = red
     * < 0 = blue
     * = 0 = white
     */
    private final Map<Integer, Integer> teamToColor = new HashMap<>();
    /**
     * [capturePoint index][x,y,z coordinate][first or second bound]
     */
    private final double[][][] bounds;
    private final String[] players;

    public CapturePointDetector(int gameID) {
        super(gameID, 8, 100);
        this.firstPlayerToCapture = new ArrayList<>(Arrays.asList(null, null, null, null, null));
        this.capturePoints = ((DomGame) getGame()).getCapturePoints().toArray(new CapturePoint[((DomGame) getGame()).getCapturePoints().size()]);
        this.bounds = new double[5][3][2];
        this.playersCurrentlyIn = new boolean[5];
        for (int i = 0; i < this.capturePoints.length; i++) {
            playersCurrentlyIn[i] = false;
            Location[] cbounds = this.capturePoints[i].getBounds();
            teamToColor.put(i, 0);
            for (int b = 0; b < cbounds.length; b++) {
                this.bounds[i][0][b] = cbounds[b].getX();
                this.bounds[i][1][b] = cbounds[b].getY();
                this.bounds[i][2][b] = cbounds[b].getZ();
            }
        }
        List<String> names = new ArrayList<>();
        for(Player player : getGame().getBukkitPlayers()) names.add(player.getName());
        names.removeIf((name) -> getGame().isSpectating(name));
        this.players = names.toArray(new String[names.size()]);
        this.scoreboard = ((DomScoreboard) getGame().getGameScoreboard());
        red = getGame().getTeam(0).getTeamEnum();
        blue = getGame().getTeam(1).getTeamEnum();

        this.bold = false;
    }

    public CapturePoint[] getCapturePoints() {
        return capturePoints;
    }

    /**
     * If the player is within bound of a specified capture point.
     * @param i the index of the capture point
     * @param player the player
     * @return whether the player is within the bound
     */
    private boolean isInBound(int i, Player player) {
        if(player == null) return false;
        Location location = player.getLocation();
        double x = location.getBlockX();
        double y = location.getBlockY();
        double z = location.getBlockZ();
        double[][] cpoint = this.bounds[i];
        if (cpoint[0][1] <= x && x <= cpoint[0][0]){
            if (cpoint[1][0] <= y && y <= cpoint[1][1]) {
                if (cpoint[2][1] <= z && z <= cpoint[2][0]) {
                    if(!player.getAllowFlight()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @see {@link CapturePointDetector#task()}
     * If a player is currently in a capture point (using {@link CapturePointDetector#isInBound(int, Player)})
     * Then add them to the team color, as well as update if a player is in it.
     * @param i the capture point index
     */
    private void findPlayerInCap(int i) {
        boolean foundPlayer = false;
        for(int p = 0; p < players.length; p++){
            boolean a = isInBound(i, Bukkit.getPlayer(players[p]));
            if(a) {
                TeamEnum team = getGame().getTeamEnum(Bukkit.getPlayer(players[p]));
                teamToColor.put(i, teamToColor.get(i) + team.getIntData());
                firstPlayerToCapture.set(i, Bukkit.getPlayer(players[p]));
                foundPlayer = true;
            }
        }
        playersCurrentlyIn[i] = foundPlayer;
    }

    /**
     * //TODO: clean this up to work with colors
     * Capture the point if there are players in it.
     * Positive = red
     * Negative = blue
     * if there is nobody on the capture point, just neutralize it {@link CapturePoint#restoreCapture()}
     * else capture the point {@link CapturePoint#capture(String)}
     * If the point becomes captured, then call the GameCaptureEvent {@link GameCaptureEvent)
     * @param i the capture point index
     */
    private void capture(int i) {
        CapturePoint capturePoint = capturePoints[i];
        int times = teamToColor.get(i);
        TeamEnum team = null;
        if(times > 0){
            scoreboard.updateCurrentlyInCPoint(red, capturePoint, bold);
            team = capturePoint.capture(red.getName(), times);
        }else if(times < 0){
            scoreboard.updateCurrentlyInCPoint(blue, capturePoint, bold);
            team = capturePoint.capture(blue.getName(), times * -1);
        }else {
            if(capturePoint.getTeamColor() == TeamEnum.WHITE && capturePoint.isFull()) return;
            if(!playersCurrentlyIn[i]) {
                scoreboard.updateCurrentlyInCPoint(null, capturePoint, false);
                capturePoint.restoreCapture();
            }
        }
        teamToColor.put(i, 0);
        if(team != null) Bukkit.getPluginManager().callEvent(new GameCaptureEvent(getGame(), firstPlayerToCapture.get(i), capturePoint));
    }
    @Override
    public void task() {
        bold = !bold;
        for(int i = 0; i < capturePoints.length; i++) {
            findPlayerInCap(i);
            capture(i);
        }
    }


    @Override
    public void cleanup() {
        clear();
    }
}
