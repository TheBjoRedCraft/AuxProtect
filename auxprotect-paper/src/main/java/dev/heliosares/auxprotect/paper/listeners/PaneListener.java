package dev.heliosares.auxprotect.paper.listeners;

import dev.heliosares.auxprotect.adapters.sender.SpigotSenderAdapter;
import dev.heliosares.auxprotect.core.APPermission;
import dev.heliosares.auxprotect.paper.AuxProtectPaper;
import dev.heliosares.auxprotect.utils.Pane;
import dev.heliosares.auxprotect.utils.Pane.Type;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class PaneListener implements Listener {

  private final AuxProtectPaper plugin;

  public PaneListener(AuxProtectPaper plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClickEvent(InventoryClickEvent e) {
    if (e.getInventory().getHolder() != null && e.getInventory().getHolder() instanceof Pane pane) {
      if (pane.isCancelled()) {
        e.setCancelled(true);
        return;
      }
      if (pane.type == Type.SHOW) {
        if (!APPermission.INV_EDIT.hasPermission(
            new SpigotSenderAdapter(plugin, e.getWhoClicked()))) {
          e.setCancelled(true);
        }
        if (e.getInventory().equals(e.getClickedInventory())) {
          if (pane.click(e.getSlot())) {
            e.setCancelled(true);
          }
        }
      }
    }
  }

  @EventHandler
  public void onInventoryCloseEvent(InventoryCloseEvent e) {
    if (e.getInventory().getHolder() != null && e.getInventory().getHolder() instanceof Pane pane) {
      if (pane.type == Type.CLAIM) {
      }
      pane.close();
    }
  }
}
