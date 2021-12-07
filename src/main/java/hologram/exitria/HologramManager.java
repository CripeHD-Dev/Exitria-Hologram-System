package hologram.exitria;

import hologram.exitria.hologram.HologramData;
import hologram.exitria.hologram.HologramGroup;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HologramManager {

    private final Hologram hologram;
    private File file;
    private YamlConfiguration cfg;

    private final Map<String, HologramGroup> holograms;

    public HologramManager(Hologram hologram) {
        this.hologram = hologram;
        holograms = new HashMap<>();
        reload();
    }

    public void reload() {
        disable();
        file = new File(hologram.getDataFolder() + "//holograms.yml");
        cfg = YamlConfiguration.loadConfiguration(file);
        int slot;
        holograms.clear();

        for (String key : cfg.getConfigurationSection("").getKeys(false)) {
            slot = 0;
            holograms.put(key.toLowerCase(), new HologramGroup(key, new Location(Bukkit.getWorld(cfg.getString(key + ".location.world")), cfg.getDouble(key + ".location.x"), cfg.getDouble(key + ".location.y"), cfg.getDouble(key + ".location.z"))));
            if (cfg.contains(key.toLowerCase() + ".icon"))
                getHologramGroup(key).setMaterial(Material.getMaterial(cfg.getString(key.toLowerCase() + ".icon")));

            while (cfg.contains(key.toLowerCase() + ".line-" + slot)) {
                holograms.get(key.toLowerCase()).getHolograms().put(slot, new HologramData(cfg.getString(key.toLowerCase() + ".line-" + slot)));
                slot++;
            }
        }
        for (Player players : Bukkit.getOnlinePlayers()) {
            for (HologramGroup hologramGroup : holograms.values()) {
                sendHologram(hologramGroup, players);
            }
        }
        hologram.getLogger().info(holograms.size() + " Hologramm[e] wurden geladen.");
    }

    public void createHologram(final String name, final String text, final Location location) {
        final HologramGroup hologramGroup = new HologramGroup(name, location);
        hologramGroup.addLine(new HologramData(text));
        holograms.put(name.toLowerCase(), hologramGroup);
        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase() + ".name", name);
                cfg.set(name.toLowerCase() + ".location.x", location.getX());
                cfg.set(name.toLowerCase() + ".location.y", location.getY());
                cfg.set(name.toLowerCase() + ".location.z", location.getZ());
                cfg.set(name.toLowerCase() + ".location.world", location.getWorld().getName());
                cfg.set(name.toLowerCase() + ".line-0", text);
                save();
            }
        });
        for (Player players : Bukkit.getOnlinePlayers())
            sendHologram(hologramGroup, players);
    }

    public void addLine(final String name, final String text) {
        getHologramGroup(name).addLine(new HologramData(text));
        for (Player players : Bukkit.getOnlinePlayers()) {
            removeHologram(getHologramGroup(name), players);
            sendHologram(getHologramGroup(name), players);
        }
        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase() + ".line-" + (getHologramGroup(name).getHolograms().size() - 1), text);
                save();
            }
        });
    }

    public void removeLine(final String name) {
        getHologramGroup(name).removeLine();

        // delete if emtpy
        if (getHologramGroup(name).getHolograms().isEmpty()) {
            delete(name);
        } else {
            for (Player players : Bukkit.getOnlinePlayers()) {
                removeHologram(getHologramGroup(name), players);
                sendHologram(getHologramGroup(name), players);
            }
            Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
                @Override
                public void run() {
                    cfg.set(name.toLowerCase() + ".line-" + getHologramGroup(name).getHolograms().size(), null);
                    save();
                }
            });
        }
    }

    public void setIcon(final String name, final Material material) {
        getHologramGroup(name).setMaterial(material);
        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase() + ".icon", material.name());
                save();
            }
        });
    }

    public void removeIcon(final String name) {
        getHologramGroup(name).removeIcon();
        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase() + ".icon" + getHologramGroup(name).getHolograms().size(), null);
                save();
            }
        });
    }

    public void delete(final String name) {
        getHologramGroup(name).removeIcon();
        for (Player player : Bukkit.getOnlinePlayers())
            removeHologram(getHologramGroup(name), player);
        holograms.remove(name.toLowerCase());
        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase(), null);
                save();
            }
        });
    }

    public void changeWorld(final Player player, final World oldWorld) {
        for (HologramGroup hologramGroup : holograms.values()) {
            if (oldWorld == hologramGroup.getLocation().getWorld())
                removeHologram(hologramGroup, player);
            else if (player.getWorld() == hologramGroup.getLocation().getWorld())
                sendHologram(hologramGroup, player);
        }
    }

    public void sendAll(final Player player) {
        for (HologramGroup hologramGroup : holograms.values())
            if (player.getWorld() == hologramGroup.getLocation().getWorld())
                sendHologram(hologramGroup, player);
    }

    public void sendHologram(final HologramGroup hologramGroup, final Player player) {
        final WorldServer worldServer = ((CraftWorld) hologramGroup.getLocation().getWorld()).getHandle();
        EntityArmorStand armorStand;
        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving;
        PacketPlayOutEntityMetadata entityMetadata;
        float height = 2f;
        hologramGroup.addPlayer(player);
        for (int i = 0; i < hologramGroup.getHolograms().size(); i++) {
            armorStand = new EntityArmorStand(worldServer,
                    hologramGroup.getLocation().clone().getX(),
                    hologramGroup.getLocation().clone().getY() - height,
                    hologramGroup.getLocation().clone().getZ());
            armorStand.setCustomName(new ChatMessage(getLine(hologramGroup.getHolograms().get(i), player)));
            armorStand.setCustomNameVisible(true);
            armorStand.setInvisible(true);
            armorStand.setNoGravity(true);
            height += 0.28;
            packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving(armorStand);
            entityMetadata = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), false);
            hologramGroup.getList(player).add(armorStand.getId());
            sendPacket(player, packetPlayOutSpawnEntityLiving);
            sendPacket(player, entityMetadata);
        }
    }

    public void removeHologram(final HologramGroup hologramGroup, final Player player) {
        PacketPlayOutEntityDestroy entityDestroy;
        for (int id : hologramGroup.getList(player)) {
            entityDestroy = new PacketPlayOutEntityDestroy(id);
            sendPacket(player, entityDestroy);
        }
        hologramGroup.removePlayer(player);
    }

    public void removeAllHolograms(final Player player) {
        for (HologramGroup hologramGroup : holograms.values())
            if (hologramGroup.containsPlayer(player))
                removeHologram(hologramGroup, player);
    }

    public void disable() {
        for (Player player : Bukkit.getOnlinePlayers())
            removeAllHolograms(player);
        for (HologramGroup hologramGroup : holograms.values())
            hologramGroup.removeIcon();
    }

    private void sendPacket(final Player player, final Packet<?> packet) {
        ((CraftPlayer) player).getHandle().b.sendPacket(packet);
    }

    public HologramGroup getHologramGroup(final String name) {
        return holograms.get(name.toLowerCase());
    }

    public boolean existHologram(final String name) {
        return holograms.containsKey(name.toLowerCase());
    }

    private String getLine(final HologramData hologramData, final Player player) {
        return ChatColor.translateAlternateColorCodes('&', hologramData.getDisplay().replace("%player%", player.getName()));
    }

    public Map<String, HologramGroup> getHolograms() {
        return holograms;
    }

    private void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
