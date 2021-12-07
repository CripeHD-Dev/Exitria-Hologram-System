package hologram.exitria.commands;

import hologram.exitria.hologram.HologramGroup;
import hologram.exitria.HologramManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import systemapi.exitria.SystemUtils;
import systemapi.exitria.command.PlayerCommand;

import java.util.List;

public class HologramCommand extends PlayerCommand {

    private final HologramManager hologramManager;

    public HologramCommand(JavaPlugin javaPlugin, String command, HologramManager hologramManager) {
        super(javaPlugin, command);
        this.hologramManager = hologramManager;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("hologram.hologram")) {
            sendNoPerm(player);
            return;
        }

        switch (args.length) {
            case 1:
                switch (args[0].toLowerCase()) {
                    case "list":
                        list(player);
                        break;
                    case "reload":
                        reload(player);
                        break;
                    default:
                        sendUsage(player);
                        break;
                }
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "removeicon":
                        removeicon(player, args);
                        break;
                    case "delete":
                        deleteHologram(player, args);
                        break;
                    case "removeline":
                        removeline(player, args);
                        break;
                    default:
                        sendUsage(player);
                        break;
                }
                break;
            case 3:
                switch (args[0].toLowerCase()) {
                    case "seticon":
                        seticon(player, args);
                        break;
                    case "create":
                        createHologram(player, args);
                        break;
                    case "addline":
                        addline(player, args);
                        break;
                    default:
                        sendUsage(player);
                        break;
                }
                break;
            default:
                if (args.length > 3) {
                    switch (args[0].toLowerCase()) {
                        case "create":
                            createHologram(player, args);
                            break;
                        case "addline":
                            addline(player, args);
                            break;
                        default:
                            sendUsage(player);
                            break;
                    }
                    break;
                } else sendUsage(player);
        }
    }

    @Override
    public List<String> tab(CommandSender player, String[] args, List<String> list) {
        if (!player.hasPermission("hologram.hologram")) {
            return list;
        }

        if (args.length == 1) {
            list.add("create");
            list.add("delete");
            list.add("seticon");
            list.add("removeicon");
            list.add("addline");
            list.add("removeline");
            list.add("list");
            list.add("reload");
        } else if (args.length == 2) {
            list.addAll(hologramManager.getHolograms().keySet());
        }
        return list;
    }

    private void sendUsage(Player player) {
        sendUsage(player, "/Hologram create <Name> <Text>");
        sendUsage(player, "/Hologram delete <Name>");
        sendUsage(player, "/Hologram seticon <Name> <Material>");
        sendUsage(player, "/Hologram removeicon <Name>");
        sendUsage(player, "/Hologram addline <Name> <Text>");
        sendUsage(player, "/Hologram removeline <Name>");
        sendUsage(player, "/Hologram list");
        sendUsage(player, "/Hologram reload");
    }

    private void createHologram(Player player, String[] args) {
        if (hologramManager.existHologram(args[1])) {
            sendError(player, "Dieses Hologram existiert bereits.");
            return;
        }
        String message = SystemUtils.arrayToString(args, 2);

        hologramManager.createHologram(args[1], message, player.getEyeLocation());
        sendMessage(player, "Das Hologram §a" + args[1] + getSystemAPI().getDefaultColor() + " wurde erstellt.");
    }

    private void deleteHologram(Player player, String[] args) {
        if (!hologramManager.existHologram(args[1])) {
            sendError(player, "Dieses Hologram existiert nicht.");
            return;
        }
        hologramManager.delete(args[1]);
        sendMessage(player, "Das Hologram wurde gelöscht.");
    }

    private void seticon(Player player, String[] args) {
        if (!hologramManager.existHologram(args[1])) {
            sendError(player, "Dieses Hologram existiert nicht.");
            return;
        }
        Material material = Material.getMaterial(args[2].toUpperCase());
        if (material == null) {
            sendError(player, "Dieses Material existiert nicht.");
            return;
        }
        hologramManager.setIcon(args[1], material);
        sendMessage(player, "Das Icon wurde gesetzt.");
    }

    private void removeicon(Player player, String[] args) {
        if (!hologramManager.existHologram(args[1])) {
            sendError(player, "Dieses Hologram existiert nicht.");
            return;
        }
        if (hologramManager.getHologramGroup(args[1]).getItem() == null) {
            sendError(player, "Dieses Hologram besitzt kein Icon.");
            return;
        }
        hologramManager.removeIcon(args[1]);
        sendMessage(player, "Das Icon wurde entfernt.");
    }

    private void addline(Player player, String[] args) {
        if (!hologramManager.existHologram(args[1])) {
            sendError(player, "Dieses Hologram existiert nicht.");
            return;
        }
        String message = SystemUtils.arrayToString(args, 2);
        hologramManager.addLine(args[1], message);
        sendMessage(player, "Eine neue Zeile wurde hinzugefügt.");
    }

    private void removeline(Player player, String[] args) {
        if (!hologramManager.existHologram(args[1])) {
            sendError(player, "Dieses Hologram existiert nicht.");
            return;
        }
        hologramManager.removeLine(args[1]);
        sendMessage(player, "Die letzte Zeile wurde gelöscht.");
    }

    private void list(Player player) {
        if (hologramManager.getHolograms().isEmpty()) {
            sendError(player, "Es gibt derzeit keine Hologramme.");
            return;
        }
        sendMessage(player, "Liste aller Hologramme:");
        for (HologramGroup hologramGroup : hologramManager.getHolograms().values()) {
            sendMessage(player, "§8- §a" + hologramGroup.getName());
        }
    }

    private void reload(Player player) {
        hologramManager.reload();
        sendMessage(player, "Das Hologram-System wurde neu geladen.");
    }

}
