package dev.heliosares.auxprotect.database;

import dev.heliosares.auxprotect.adapters.sender.SenderAdapter;
import dev.heliosares.auxprotect.core.APPermission;
import dev.heliosares.auxprotect.core.Language;
import dev.heliosares.auxprotect.utils.TimeUtil;
import dev.kshl.kshlib.exceptions.BusyException;
import java.sql.SQLException;
import java.util.TimeZone;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class DbEntry {

  @Getter
  protected final String world;
  @Getter
  protected final int x, y, z, pitch, yaw;
  @Getter
  protected final EntryAction action;
  protected final boolean state;
  @Getter
  private final long snowflake;
  protected SQLManager sql;
  @Setter
  @Getter
  protected String data;
  protected String userLabel;
  protected String user;
  protected int uid;
  protected String targetLabel;
  protected String target;
  protected int target_id;
  private long blobid = -1;
  @Setter
  private byte[] blob;

  public DbEntry(String userLabel, EntryAction action, boolean state, String world, int x, int y,
      int z,
      int pitch,
      int yaw, String targetLabel, String data, SQLManager sql) {
    this.snowflake = Snowflake.getNextSnowflake();
    this.userLabel = userLabel;
    this.action = action;
    this.state = state;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.pitch = pitch;
    this.yaw = yaw;
    this.targetLabel = targetLabel;
    this.data = data;
    this.sql = sql;
  }

  public DbEntry(String userLabel, EntryAction action, boolean state, String targetLabel,
      String data) {
    this(userLabel, action, state, null, 0, 0, 0, 0, 0, targetLabel, data,
        SQLManager.getInstance());
  }

  protected DbEntry(long snowflake, int uid, EntryAction action, boolean state, String world, int x,
      int y, int z,
      int pitch, int yaw, String target, int target_id, String data, SQLManager sql) {
    this.snowflake = snowflake;
    this.uid = uid;
    this.action = action;
    this.state = state;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.pitch = pitch;
    this.yaw = yaw;
    this.targetLabel = target;
    this.target_id = target_id;
    this.data = data;
    this.sql = sql;
  }

  protected SQLManager getSql() {
    return sql;
  }

  void setSql(SQLManager sql) {
    this.sql = sql;
  }

  public void deResolveUIDs() {
    uid = -1;
    target_id = -1;
  }

  public long getTime() {
    return snowflake / Snowflake.COUNTER_FACTOR;
  }

  public long getCounter() {
    return snowflake % Snowflake.COUNTER_FACTOR;
  }

  public boolean getState() {
    return state;
  }

  public int getUid() throws SQLException, BusyException {
    if (uid > 0) {
      return uid;
    }
    return uid = sql.getUserManager().getUID(getUserUUID(), true);
  }

  public String getUser() throws SQLException, BusyException {
    return getUser(true);
  }

  public String getUser(boolean resolve) throws SQLException, BusyException {
    if (user != null || !resolve) {
      return user;
    }
    if (!getUserUUID().startsWith("$") || getUserUUID().length() != 37) {
      return user = getUserUUID();
    }
    user = sql.getUserManager().getUsernameFromUID(getUid());
    return user == null ? (user = getUserUUID()) : user;
  }

  public int getTargetId() throws SQLException, BusyException {
    if (action.getTable().hasStringTarget()) {
      return -1;
    }
    if (target_id > 0) {
      return target_id;
    }
    return target_id = sql.getUserManager().getUID(getTargetUUID(), true);
  }

  public String getTarget() throws SQLException, BusyException {
    return getTarget(true);
  }

  public String getTarget(boolean resolve) throws SQLException, BusyException {
    if (target != null || !resolve) {
      return target;
    }
    if (action.getTable().hasStringTarget() || !getTargetUUID().startsWith("$")
        || getTargetUUID().length() != 37) {
      return target = getTargetUUID();
    }
    target = sql.getUserManager().getUsernameFromUID(getTargetId());
    return target == null ? (target = getTargetUUID()) : target;
  }

  public String getTargetUUID() throws SQLException, BusyException {
    if (targetLabel != null) {
      return targetLabel;
    }
    if (target_id > 0) {
      targetLabel = sql.getUserManager().getUUIDFromUID(target_id);
    } else if (target_id == 0) {
      return targetLabel = "";
    }
    return targetLabel == null ? (targetLabel = "#null") : targetLabel;
  }

  public String getUserUUID() throws SQLException, BusyException {
    if (userLabel != null) {
      return userLabel;
    }
    if (uid > 0) {
      userLabel = sql.getUserManager().getUUIDFromUID(uid);
    } else if (uid == 0) {
      return userLabel = "";
    }
    return userLabel == null ? (userLabel = "#null") : userLabel;
  }

  public double getDistance(DbEntry entry) {
    return Math.sqrt(getDistanceSq(entry));
  }

  public double getDistanceSq(DbEntry entry) {
    return Math.pow(getX() - entry.getX(), 2)
        + Math.pow(getY() - entry.getY(), 2)
        + Math.pow(getZ() - entry.getZ(), 2);
  }

  public byte[] getBlob() throws SQLException, BusyException {
    if (blob == null) {
      blob = sql.getBlob(this);
    }
    return blob;
  }

  public boolean hasBlob() {
    return blob != null || blobid >= 0;
  }

  public long getBlobID() {
    return blobid;
  }

  protected void setBlobID(long blobid) {
    this.blobid = blobid;
  }

  public Component toComponent(SenderAdapter<?, ?> sender, String commandPrefix, int index,
      TimeZone timeZone) throws SQLException, BusyException {
    TextComponent.Builder builder = Component.text();

    builder.append(Component.text(getUser(), NamedTextColor.BLUE))
        .append(Component.text(" "))
        .append(Component.text(getAction().getText(getState()), NamedTextColor.WHITE))
        .append(Component.text(" "))
        .append(Component.text(getTarget(), NamedTextColor.BLUE))
        .append(Component.text(" "));

    builder.append(timeComponent(timeZone));
    builder.append(dataComponent(sender));
    builder.append(buttonsComponent(sender, commandPrefix, index));
    builder.append(coordinatesComponent(sender));

    return builder.build();
  }

  public Component timeComponent(TimeZone timeZone) {
    String msg = System.currentTimeMillis() - getTime() < 55
        ? Language.L.RESULTS__TIME_NOW.translate()
        : Language.L.RESULTS__TIME.translate(
            TimeUtil.millisToString(System.currentTimeMillis() - getTime()));

    return Component.text(msg, NamedTextColor.GRAY)
        .hoverEvent(HoverEvent.showText(
            Component.text(
                    TimeUtil.format(getTime(), TimeUtil.entryTimeFormat, timeZone.toZoneId()))
                .append(Component.newline())
                .append(Component.text(Language.L.RESULTS__CLICK_TO_COPY_TIME.translate(getTime())))
        ))
        .clickEvent(ClickEvent.copyToClipboard(getTime() + "e"));
  }

  public Component dataComponent(SenderAdapter<?, ?> sender) {
    String data = getData();
    if (data == null || data.isEmpty()) {
      return Component.empty();
    }

    TextComponent.Builder builder = Component.text();

    if (getAction().equals(EntryAction.SESSION)
        && !APPermission.LOOKUP_ACTION.dot(EntryAction.SESSION.toString().toLowerCase()).dot("ip")
        .hasPermission(sender)) {

      builder.append(Component.text(" [REDACTED]", NamedTextColor.GRAY));
      return builder.build();
    }

    builder.append(Component.text(" [", NamedTextColor.GRAY))
        .append(Component.text(data, NamedTextColor.GRAY)
            .clickEvent(ClickEvent.copyToClipboard(data)))
        .append(Component.text("]", NamedTextColor.GRAY));

    return builder.build();
  }

  public Component buttonsComponent(SenderAdapter<?, ?> sender, String commandPrefix, int index)
      throws SQLException, BusyException {
    TextComponent.Builder builder = Component.text();

    if (hasBlob() && APPermission.INV.hasPermission(sender)) {
      builder.append(Component.text(" [" + Language.L.RESULTS__VIEW + "]", NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand(String.format(commandPrefix + " inv %d", index))));
    }

    if (getAction().equals(EntryAction.KILL)
        && APPermission.INV.hasPermission(sender)
        && !getTarget().startsWith("#")) {

      builder.append(Component.text(" [" + Language.L.RESULTS__VIEW_INV + "]", NamedTextColor.GREEN)
          .clickEvent(ClickEvent.runCommand(
              String.format(commandPrefix + " l u:%s a:inventory target:death time:%de+-20e",
                  getTarget(), getTime())
          )));
    }

    return builder.build();
  }

  public Component coordinatesComponent(SenderAdapter<?, ?> sender) {
    TextComponent.Builder builder = Component.text();

    builder.append(Component.newline())
        .append(
            Component.text(String.format("(x%d/y%d/z%d/%s)", x, y, z, world), NamedTextColor.GRAY));

    if (sender == null || APPermission.TP.hasPermission(sender)) {
      builder.clickEvent(ClickEvent.callback((audience) -> {
        sender.teleport(x + 0.5, y, z + 0.5, world);
      }));
    }

    return builder.build();
  }
}