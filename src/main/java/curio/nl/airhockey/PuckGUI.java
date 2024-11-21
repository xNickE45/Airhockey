package curio.nl.airhockey;

import me.legofreak107.library.guilibrary.ItemStackBuilder;
import me.legofreak107.library.guilibrary.gui.GuiItem;
import me.legofreak107.library.guilibrary.gui.GuiMenu;
import me.legofreak107.library.guilibrary.gui.InvalidGuiLayoutException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PuckGUI extends GuiMenu {



    // Layout for the GUI
    private int[][] layout = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 2, 3, 4, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
    };


    // Constructor
    public PuckGUI(Player player, Puck puck) {

        HashMap<Integer, GuiItem> items = new HashMap<>();

        GuiItem BluePuck = new GuiItem(
                new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR).setColor(0,0,255)
                        .setModelData(1).setName("§1Blue Puck").build(),
                event -> {
                    player.sendMessage("§6You Picked the Blue Puck!");
                    puck.setPuckItem(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(0,0,255)
                                    .setModelData(1)
                                    .build());

                    puck.getPuckEntity().getEquipment().setHelmet(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(0,0,255)
                                    .setModelData(1)
                                    .build());

                }
        );

        GuiItem PinkPuck = new GuiItem(
                new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR).setColor(255, 0, 215)
                        .setModelData(1).setName("§5Pink Puck").build(),
                event -> {
                    player.sendMessage("§6You Picked the Pink Puck!");
                    puck.setPuckItem(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(255, 0, 215)
                                    .setModelData(1)
                                    .build());

                    puck.getPuckEntity().getEquipment().setHelmet(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(255, 0, 215)
                                    .setModelData(1)
                                    .build());
                }
        );

        GuiItem RedPuck = new GuiItem(
                new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR).setColor(225,0,0)
                        .setModelData(1).setName("§4Red Puck").build(),
                event -> {
                    player.sendMessage("§6You Picked the Red Puck!");
                    puck.setPuckItem(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(225,0,0)
                                    .setModelData(1)
                                    .build());

                    puck.getPuckEntity().getEquipment().setHelmet(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(255,0,0)
                                    .setModelData(1)
                                    .build());

                }
        );

        GuiItem LightPinkPuck = new GuiItem(
                new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR).setColor(225,209,223)
                        .setModelData(1).setName("§dLight Pink Puck").build(),
                event -> {
                    player.sendMessage("§6You Picked the Light Pink Puck!");
                    puck.setPuckItem(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(225,85,255)
                                    .setModelData(1)
                                    .build());
                    puck.getPuckEntity().getEquipment().setHelmet(
                            new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .setColor(225,85,255)
                                    .setModelData(1)
                                    .build());

                }
        );


        // Adding items to the layout
        items.put(1, BluePuck);
        items.put(2, PinkPuck);
        items.put(3, RedPuck);
        items.put(4, LightPinkPuck);

        try {
            String title = PlainTextComponentSerializer.plainText().serialize(Component.text("§6Choose ur Puck"));
            init(layout, items, title, closeEvent -> {
//                closeEvent.getPlayer().sendMessage("You closed the GUI!");
            });
        } catch (InvalidGuiLayoutException e) {
            e.printStackTrace();
        }
    }

    // Method to open the GUI for the player
    public void openGUI(Player player) {
        player.sendMessage("Open PuckGUI");
        open(player);
    }


}