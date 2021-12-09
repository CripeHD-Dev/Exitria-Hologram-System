package hologram.exitria.listener;

import hologram.exitria.HologramManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import systemapi.exitria.SystemUtils;
import systemapi.exitria.utils.Listener;

public class HologramListener extends Listener {

    private final HologramManager hologramManager;

    public HologramListener(JavaPlugin javaPlugin, HologramManager hologramManager) {
        super(javaPlugin);
        this.hologramManager = hologramManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        hologramManager.sendAll(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hologramManager.removeAllHolograms(e.getPlayer());
    }

    @EventHandler
    public void onChange(PlayerChangedWorldEvent event) {
        hologramManager.changeWorld(event.getPlayer(), event.getFrom());
    }

}
