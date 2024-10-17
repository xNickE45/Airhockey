package curio.nl.airhockey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            Player player = (Player) sender; ;
            if (gameArena.isGameActive()) {
                player.sendMessage(ChatColor.DARK_RED + "A game is already happening.");
                return true;
            }
            gameArena.setArenaLocation(player.getLocation());
            gameArena.createArena();
            for (Player onlinePlayers : Bukkit.getOnlinePlayers()){
                gameArena.sendTeamSelectionMessage(onlinePlayers);
            }
            return true;
        }
        sender.sendMessage("Only players can use this command.");
        return false;
    }
}