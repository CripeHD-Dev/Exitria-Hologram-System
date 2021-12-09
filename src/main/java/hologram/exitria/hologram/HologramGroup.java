package hologram.exitria.hologram;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramGroup {

    private final String name;
    private final Location location;
    private final Map<Integer, HologramData> holograms;
    private Material material;
    private final Map<Player, List<Integer>> playersLines;
    private final Map<Player, Integer> playersIcon;

    public HologramGroup(String name, Location location) {
        this.name = name;
        this.location = location;
        holograms = new HashMap<>();
        playersLines = new HashMap<>();
        playersIcon = new HashMap<>();
        material = null;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void addLine(final HologramData hologramData) {
        holograms.put(holograms.size(), hologramData);
    }

    public void removeLine() {
        holograms.remove(holograms.size() - 1);
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public boolean containsPlayer(final Player player) {
        return playersLines.containsKey(player);
    }

    public void addPlayer(final Player player) {
        playersLines.put(player, new ArrayList<>());
    }

    public void removePlayer(final Player player) {
        playersLines.remove(player);
    }

    public List<Integer> getList(final Player player) {
        return playersLines.get(player);
    }

    public Material getMaterial() {
        return material;
    }

    public Map<Integer, HologramData> getHolograms() {
        return holograms;
    }

    public Map<Player, List<Integer>> getPlayersLines() {
        return playersLines;
    }

    public Map<Player, Integer> getPlayersIcon() {
        return playersIcon;
    }
}
