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
            System.out.println("End command executed by player.");
            gameArena.clearArena();
            sender.sendMessage("Game ended!");
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }
}