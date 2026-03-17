package dev.heliosares.auxprotect.paper.listeners;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;
import dev.heliosares.auxprotect.AuxProtectPaper;
import dev.heliosares.auxprotect.database.EntryAction;
import dev.heliosares.auxprotect.database.TransactionEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ChestShopListener implements Listener {

  private final AuxProtectPaper plugin;

  public ChestShopListener(AuxProtectPaper plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onShopPostTransactionEvent(TransactionEvent e) {
    boolean state = e.getTransactionType() == TransactionType.BUY;
    short qty = 0;
    for (ItemStack i : e.getStock()) {
      qty += (short) i.getAmount();
    }
    if (qty == 0) {
      return;
    }

    String buyerLabel = AuxProtectPaper.getLabel(e.getClient());
    String sellerLabel = AuxProtectPaper.getLabel(e.getOwnerAccount().getUuid());

    plugin.add(
        new TransactionEntry(buyerLabel, EntryAction.SHOP_CS, state, e.getSign().getLocation(),
            e.getStock()[0].getType().toString().toLowerCase(), "", qty,
            e.getExactPrice().doubleValue(), plugin.getEconomy().getBalance(e.getClient()),
            e.getStock()[0], sellerLabel));
  }
}
