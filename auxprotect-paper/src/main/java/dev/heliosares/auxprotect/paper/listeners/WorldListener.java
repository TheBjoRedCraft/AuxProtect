package dev.heliosares.auxprotect.paper.listeners;

import dev.heliosares.auxprotect.database.DbEntry;
import dev.heliosares.auxprotect.database.EntryAction;
import dev.heliosares.auxprotect.database.SQLManager;
import dev.heliosares.auxprotect.database.SingleItemEntry;
import dev.heliosares.auxprotect.database.SpigotDbEntry;
import dev.heliosares.auxprotect.paper.AuxProtectPaper;
import org.bukkit.Location;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;

public class WorldListener implements Listener {

  private final AuxProtectPaper plugin;

  public WorldListener(AuxProtectPaper plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onLightningStrikeEvent(LightningStrikeEvent e) {
    plugin.add(
        new SpigotDbEntry("#env", EntryAction.LIGHTNING, false, e.getLightning().getLocation(), "",
            e.getLightning().isEffect() ? "effect" : ""));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onHangingBreakEvent(HangingBreakEvent e) {
    if (e.getCause() == RemoveCause.ENTITY) {
      return;
    }
    if (e.getEntity() instanceof final ItemFrame item) {
      DbEntry entry = new SingleItemEntry("#" + e.getCause().toString().toLowerCase(),
          EntryAction.ITEMFRAME, false, item.getLocation(),
          item.getItem().getType().toString().toLowerCase(), "", item.getItem());
      plugin.add(entry);
    }

  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void on(RaidTriggerEvent e) {
    AuxProtectPaper.getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(
        () -> plugin.add(
            createDbEntry(AuxProtectPaper.getLabel(e.getPlayer()), EntryAction.RAIDTRIGGER, false,
                e.getPlayer().getLocation(), "", "")), 3L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void on(RaidSpawnWaveEvent e) {
    AuxProtectPaper.getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(
        () -> e.getRaiders().forEach(raider -> plugin.add(
            createDbEntry("#raid", EntryAction.RAIDSPAWN, false, raider.getLocation(),
                AuxProtectPaper.getLabel(raider), ""))), 1L);
  }

  @EventHandler
  public void on(BlockDispenseLootEvent e) {
    for (ItemStack loot : e.getDispensedLoot()) {
      SingleItemEntry sie = new SingleItemEntry(
          "#" + e.getBlock().getType(),
          EntryAction.DROP,
          false,
          e.getBlock().getLocation(),
          loot.getType().toString().toLowerCase(),
          "",
          loot
      );
      plugin.add(sie);
    }
  }

  private DbEntry createDbEntry(String userLabel, EntryAction action, boolean state,
      Location location, String targetLabel, String data) {
    return new DbEntry(
        userLabel, action, state, location.getWorld().getName(), location.blockX(),
        location.blockY(), location.blockZ(), (int) location.getPitch(), (int) location.getYaw(),
        targetLabel, data,
        SQLManager.getInstance()
    );
  }
}
