package me.legofreak107.library.guilibrary.listeners;

import me.legofreak107.library.guilibrary.GuiLibrary;
import me.legofreak107.library.guilibrary.gui.GuiItem;
import me.legofreak107.library.guilibrary.gui.GuiMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!GuiLibrary.menusOpen.containsKey(player)) return;
        GuiMenu open = GuiLibrary.menusOpen.get(player);
        if (e.getCurrentItem() == null) {
            if (open.isLocked()) {
                e.setCancelled(true);
                return;
            }
            return;
        }
        if (player.getOpenInventory().getBottomInventory() == e.getClickedInventory()) {
            return;
        }
        e.setCancelled(true);
        int slot = e.getSlot();
        if (!open.getLinkedItems().containsKey(slot)) return;
        GuiItem item = open.getLinkedItems().get(slot);
        if (item.getClickHandler() == null) return;
        item.getClickHandler().accept(e);
    }

}
