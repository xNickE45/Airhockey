package curio.nl.airhockey;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class GameArena {
    private final World world;
    private final JavaPlugin plugin;
    private Location corner1;
    private Location corner2;
    private Location goal1;
    private Location goal2;
    private BukkitTask particleTask;
    private final Map<Location, Material> originalBlocks = new HashMap<>();

    public GameArena(World world, JavaPlugin plugin) {
        this.world = world;
        this.plugin = plugin;
    }

    public void setArenaLocation(Location playerLocation) {
        double x = playerLocation.getX();
        double y = playerLocation.getY();
        double z = playerLocation.getZ();
        this.corner1 = new Location(world, x, y, z);
        this.corner2 = new Location(world, x + 20, y + 6, z + 20); // Increase arena size
        this.goal1 = new Location(world, x + 10, y - 1, z); // Adjust goal to be in the middle
        this.goal2 = new Location(world, x + 10, y - 1, z + 20); // Adjust goal to be in the middle
    }

    public void displayParticles() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (double x = corner1.getX(); x <= corner2.getX(); x += 0.5) {
                    for (double y = corner1.getY(); y <= corner2.getY(); y += 0.5) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, x, y, corner1.getZ()), 1);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, x, y, corner2.getZ()), 1);
                    }
                }
                for (double z = corner1.getZ(); z <= corner2.getZ(); z += 0.5) {
                    for (double y = corner1.getY(); y <= corner2.getY(); y += 0.5) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, corner1.getX(), y, z), 1);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, corner2.getX(), y, z), 1);
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
        for (int i = -1; i <= 1; i++) { // Make the goals 3 blocks wide
            world.getBlockAt(goal1.clone().add(i, 0, 0)).setType(Material.GOLD_BLOCK);
            world.getBlockAt(goal2.clone().add(i, 0, 0)).setType(Material.GOLD_BLOCK);
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
    }

    public void createArena() {
        flattenField();
        displayParticles();
        createGoals();
    }

    public void clearArena() {
        System.out.println("Clearing arena...");
        stopParticles();
        for (double x = corner1.getX(); x <= corner2.getX(); x++) {
            for (double y = corner1.getY(); y <= corner2.getY(); y++) {
                for (double z = corner1.getZ(); z <= corner2.getZ(); z++) {
                    Location loc = new Location(world, x, y, z);
                    Material originalMaterial = originalBlocks.get(loc);
                    if (originalMaterial != null) {
                        world.getBlockAt(loc).setType(originalMaterial);
                    }
                }
            }
        }
        for (int i = -1; i <= 1; i++) { // Clear the goals
            world.getBlockAt(goal1.clone().add(i, 0, 0)).setType(Material.AIR);
            world.getBlockAt(goal2.clone().add(i, 0, 0)).setType(Material.AIR);
        }
        originalBlocks.clear();
    }
}