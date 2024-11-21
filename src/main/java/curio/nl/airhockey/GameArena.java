package curio.nl.airhockey;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.broadcastMessage;

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
    private Puck puck;
    private int redTeamScore = 0;
    private int blueTeamScore = 0;
    private final int winningScore = 8; // Define the winning score
    private BukkitTask gameTimerTask;
    private final int gameDuration = 600; // 10 minutes
    private final Map<Player, Location> playerOriginalLocations = new HashMap<>();
    private boolean joinPeriodEnded = false;
    private Location puckSpawnLocation;


    public GameArena(World world, JavaPlugin plugin) {
        this.world = world;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void noBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (gameActive && playerTeams.containsKey(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.DARK_RED + "You cannot break blocks during the game.");
        }
    }

    @EventHandler
    private void noBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (gameActive && playerTeams.containsKey(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.DARK_RED + "You cannot place blocks during the game.");
        }
    }

    @EventHandler
    private void noPlayerHit(EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player && gameActive) {
            Player player = (Player) event.getDamager();
            if (playerTeams.containsKey(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You cannot hit other players during the game.");
            }
        }
    }

    @EventHandler
    private void playerLeftArena(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (gameActive && playerTeams.containsKey(player)) {
            Location to = event.getTo();
            if (to.getX() < corner1.getX() - 1 || to.getX() > corner2.getX() + 1 ||
                    to.getY() < corner1.getY() - 1 || to.getY() > corner2.getY() + 1 ||
                    to.getZ() < corner1.getZ() - 1 || to.getZ() > corner2.getZ() + 1) {
                event.setCancelled(true);
                teleportPlayerBackToArena(player);
            }
        }
    }
    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (gameActive && playerTeams.containsKey(player) && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Location to = event.getTo();
            if (to.getX() < corner1.getX() - 1 || to.getX() > corner2.getX() + 1 ||
                    to.getY() < corner1.getY() - 1 || to.getY() > corner2.getY() + 1 ||
                    to.getZ() < corner1.getZ() - 1 || to.getZ() > corner2.getZ() + 1) {
                event.setCancelled(true);
                teleportPlayerBackToArena(player);
                player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You cannot leave the arena until the game ends.");
            }
        }
    }

    private void teleportPlayerBackToArena(Player player) {
        Location to = player.getLocation();
        if (to.getX() < corner1.getX() - 1 || to.getX() > corner2.getX() + 1 ||
                to.getY() < corner1.getY() - 1 || to.getY() > corner2.getY() + 1 ||
                to.getZ() < corner1.getZ() - 1 || to.getZ() > corner2.getZ() + 1) {
            Team team = playerTeams.get(player);
            Location teleportLocation = team == Team.RED ? goal1.clone().add(0, 1, 0) : goal2.clone().add(0, 1, 0);
            player.teleport(teleportLocation);
            player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You cannot leave the arena until the game ends.");
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (playerTeams.containsKey(player)) {
            playerTeams.remove(player);
            player.sendMessage(ChatColor.DARK_RED + "You have ragequited the game.");
            checkGameStatus();
        }
    }

    private void checkGameStatus() {
        boolean redTeamHasPlayers = playerTeams.values().stream().anyMatch(t -> t == Team.RED);
        boolean blueTeamHasPlayers = playerTeams.values().stream().anyMatch(t -> t == Team.BLUE);

        if (!redTeamHasPlayers || !blueTeamHasPlayers) {
            broadcastMessage(ChatColor.DARK_RED + "The game has ended because a team has Ragequited.");
            endGame(null); // End the game if one of the teams has no players
        }
    }

    public void setArenaLocation(Location playerLocation) {
        double x = playerLocation.getX();
        double y = 255;
        double z = playerLocation.getZ();
        this.corner1 = new Location(world, x, y, z);
        this.corner2 = new Location(world, x + 37, y + 6, z + 37); // Adjust arena size
        this.goal1 = new Location(world, x + 18.5, y - 1, z + 2); // Adjust goal to be in the middle
        this.goal2 = new Location(world, x + 18.5, y - 1, z + 35, 180,0); // Adjust goal to be in the middle
    }

    public void displayParticles() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (double x = corner1.getX() - 1; x <= corner2.getX() + 1; x += 0.5) {
                    world.spawnParticle(Particle.HAPPY_VILLAGER, x, corner1.getY(), corner1.getZ() - 1, 1);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, x, corner1.getY(), corner2.getZ() + 1, 1);
                }
                for (double z = corner1.getZ() - 1; z <= corner2.getZ() + 1; z += 0.5) {
                    world.spawnParticle(Particle.HAPPY_VILLAGER, corner1.getX() - 1, corner1.getY(), z, 1);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, corner2.getX() + 1, corner1.getY(), z, 1);
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
                    world.getBlockAt(new Location(world, x, corner1.getY() - 1, z)).setType(Material.IRON_BLOCK);
                }
            }
        }
    }

    public void createArena() {
        flattenField();
        displayParticles();
        createGoals();
        spawnPuck();
        gameActive = true;

        checkAndStartCountdown();
    }

    public void clearArena() {
        System.out.println("Clearing arena...");
        stopParticles();
        if (puck != null) {
            puck.removePuck();
            puck = null;
        }
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
                    world.getBlockAt(new Location(world, x, y, z)).setType(Material.AIR);
                }
            }
        }
        // Clear the border
        for (double x = corner1.getX() - 1; x <= corner2.getX() + 1; x++) {
            for (double z = corner1.getZ() - 1; z <= corner2.getZ() + 1; z++) {
                if (x == corner1.getX() - 1 || x == corner2.getX() + 1 || z == corner1.getZ() - 1 || z == corner2.getZ() + 1) {
                    world.getBlockAt(new Location(world, x, corner1.getY() - 1, z)).setType(Material.AIR);
                }
            }
        }
        originalBlocks.clear();
    }

    public void endGame(Team winningTeam) {
        if (!gameActive) {
            broadcastMessage(ChatColor.DARK_RED + "There is no game happening right now.");
            return;
        }
        clearArena();
        for (Player p : playerTeams.keySet()) {
            p.getInventory().setHelmet(null);
            p.getInventory().setChestplate(null);
            p.getInventory().setLeggings(null);
            p.getInventory().setBoots(null);
            if (winningTeam !=null) {
                ChatColor teamColor = winningTeam == Team.RED ? ChatColor.RED : ChatColor.BLUE;
                p.sendTitle( teamColor + winningTeam.name() + " Team wins!", "", 10, 70, 20);
            }
            p.setWalkSpeed(0.2f); // Enable movement
            p.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.42); // Enable jumping
            if (playerOriginalLocations.containsKey(p)) {
                p.teleport(playerOriginalLocations.get(p));
            }
        }
        playerTeams.clear();
        gameActive = false;
        redTeamScore = 0;
        blueTeamScore = 0;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

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
        if (joinPeriodEnded) {
            player.sendMessage(ChatColor.DARK_RED + "The team joining period has ended. You cannot join a team now.");
            return;
        }
        if (playerTeams.containsKey(player)) {
            player.sendMessage(ChatColor.DARK_RED + "You can only pick one team.");
            return;
        }
        playerOriginalLocations.put(player, player.getLocation());
        playerTeams.put(player, team);
        Location teleportLocation = team == Team.RED ? goal1.clone().add(0, 1, 0) : goal2.clone().add(0, 1, 0);
        player.teleport(teleportLocation);
        ChatColor teamColor = team == Team.RED ? ChatColor.RED : ChatColor.BLUE;
        player.sendMessage(teamColor + "You have joined the " + teamColor + team.name() + " team!");
        equipTeamArmor(player, team);
        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0); // Set jump strength
        player.setWalkSpeed(0);

        playerTeamJoinsTimer();
        teamBalance();

        PuckGUI puckGUI = new PuckGUI(player, puck);
        puckGUI.openGUI(player);
    }

    private void checkAndStartCountdown() {
        boolean redTeamHasPlayers = playerTeams.values().stream().anyMatch(t -> t == Team.RED);
        boolean blueTeamHasPlayers = playerTeams.values().stream().anyMatch(t -> t == Team.BLUE);

        if (redTeamHasPlayers && blueTeamHasPlayers) {
            startCountdown();
        } else {
            broadcastMessage(ChatColor.GOLD + "Waiting for players to join both teams.");
        }
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
    private void spawnPuck() {
        puckSpawnLocation = new Location(world, (corner1.getX() + corner2.getX()) / 2, corner1.getY(), (corner1.getZ() + corner2.getZ()) / 2);
        if (puck == null) {
            puck = new Puck(plugin, world, puckSpawnLocation, corner1, corner2, this);
        }
    }

    public boolean isPlayerInTeam(Player player) {
        return playerTeams.containsKey(player);
    }

    public void checkGoal(Location puckLocation) {
        if (isInGoal(puckLocation, goal1)) {
            blueTeamScore++;
            broadcastScore();
            if (blueTeamScore >= winningScore) {
                endGame(Team.BLUE);
            } else {
                puck.setVelocity(new Vector(0, 0, 0)); // Stop the puck
                puck.teleport(puckSpawnLocation);
                startCountdown();
            }
        } else if (isInGoal(puckLocation, goal2)) {
            redTeamScore++;
            broadcastScore();
            if (redTeamScore >= winningScore) {
                endGame(Team.RED);
            } else {
                puck.setVelocity(new Vector(0, 0, 0)); // Stop the puck
                puck.teleport(puckSpawnLocation);
                startCountdown();
            }
        }
    }

    private void startCountdown() {
        for (Player player : playerTeams.keySet()) {
            // Teleport players to their respective goals
            Location teleportLocation = playerTeams.get(player) == Team.RED ? goal1.clone().add(0, 1, 0) : goal2.clone().add(0, 1, 0);
            player.teleport(teleportLocation);
            player.sendMessage(ChatColor.GOLD + "Get ready! The game will start soon.");
            player.setWalkSpeed(0); // Disable movement
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0); // Disable jumping
        }

        new BukkitRunnable() {
            int countdown = 5; // 5-second countdown

            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player player : playerTeams.keySet()) {
                        player.sendTitle(ChatColor.RED + "Starting in", ChatColor.YELLOW + String.valueOf(countdown), 10, 20, 10);
                    }
                    countdown--;
                } else {
                    for (Player player : playerTeams.keySet()) {
                        player.sendTitle(ChatColor.GREEN + "Go!", "", 10, 20, 10);
                        player.setWalkSpeed(0.2f); // Enable movement
                        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.42); // Enable jumping

                    }
                    startGameTimer();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
    }
    private void startGameTimer() {
        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                endGame(null);
                broadcastMessage(ChatColor.DARK_RED + "The game has ended due to time running out.");
            }
        }.runTaskLater(plugin, gameDuration * 20); // Convert seconds to ticks
    }

    private boolean isInGoal(Location puckLocation, Location goal) {
        // Define goal boundaries and check if puckLocation is within them
        return puckLocation.distance(goal) < 1.5; // Example distance check
    }


    private void broadcastScore() {
        for (Player player : playerTeams.keySet()) {
            player.sendMessage(ChatColor.RED + "Red Team: " + redTeamScore + " | " + ChatColor.BLUE + "Blue Team: " + blueTeamScore);
        }
    }

    private void broadcastMessage(String message) {
        for (Player player : playerTeams.keySet()) {
            player.sendMessage(message);
        }
    }

    private void teamBalance() {
        int redTeamCount = 0;
        int blueTeamCount = 0;
        for (Team team : playerTeams.values()) {
            if (team == Team.RED) {
                redTeamCount++;
            } else {
                blueTeamCount++;
            }
        }

        if (redTeamCount > blueTeamCount + 1) {
            for (Player player : playerTeams.keySet()) {
                if (playerTeams.get(player) == Team.RED) {
                    playerTeams.put(player, Team.BLUE);
                    player.sendMessage(ChatColor.BLUE + "You have been moved to the Blue team to balance the game.");
                    Location teleportLocation = goal2.clone().add(0, 1, 0);
                    player.teleport(teleportLocation);
                    equipTeamArmor(player, Team.BLUE);
                    break;
                }
            }
        } else if (blueTeamCount > redTeamCount + 1) {
            for (Player player : playerTeams.keySet()) {
                if (playerTeams.get(player) == Team.BLUE) {
                    playerTeams.put(player, Team.RED);
                    player.sendMessage(ChatColor.RED + "You have been moved to the Red team to balance the game.");
                    Location teleportLocation = goal1.clone().add(0, 1, 0);
                    player.teleport(teleportLocation);
                    equipTeamArmor(player, Team.RED);
                    break;
                }
            }
        }
    }

    public void playerTeamJoinsTimer() {
        if (playerTeams.size() >= 2) {
            new BukkitRunnable() {
                int joinCountdown = 20; // 20 seconds for players to join

                @Override
                public void run() {
                    if (joinCountdown > 0) {
                        if (joinCountdown == 20 || joinCountdown == 10 || joinCountdown == 5 || joinCountdown <= 5) {
                            broadcastMessage(ChatColor.GOLD + "Players have " + joinCountdown + " seconds to join teams.");
                        }
                        joinCountdown--;
                    } else {
                        broadcastMessage(ChatColor.RED + "Team joining period has ended. The game will start soon.");
                        joinPeriodEnded = true;
                        this.cancel();
                        checkAndStartCountdown();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
        }
    }

    public boolean isGameActive() {
        return gameActive;
    }
}