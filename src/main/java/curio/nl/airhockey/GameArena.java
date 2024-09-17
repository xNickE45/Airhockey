package curio.nl.airhockey;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class GameArena implements Listener {
    private final World world;
    private final JavaPlugin plugin;
    private Location corner1;
    private Location corner2;
    private Location goal1;
    private Location goal2;
    private BukkitTask particleTask;
    private final Map<Location, Material> originalBlocks = new HashMap<>();
    private final Map<Player, Team> playerTeams = new HashMap<>();
    private boolean gameActive = false;

    public GameArena(World world, JavaPlugin plugin) {
        this.world = world;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setArenaLocation(Location playerLocation) {
        double x = playerLocation.getX();
        double y = playerLocation.getY();
        double z = playerLocation.getZ();
        this.corner1 = new Location(world, x, y, z);
        this.corner2 = new Location(world, x + 37, y + 6, z + 37); // Adjust arena size
        this.goal1 = new Location(world, x + 18.5, y - 1, z + 2); // Adjust goal to be in the middle
        this.goal2 = new Location(world, x + 18.5, y - 1, z + 35); // Adjust goal to be in the middle
    }

    public void displayParticles() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (double x = corner1.getX() - 1; x <= corner2.getX() + 1; x += 0.5) {
                    for (double y = corner1.getY(); y <= corner2.getY(); y += 0.5) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, x, y, corner1.getZ() - 1), 1);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, x, y, corner2.getZ() + 1), 1);
                    }
                }
                for (double z = corner1.getZ() - 1; z <= corner2.getZ() + 1; z += 0.5) {
                    for (double y = corner1.getY(); y <= corner2.getY(); y += 0.5) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, corner1.getX() - 1, y, z), 1);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, corner2.getX() + 1, y, z), 1);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Schedule to run every second (20 ticks)
    }

    public void stopParticles() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    public void createGoals() {
        System.out.println("Creating goals...");
        // Create a goal shape with a depth of 4 blocks
        for (int i = -2; i <= 2; i++) { // Make the goals 5 blocks wide
            for (int j = 0; j <= 3; j++) { // Make the goals 4 blocks deep
                if (j == 3 && (i == -2 || i == 2)) continue; // Skip corners to create a cup shape
                world.getBlockAt(goal1.clone().add(i, 0, -j)).setType(Material.GOLD_BLOCK);
                world.getBlockAt(goal2.clone().add(i, 0, j)).setType(Material.GOLD_BLOCK);
            }
        }
        // Create the top of the goal
        for (int i = -2; i <= 2; i++) {
            if (i == -2 || i == 2) continue; // Skip corners to create a cup shape
            world.getBlockAt(goal1.clone().add(i, 1, -3)).setType(Material.GOLD_BLOCK);
            world.getBlockAt(goal2.clone().add(i, 1, 3)).setType(Material.GOLD_BLOCK);
        }
        // Add blocks to the right and left sides of the goals to create a top
        for (int j = 0; j <= 3; j++) {
            world.getBlockAt(goal1.clone().add(-2, 1, -j)).setType(Material.GOLD_BLOCK);
            world.getBlockAt(goal1.clone().add(2, 1, -j)).setType(Material.GOLD_BLOCK);
            world.getBlockAt(goal2.clone().add(-2, 1, j)).setType(Material.GOLD_BLOCK);
            world.getBlockAt(goal2.clone().add(2, 1, j)).setType(Material.GOLD_BLOCK);
        }
    }

    public void flattenField() {
        System.out.println("Flattening field...");
        for (double x = corner1.getX(); x <= corner2.getX(); x++) {
            for (double z = corner1.getZ(); z <= corner2.getZ(); z++) {
                Location groundLoc = new Location(world, x, corner1.getY() - 1, z);
                originalBlocks.put(groundLoc, world.getBlockAt(groundLoc).getType());
                world.getBlockAt(groundLoc).setType(Material.GRASS_BLOCK);
                for (double y = corner1.getY(); y <= corner2.getY(); y++) {
                    Location airLoc = new Location(world, x, y, z);
                    originalBlocks.put(airLoc, world.getBlockAt(airLoc).getType());
                    world.getBlockAt(airLoc).setType(Material.AIR);
                }
            }
        }
        // Add border around the arena
        for (double x = corner1.getX() - 1; x <= corner2.getX() + 1; x++) {
            for (double z = corner1.getZ() - 1; z <= corner2.getZ() + 1; z++) {
                if (x == corner1.getX() - 1 || x == corner2.getX() + 1 || z == corner1.getZ() - 1 || z == corner2.getZ() + 1) {
                    Location borderLoc = new Location(world, x, corner1.getY() - 1, z);
                    originalBlocks.put(borderLoc, world.getBlockAt(borderLoc).getType());
                    world.getBlockAt(borderLoc).setType(Material.STONE);
                }
            }
        }
    }

    public void createArena() {
        flattenField();
        displayParticles();
        createGoals();
        gameActive = true;
    }

    public void clearArena() {
        System.out.println("Clearing arena...");
        stopParticles();
        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            world.getBlockAt(entry.getKey()).setType(entry.getValue());
        }
        for (int i = -2; i <= 2; i++) { // Clear the goals
            for (int j = 0; j <= 3; j++) {
                world.getBlockAt(goal1.clone().add(i, 0, -j)).setType(Material.AIR);
                world.getBlockAt(goal2.clone().add(i, 0, j)).setType(Material.AIR);
            }
        }
        for (int i = -2; i <= 2; i++) { // Clear the top of the goals
            if (i == -2 || i == 2) continue; // Skip corners to create a cup shape
            world.getBlockAt(goal1.clone().add(i, 1, -3)).setType(Material.AIR);
            world.getBlockAt(goal2.clone().add(i, 1, 3)).setType(Material.AIR);
        }
        for (int j = 0; j <= 3; j++) { // Clear the right and left sides of the goals
            world.getBlockAt(goal1.clone().add(-2, 1, -j)).setType(Material.AIR);
            world.getBlockAt(goal1.clone().add(2, 1, -j)).setType(Material.AIR);
            world.getBlockAt(goal2.clone().add(-2, 1, j)).setType(Material.AIR);
            world.getBlockAt(goal2.clone().add(2, 1, j)).setType(Material.AIR);
        }
        for (double x = corner1.getX(); x <= corner2.getX(); x++) { // Clear the top of the arena
            for (double z = corner1.getZ(); z <= corner2.getZ(); z++) {
                for (double y = corner1.getY(); y <= corner2.getY(); y++) {
                    Location airLoc = new Location(world, x, y, z);
                    world.getBlockAt(airLoc).setType(Material.AIR);
                }
            }
        }
        originalBlocks.clear();
    }

    public void endGame() {
        clearArena();
        for (Player player : playerTeams.keySet()) {
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
        }
        playerTeams.clear();
        gameActive = false;
    }

    public void sendTeamSelectionMessage(Player player) {
        TextComponent redTeam = new TextComponent("Join Red Team");
        redTeam.setColor(ChatColor.RED);
        redTeam.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jointeam red"));

        TextComponent blueTeam = new TextComponent("Join Blue Team");
        blueTeam.setColor(ChatColor.BLUE);
        blueTeam.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jointeam blue"));

        player.spigot().sendMessage(new ComponentBuilder("Select your team: ").append(redTeam).append(" ").append(blueTeam).create());
    }

    public void joinTeam(Player player, Team team) {
        if (!gameActive) {
            player.sendMessage(ChatColor.DARK_RED + "This game has already ended.");
            return;
        }
        if (playerTeams.containsKey(player)) {
            player.sendMessage(ChatColor.DARK_RED + "You can only pick one team.");
            return;
        }
        playerTeams.put(player, team);
        Location teleportLocation = team == Team.RED ? goal1.clone().add(0, 1, 0) : goal2.clone().add(0, 1, 0);
        player.teleport(teleportLocation);
        ChatColor teamColor = team == Team.RED ? ChatColor.RED : ChatColor.BLUE;
        player.sendMessage(teamColor + "You have joined the " + teamColor + team.name() + " team!");
        equipTeamArmor(player, team);
    }

    private void equipTeamArmor(Player player, Team team) {
        Color color = team == Team.RED ? Color.RED : Color.BLUE;

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(color);
        helmet.setItemMeta(helmetMeta);

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestplateMeta.setColor(color);
        chestplate.setItemMeta(chestplateMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        leggingsMeta.setColor(color);
        leggings.setItemMeta(leggingsMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(color);
        boots.setItemMeta(bootsMeta);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (gameActive && playerTeams.containsKey(player)) {
            Location to = event.getTo();
            if (to.getX() < corner1.getX() - 1 || to.getX() > corner2.getX() + 1 ||
                    to.getY() < corner1.getY() - 1 || to.getY() > corner2.getY() + 1 ||
                    to.getZ() < corner1.getZ() - 1 || to.getZ() > corner2.getZ() + 1) {
                event.setCancelled(true);
                Location teleportLocation = playerTeams.get(player) == Team.RED ? goal1.clone().add(0, 1, 0) : goal2.clone().add(0, 1, 0);
                player.teleport(teleportLocation);
                player.sendMessage(ChatColor.DARK_RED + "You cannot leave the arena until the game ends.");
            }
        }
    }
}