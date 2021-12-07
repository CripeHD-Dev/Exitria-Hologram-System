package hologram.exitria;

import hologram.exitria.commands.HologramCommand;
import hologram.exitria.listener.HologramListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Hologram extends JavaPlugin {

    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        super.onEnable();
        hologramManager = new HologramManager(this);

        new HologramCommand(this, "hologram", hologramManager);

        new HologramListener(this, hologramManager);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hologramManager.disable();
    }
}