package net.dungeonhd.jerryGunPlugin.command;

import net.dungeonhd.jerryGunPlugin.JerryGunPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.dungeonhd.jerryGunPlugin.util.Text.legacy;

/**
 * /giveitem [player] - gives the Jerry-chine Gun to the sender, or to the
 * specified player if the sender has permission to give it to others.
 */
public class GiveItemCommand implements CommandExecutor, TabCompleter {

    private final JerryGunPlugin plugin;

    public GiveItemCommand(JerryGunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length >= 1) {
            if (!sender.hasPermission("jerrychinegun.giveitem.others")) {
                sender.sendMessage(legacy("&cYou don't have permission to give this item to other players."));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(legacy("&cPlayer not found: &f" + args[0]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(legacy("&cConsole has to specify a player: /giveitem <player>"));
                return true;
            }
            target = (Player) sender;
        }

        ItemStack item = plugin.getGunItem().create();
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            target.getWorld().dropItemNaturally(target.getLocation(), item);
            target.sendMessage(legacy("&eYour inventory was full, the Jerry-chine Gun was dropped at your feet."));
        }

        target.sendMessage(legacy("&aYou received the &dJerry-chine Gun&a!"));
        if (sender != target) {
            sender.sendMessage(legacy("&aGave a &dJerry-chine Gun &ato &f" + target.getName() + "&a."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("jerrychinegun.giveitem.others")) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
