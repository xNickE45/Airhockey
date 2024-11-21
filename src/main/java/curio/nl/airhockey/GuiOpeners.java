package curio.nl.airhockey;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuiOpeners implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

//        if (label.equalsIgnoreCase("PuckGUI")) {
//            PuckGUI puckGUI = new PuckGUI(player);
//            puckGUI.openGUI(player);
//            return true;
//        }
        return false;
    }
}