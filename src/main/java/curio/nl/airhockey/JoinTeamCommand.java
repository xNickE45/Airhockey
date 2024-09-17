package curio.nl.airhockey;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinTeamCommand implements CommandExecutor {
    private final GameArena gameArena;

    public JoinTeamCommand(GameArena gameArena) {
        this.gameArena = gameArena;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                String teamName = args[0].toUpperCase();
                try {
                    Team team = Team.valueOf(teamName);
                    gameArena.joinTeam(player, team);
                    return true;
                } catch (IllegalArgumentException e) {
                    player.sendMessage("Invalid team name. Use 'red' or 'blue'.");
                }
            } else {
                player.sendMessage("Please specify a team name.");
            }
        } else {
            sender.sendMessage("Only players can use this command.");
        }
        return false;
    }
}