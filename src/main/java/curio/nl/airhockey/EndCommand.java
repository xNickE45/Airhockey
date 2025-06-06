package curio.nl.airhockey;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndCommand implements CommandExecutor {
    private final GameArena gameArena;

    public EndCommand(GameArena gameArena) {
        this.gameArena = gameArena;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("airhockey.endgame")) {
                player.sendMessage("You don't have permission to use this command.");
                return false;
            }
            gameArena.endGame(null);
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }
}