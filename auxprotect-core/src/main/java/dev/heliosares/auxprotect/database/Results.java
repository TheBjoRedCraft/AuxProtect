package dev.heliosares.auxprotect.database;

import dev.heliosares.auxprotect.adapters.sender.SenderAdapter;
import dev.heliosares.auxprotect.core.APPlayer;
import dev.heliosares.auxprotect.core.IAuxProtect;
import dev.heliosares.auxprotect.core.Language;
import dev.heliosares.auxprotect.core.Parameters;
import dev.heliosares.auxprotect.core.Parameters.Flag;
import dev.heliosares.auxprotect.utils.TimeUtil;
import dev.kshl.kshlib.exceptions.BusyException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class Results {

  public static final char LEFT_ARROW = 9668;
  public static final char RIGHT_ARROW = 9658;
  public static final HoverEvent clickToCopyHoverEvent = HoverEvent.showText(
      Component.text(Language.L.RESULTS__CLICK_TO_COPY.translate(), NamedTextColor.GRAY));
  protected final SenderAdapter player;
  final IAuxProtect plugin;
  @Getter
  private final List<DbEntry> entries;
  private final Parameters params;
  @Setter
  @Getter
  private int perPage = 4;
  @Getter
  private int currentPage = 0;

  public Results(IAuxProtect plugin, List<DbEntry> entries, SenderAdapter player,
      Parameters params) {
    this.entries = entries;
    this.player = player;
    this.plugin = plugin;

    boolean allNullWorld = true;
    int count = 0;
    for (DbEntry entry : entries) {
      if (entry.getWorld() != null && !entry.getWorld().equals("#null")) {
        allNullWorld = false;
        break;
      }
      if (count++ > 1000) {
        break;
      }
    }
    if (allNullWorld) {
      setPerPage(10);
    }
    this.params = params;
  }

  public static void sendEntry(IAuxProtect plugin, SenderAdapter player, DbEntry entry, int index,
      boolean time, boolean coords, boolean showData) throws SQLException, BusyException {
    String commandPrefix = "/" + plugin.getCommandPrefix();
    APPlayer<?> apPlayer = plugin.getAPPlayer(player);
    TimeZone timeZone =
        time ? (apPlayer == null ? TimeZone.getDefault() : apPlayer.getTimeZone()) : null;

    if (entry instanceof DbEntryGroup group) {
      TextComponent.Builder builder = Component.text();
      if (time) {
        builder.append(Component.text(Language.L.RESULTS__TIME.translate(
                    TimeUtil.millisToString(System.currentTimeMillis() - group.getFirstTime()) +
                        "-" + TimeUtil.millisToString(System.currentTimeMillis() - group.getLastTime())),
                NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(
                Component.text(TimeUtil.format(group.getFirstTime(), TimeUtil.entryTimeFormat,
                        timeZone.toZoneId()) +
                        " - " + TimeUtil.format(group.getLastTime(), TimeUtil.entryTimeFormat,
                        apPlayer.getTimeZone().toZoneId()) +
                        "\n" + Language.L.RESULTS__CLICK_TO_COPY_TIME.translate(entry.getTime()),
                    NamedTextColor.GRAY)))
            .clickEvent(ClickEvent.copyToClipboard(group.getFormattedEpoch()))
        );
      }
      builder.append(
          Component.text(" " + Language.L.RESULTS__GROUPING_OF.translate(group.getNumEntries()),
                  NamedTextColor.GRAY)
              .clickEvent(ClickEvent.runCommand(commandPrefix + " lookup " + group.hash() + "g")));
      player.sendMessage(builder.build());
    } else {
      TextComponent.Builder builder = Component.text();
      if (time) {
        builder.append(entry.timeComponent(timeZone));
      }

      NamedTextColor actionColor = entry.getAction().hasDual ? (entry.getState() ? NamedTextColor.GREEN : NamedTextColor.RED) : NamedTextColor.GRAY;
      String actionSign = entry.getAction().hasDual ? (entry.getState() ? "+" : "-") : "-";
      builder.append(Component.text(" " + actionSign + " ", actionColor));
      builder.append(Component.text(entry.getUser(), NamedTextColor.BLUE))
          .append(Component.text(" "));
      builder.append(Component.text(entry.action.getText(entry.state), NamedTextColor.WHITE))
          .append(Component.text(" "));

      String target = entry.getTarget();
      if (target != null && !target.isEmpty()) {
        builder.append(Component.text(target, NamedTextColor.BLUE))
            .append(Component.text(" "));
      }

      if (showData) {
        builder.append(entry.dataComponent(player));
      }
      builder.append(entry.buttonsComponent(player, commandPrefix, index));

      if (entry.getWorld() != null && !entry.getWorld().equals("$null") && coords) {
        builder.append(entry.coordinatesComponent(player));
      }
      player.sendMessage(builder.build());
    }
  }

  public DbEntry get(int i) {
    return getEntries().get(i);
  }

  public void sendHeader() {
    String headerColor = "&t";
    StringBuilder line = new StringBuilder("&m");
    line.append(String.valueOf((char) 65293).repeat(6));
    line.append("&t");
    if (new Random().nextDouble() < 0.001) {
      headerColor = "&s";
    }
    player.sendMessageRaw(headerColor + line + "  " + Language.L.RESULTS__HEADER + "&t  " + line);
  }

  public void showPage(int page) throws SQLException, BusyException {
    showPage(page, getPerPage());
  }

  public void showPage(int page, int perPage_) throws SQLException, BusyException {
    int lastpage = getNumPages(perPage_);
    if (page > lastpage || page < 1) {
      player.sendLang(Language.L.COMMAND__LOOKUP__NOPAGE);
      return;
    }
    setPerPage(perPage_);
    setCurrentPage(page);
    sendHeader();
    for (int i = (page - 1) * getPerPage(); i < page * getPerPage() && i < getEntries().size();
        i++) {
      sendEntry(getEntries().get(i), i);
    }
    sendArrowKeys(page);
  }

  public void sendEntry(DbEntry entry, int index) throws SQLException, BusyException {
    sendEntry(plugin, player, entry, index, true, !params.hasFlag(Flag.HIDE_COORDS),
        !params.hasFlag(Flag.HIDE_DATA));
  }

  protected String getCommandPrefix() {
    return "/" + plugin.getCommandPrefix() + " l ";
  }

  protected String getCommand(int which) {
    return switch (which) {
      case -2 -> "1:" + getPerPage();
      case -1 -> (currentPage - 1) + ":" + getPerPage();
      case 1 -> (currentPage + 1) + ":" + getPerPage();
      case 2 -> getNumPages(getPerPage()) + ":" + getPerPage();
      default -> throw new IllegalArgumentException();
    };
  }

  public void sendArrowKeys(int page) {
    TextComponent.Builder builder = Component.text();
    int lastpage = getNumPages(getPerPage());
    builder.append(Component.text("(", NamedTextColor.GRAY));

    if (page > 1) {
      builder.append(Component.text(LEFT_ARROW + "" + LEFT_ARROW, NamedTextColor.AQUA)
          .clickEvent(ClickEvent.runCommand(getCommandPrefix() + getCommand(-2)))
          .hoverEvent(HoverEvent.showText(
              Component.text(Language.L.RESULTS__PAGE__FIRST.translate(), NamedTextColor.GRAY))));
      builder.append(Component.text(" "));
      builder.append(Component.text(LEFT_ARROW + "", NamedTextColor.AQUA)
          .clickEvent(ClickEvent.runCommand(getCommandPrefix() + getCommand(-1)))
          .hoverEvent(HoverEvent.showText(
              Component.text(Language.L.RESULTS__PAGE__PREVIOUS.translate(),
                  NamedTextColor.GRAY))));
    } else {
      builder.append(Component.text(LEFT_ARROW + "" + LEFT_ARROW, NamedTextColor.DARK_GRAY));
      builder.append(Component.text(" "));
      builder.append(Component.text(LEFT_ARROW + "", NamedTextColor.DARK_GRAY));
    }

    builder.append(Component.text("  "));

    if (page < lastpage) {
      builder.append(Component.text(RIGHT_ARROW + "", NamedTextColor.AQUA)
          .clickEvent(ClickEvent.runCommand(getCommandPrefix() + getCommand(1)))
          .hoverEvent(HoverEvent.showText(
              Component.text(Language.L.RESULTS__PAGE__NEXT.translate(), NamedTextColor.GRAY))));
      builder.append(Component.text(" "));
      builder.append(Component.text(RIGHT_ARROW + "" + RIGHT_ARROW, NamedTextColor.AQUA)
          .clickEvent(ClickEvent.runCommand(getCommandPrefix() + getCommand(2)))
          .hoverEvent(HoverEvent.showText(
              Component.text(Language.L.RESULTS__PAGE__LAST.translate(), NamedTextColor.GRAY))));
    } else {
      builder.append(Component.text(RIGHT_ARROW + "", NamedTextColor.DARK_GRAY));
      builder.append(Component.text(" "));
      builder.append(Component.text(RIGHT_ARROW + "" + RIGHT_ARROW, NamedTextColor.DARK_GRAY));
    }

    builder.append(Component.text(")  ", NamedTextColor.GRAY));
    Language.L lang = entries.getFirst() instanceof DbEntryGroup
        ? Language.L.COMMAND__LOOKUP__PAGE_FOOTER_GROUPS
        : Language.L.COMMAND__LOOKUP__PAGE_FOOTER;
    builder.append(Component.text(
        Language.translate(lang, page, getNumPages(getPerPage()), getEntries().size()),
        NamedTextColor.GRAY));
    player.sendMessage(builder.build());
  }

  public int getNumPages(int perpage) {
    return (int) Math.ceil(getEntries().size() / (double) perpage);
  }

  public int getSize() {
    return getEntries().size();
  }

  protected void setCurrentPage(int currentPage) {
    this.currentPage = currentPage;
  }
}