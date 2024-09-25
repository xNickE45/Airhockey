package curio.nl.airhockey;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;

public class Puck implements Listener {
    private final World world;
    private final Location corner1;
    private final Location corner2;
    private final JavaPlugin plugin;
    private final GameArena gameArena;
    private ArmorStand puckEntity;
    private Vector velocity;
    private double rotationAngle;

    public Puck(JavaPlugin plugin, World world, Location spawnLocation, Location corner1, Location corner2, GameArena gameArena) {
        this.plugin = plugin;
        this.world = world;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.velocity = new Vector(0, 0, 0);
        this.rotationAngle = 0;
        this.gameArena = gameArena;
        spawnPuck(spawnLocation);
        startAutoMovement();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void spawnPuck(Location location) {
        puckEntity = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        puckEntity.setVisible(false);
        puckEntity.setGravity(false);
        puckEntity.setInvulnerable(true);
        puckEntity.setRemoveWhenFarAway(false); // Ensure the puck is not removed when far away

        // Create an item stack with the custom model
        ItemStack puckItem = new ItemStack(Material.STICK); // Use a placeholder item
        ItemMeta meta = puckItem.getItemMeta();
        meta.setCustomModelData(1); // Set the custom model data to match your model
        puckItem.setItemMeta(meta);
        puckEntity.setHelmet(puckItem);

        puckEntity.setHelmet(puckItem);
        puckEntity.setHeadPose(puckEntity.getHeadPose().setX(0)); // Ensure the head is not rotated
        puckEntity.teleport(location); // Position the puck directly on the ground
        puckEntity.setMarker(false); // Ensure the hitbox is enabled

        // Add initial random velocity
        double initialX = (Math.random() - 0.5) * 0.2;
        double initialZ = (Math.random() - 0.5) * 0.2;
        velocity = new Vector(initialX, 0, initialZ);
    }

    public void movePuck() {
        if (puckEntity == null) {
            return; // Prevent NullPointerException
        }

        Location currentLocation = puckEntity.getLocation();
        Location newLocation = currentLocation.add(velocity);

        // Ensure the puck only moves on the X and Z axes
        newLocation.setY(corner1.getY()); // Keep the puck on the ground

        // Check boundaries
        boolean hitWall = false;
        if (newLocation.getX() < corner1.getX() || newLocation.getX() > corner2.getX()) {
            velocity.setX(-velocity.getX()); // Reverse X direction on boundary hit
            hitWall = true;
        }
        if (newLocation.getZ() < corner1.getZ() || newLocation.getZ() > corner2.getZ()) {
            velocity.setZ(-velocity.getZ()); // Reverse Z direction on boundary hit
            hitWall = true;
        }

        // Check for gold block collision
        if (world.getBlockAt(newLocation).getType() == Material.GOLD_BLOCK) {
            velocity.multiply(-1); // Reverse direction on gold block hit
            hitWall = true;
        }

        if (hitWall) {
            // Add slight randomness to the velocity on bounce
            double randomX = (Math.random() - 0.5) * 0.1;
            double randomZ = (Math.random() - 0.5) * 0.1;
            velocity.add(new Vector(randomX, 0, randomZ));
        } else {
            puckEntity.teleport(newLocation);
        }

        // Add wind burst particles under the puck
        world.spawnParticle(Particle.CLOUD, newLocation.clone().add(0, -0.5, 0), 5, 0.1, 0.1, 0.1, 0.01);

        // Apply friction to gradually slow down the puck
        velocity.multiply(0.95);

        // Stop the puck if the velocity is very low
        if (velocity.length() < 0.05) {
            velocity.zero();
        }

        // Rotate the puck
        rotatePuck();
    }

    private void rotatePuck() {
        rotationAngle += 5; // Adjust the rotation speed as needed
        if (rotationAngle >= 360) {
            rotationAngle = 0;
        }
        puckEntity.setHeadPose(new EulerAngle(0, Math.toRadians(rotationAngle), 0));
    }

    private void startAutoMovement() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Add slight random movement to simulate air movement
                double randomX = (Math.random() - 0.5) * 0.02; // Reduced speed
                double randomZ = (Math.random() - 0.5) * 0.02; // Reduced speed
                velocity.add(new Vector(randomX, 0, randomZ));
                movePuck();
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for smoother movement
    }

    public void removePuck() {
        if (puckEntity != null) {
            puckEntity.remove();
            puckEntity = null;
        }
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(puckEntity)) {
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                if (gameArena.isPlayerInTeam(player)) {
                    Vector hitVelocity = player.getLocation().getDirection();
                    hitPuck(hitVelocity);
                } else {
                    player.sendMessage(ChatColor.DARK_RED + "You need to be in a team to play!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked().equals(puckEntity)) {
            event.setCancelled(true); // Cancel the interaction to prevent item removal
        }
    }

    public void hitPuck(Vector hitVelocity) {
        this.velocity.add(hitVelocity.multiply(1.5)); // Increase the multiplier for faster acceleration
    }
}