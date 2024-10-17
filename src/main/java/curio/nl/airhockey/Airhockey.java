package curio.nl.airhockey;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Airhockey extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        World world = Bukkit.getWorlds().get(0);
        if (world == null) {
            System.out.println("World not found!");
            return;
        }
        GameArena gameArena = new GameArena(world, this);
        this.getCommand("startgame").setExecutor(new StartCommand(gameArena));
        this.getCommand("endgame").setExecutor(new EndCommand(gameArena));
        this.getCommand("jointeam").setExecutor(new JoinTeamCommand(gameArena));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}