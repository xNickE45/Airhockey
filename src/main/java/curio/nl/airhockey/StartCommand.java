package curio.nl.airhockey;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {
    private final GameArena gameArena;

    public StartCommand(GameArena gameArena) {
        this.gameArena = gameArena;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            System.out.println("Start command executed by player.");
            gameArena.setArenaLocation(player.getLocation());
            gameArena.createArena();
            sender.sendMessage("Game started!");
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }
}