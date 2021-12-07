package hologram.exitria.hologram;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import systemapi.exitria.utils.builder.ItemBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramGroup {

    private final String name;
    private Location location;
    private final Map<Integer, HologramData> holograms;
    private Item item;
    private final Map<String, List<Integer>> players;

    public HologramGroup(String name, Location location) {
        this.name = name;
        this.location = location;
        holograms = new HashMap<>();
        players = new HashMap<>();
        item = null;
    }

    public void setMaterial(Material material) {
        removeIcon();
        item = location.getWorld().dropItem(location.clone().add(0, 0.5, 0), new ItemBuilder(material).setDisplayname("§6HOLOGRAM§5Item").build());
        item.setCustomName("&6HOLOGRAM&5Item");
        item.setVelocity(new Vector(0, 0, 0));
        item.setGravity(false);
    }

    public void removeIcon() {
        if (item != null)
            item.remove();
        item = null;
    }

    public void addLine(final HologramData hologramData) {
        holograms.put(holograms.size(), hologramData);
    }

    public void setLine(final int line, final String text) {
        holograms.get(line).setDisplay(text);
    }

    public void removeLine() {
        holograms.remove(holograms.size() - 1);
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public boolean containsPlayer(final Player player) {
        return players.containsKey(player.getUniqueId().toString());
    }

    public void addPlayer(final Player player) {
        players.put(player.getUniqueId().toString(), new ArrayList<>());
    }

    public void removePlayer(final Player player) {
        players.remove(player.getUniqueId().toString());
    }

    public List<Integer> getList(final Player player) {
        return players.get(player.getUniqueId().toString());
    }

    public Item getItem() {
        return item;
    }

    public Map<Integer, HologramData> getHolograms() {
        return holograms;
    }
}
