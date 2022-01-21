package hologram.exitria;

import hologram.exitria.hologram.HologramData;
import hologram.exitria.hologram.HologramGroup;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import systemapi.exitria.utils.builder.ItemBuilder;

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
                if (hologramGroup.getLocation().getWorld() == players.getWorld()) sendHologram(hologramGroup, players);
            }
        }
        hologram.getLogger().info(holograms.size() + " Hologramm[e] wurden geladen.");
    }

    public void createHologram(String name, String text, Location location) {
        HologramGroup hologramGroup = new HologramGroup(name, location);
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

    public void addLine(String name, String text) {
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

    public void removeLine(String name) {
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

    public void setIcon(String name, Material material) {
        HologramGroup group = getHologramGroup(name);

        for (Player players : group.getPlayersIcon().keySet())
            removeIcon(players, group);
        group.setMaterial(material);
        WorldServer worldServer = ((CraftWorld) group.getLocation().getWorld()).getHandle();
        if (group.getMaterial() != null) {
            for (Player players : group.getPlayersLines().keySet()) {
                ItemStack itemStack = CraftItemStack.asNMSCopy(new ItemBuilder(group.getMaterial()).build());
                EntityItem item = new EntityItem(worldServer, group.getLocation().clone().getX(), group.getLocation().clone().getY() + 0.5, group.getLocation().clone().getZ(), itemStack);
                item.a(group.getLocation().clone().getX(), group.getLocation().clone().getY() + 0.5, group.getLocation().clone().getZ(), 0, 0);
                item.e(true); // No Gravity

                PacketPlayOutSpawnEntity entity = new PacketPlayOutSpawnEntity(item);
                PacketPlayOutEntityMetadata data = new PacketPlayOutEntityMetadata(item.ae(), item.ai(), true);
                group.getPlayersIcon().put(players, item.ae());
                sendPacket(players, entity);
                sendPacket(players, data);
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase() + ".icon", material.name());
                save();
            }
        });
    }

    public void removeIcon(String name) {
        HologramGroup group = getHologramGroup(name);
        for (Player players : group.getPlayersIcon().keySet())
            removeIcon(players, group);

        Bukkit.getScheduler().runTaskAsynchronously(hologram, new Runnable() {
            @Override
            public void run() {
                cfg.set(name.toLowerCase() + ".icon" + getHologramGroup(name).getHolograms().size(), null);
                save();
            }
        });
    }

    public void removeIcon(Player player, HologramGroup hologramGroup) {
        if (!hologramGroup.getPlayersIcon().containsKey(player)) return;
        PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(hologramGroup.getPlayersIcon().get(player));
        sendPacket(player, destroy);
    }

    public void delete(String name) {
        HologramGroup group = getHologramGroup(name);
        for (Player players : group.getPlayersIcon().keySet())
            removeIcon(players, group);
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

    public void changeWorld(Player player, World oldWorld) {
        for (HologramGroup hologramGroup : holograms.values()) {
            if (oldWorld == hologramGroup.getLocation().getWorld()) removeHologram(hologramGroup, player);
            else if (player.getWorld() == hologramGroup.getLocation().getWorld()) sendHologram(hologramGroup, player);
        }
    }

    public void sendAll(Player player) {
        for (HologramGroup hologramGroup : holograms.values())
            if (player.getWorld() == hologramGroup.getLocation().getWorld()) sendHologram(hologramGroup, player);
    }

    public void sendHologram(HologramGroup hologramGroup, Player player) {
        WorldServer worldServer = ((CraftWorld) hologramGroup.getLocation().getWorld()).getHandle();
        EntityArmorStand armorStand;
        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving;
        PacketPlayOutEntityMetadata entityMetadata;
        float height = 2f;
        hologramGroup.addPlayer(player);
        for (int i = 0; i < hologramGroup.getHolograms().size(); i++) {
            armorStand = new EntityArmorStand(worldServer, hologramGroup.getLocation().clone().getX(), hologramGroup.getLocation().clone().getY() - height, hologramGroup.getLocation().clone().getZ());
            armorStand.a(new ChatMessage(getLine(hologramGroup.getHolograms().get(i), player))); // Customname
            armorStand.n(true); // CustomnameVisible
            armorStand.j(true); // Invisible
            armorStand.e(true); // No Gravity
            height += 0.28;
            packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving(armorStand);
            entityMetadata = new PacketPlayOutEntityMetadata(armorStand.ae(), armorStand.ai(), false);
            hologramGroup.getList(player).add(armorStand.ae());
            sendPacket(player, packetPlayOutSpawnEntityLiving);
            sendPacket(player, entityMetadata);
        }

        // Spawn Icon
        if (hologramGroup.getMaterial() != null) {

            ItemStack itemStack = CraftItemStack.asNMSCopy(new ItemBuilder(hologramGroup.getMaterial()).build());
            EntityItem item = new EntityItem(worldServer, hologramGroup.getLocation().clone().getX(), hologramGroup.getLocation().clone().getY() + 0.5, hologramGroup.getLocation().clone().getZ(), itemStack);
            item.a(hologramGroup.getLocation().clone().getX(), hologramGroup.getLocation().clone().getY() + 0.5, hologramGroup.getLocation().clone().getZ(), 0, 0);
            item.e(true); // No Gravity

            PacketPlayOutSpawnEntity entity = new PacketPlayOutSpawnEntity(item);
            PacketPlayOutEntityMetadata data = new PacketPlayOutEntityMetadata(item.ae(), item.ai(), true);
            hologramGroup.getPlayersIcon().put(player, item.ae());

            sendPacket(player, entity);
            sendPacket(player, data);
        }
    }

    public void removeHologram(HologramGroup hologramGroup, Player player) {
        removeIcon(player, hologramGroup);
        PacketPlayOutEntityDestroy entityDestroy;
        for (int id : hologramGroup.getList(player)) {
            entityDestroy = new PacketPlayOutEntityDestroy(id);
            sendPacket(player, entityDestroy);
        }
        hologramGroup.removePlayer(player);
    }

    public void removeAllHolograms(Player player) {
        for (HologramGroup hologramGroup : holograms.values())
            if (hologramGroup.containsPlayer(player)) removeHologram(hologramGroup, player);
    }

    public void disable() {
        for (Player player : Bukkit.getOnlinePlayers())
            removeAllHolograms(player);
        for (HologramGroup hologramGroup : holograms.values())
            hologramGroup.setMaterial(null);
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().b.a(packet);
    }

    public HologramGroup getHologramGroup(String name) {
        return holograms.get(name.toLowerCase());
    }

    public boolean existHologram(String name) {
        return holograms.containsKey(name.toLowerCase());
    }

    private String getLine(HologramData hologramData, Player player) {
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
