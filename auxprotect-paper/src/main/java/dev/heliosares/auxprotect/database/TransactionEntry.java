package dev.heliosares.auxprotect.database;

import dev.heliosares.auxprotect.adapters.sender.SenderAdapter;
import dev.heliosares.auxprotect.api.AuxProtectAPI;
import dev.heliosares.auxprotect.paper.AuxProtectPaper;
import dev.heliosares.auxprotect.utils.InvSerialization;
import dev.kshl.kshlib.exceptions.BusyException;
import java.io.IOException;
import java.sql.SQLException;
import javax.annotation.Nullable;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class TransactionEntry extends SpigotDbEntry {

  @Getter
  private final short quantity;
  @Getter
  private final double cost;
  @Getter
  private final double balance;

  protected String targetLabel2;
  protected String target2;
  protected int target_id2;


  public TransactionEntry(String userLabel, EntryAction action, boolean state,
      @Nullable Location location, String targetLabel, String data, short quantity, double cost,
      double balance, String targetLabel2) {
    super(userLabel, action, state, location, targetLabel, data);
    this.quantity = quantity;
    this.cost = cost;
    this.balance = balance;
    this.targetLabel2 = targetLabel2;
  }

  // TODO this could be moved to a subclass if needed to make it non-Bukkit specific
  public TransactionEntry(String userLabel, EntryAction action, boolean state,
      @Nullable Location location, String targetLabel, String data, short quantity, double cost,
      double balance, @Nullable ItemStack itemStack, String targetLabel2) {
    super(userLabel, action, state, location, targetLabel, data);
    this.quantity = quantity > 0 || itemStack == null ? quantity : (short) itemStack.getAmount();
    this.cost = cost;
    this.balance = balance;
    this.targetLabel2 = targetLabel2;

    if (itemStack == null || !InvSerialization.isCustom(itemStack)) {
      return;
    }

    itemStack = itemStack.clone();
    itemStack.setAmount(quantity);
    try {
      setBlob(InvSerialization.toByteArray(itemStack));
    } catch (IOException e) {
      AuxProtectAPI.warning("Failed to serialize " + itemStack);
      AuxProtectAPI.getInstance().print(e);
    }
  }

  public TransactionEntry(long time, int uid, EntryAction action, boolean state, String world,
      int x, int y, int z, int pitch, int yaw, int target_id, String data, short quantity,
      double cost, double balance, int target_id2, SQLManager sql) {
    super(time, uid, action, state, world, x, y, z, pitch, yaw, null, target_id, data, sql);
    this.quantity = quantity;
    this.cost = cost;
    this.balance = balance;
    this.target_id2 = target_id2;
  }

  public ItemStack getItem()
      throws SQLException, BusyException, IOException, ClassNotFoundException {
    if (getBlob() == null) {
      return null;
    }

    ItemStack item = InvSerialization.toItemStack(getBlob());
    item.setAmount(quantity);
    return item;
  }

  public int getTargetId2() throws SQLException, BusyException {
    if (action.getTable().hasStringTarget()) {
      return -1;
    }
    if (target_id2 > 0) {
      return target_id2;
    }
    return target_id2 = sql.getUserManager().getUID(getTargetUUID2(), true);
  }

  public String getTarget2() throws SQLException, BusyException {
    return getTarget2(true);
  }

  public String getTarget2(boolean resolve) throws SQLException, BusyException {
    if (target2 != null || !resolve) {
      return target2;
    }

    if (!getTargetUUID2().startsWith("$") || getTargetUUID2().length() != 37) {
      return target2 = getTargetUUID2();
    }
    target2 = sql.getUserManager().getUsernameFromUID(getTargetId2());
    if (target2 == null) {
      target2 = getTargetUUID2();
    }
    return target2;
  }

  public String getTargetUUID2() throws SQLException, BusyException {
    if (targetLabel2 != null) {
      return targetLabel2;
    }
    if (target_id2 > 0) {
      targetLabel2 = sql.getUserManager().getUUIDFromUID(target_id2);
    } else if (target_id2 == 0) {
      return targetLabel2 = "";
    }
    if (targetLabel2 == null) {
      targetLabel2 = "#null";
    }
    return targetLabel2;
  }

  @Override
  public Component dataComponent(SenderAdapter<?, ?> sender) {
    TextComponent.Builder builder = Component.text();

    String balance = AuxProtectPaper.getInstance().formatMoney(getBalance());
    builder.append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("Balance: " + balance, NamedTextColor.GRAY)
            .hoverEvent(Results.clickToCopyHoverEvent)
            .clickEvent(ClickEvent.copyToClipboard(balance)))
        .append(Component.text("]", NamedTextColor.DARK_GRAY));

    return builder.build();
  }
}
