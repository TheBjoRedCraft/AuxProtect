package dev.heliosares.auxprotect.utils;

import dev.heliosares.auxprotect.core.IAuxProtect;
import dev.heliosares.auxprotect.core.Language;
import dev.heliosares.auxprotect.database.DbEntry;
import dev.heliosares.auxprotect.database.EntryAction;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class PlayTimeSolver {

  public static final char BLOCK = 9608;

  public static Component solvePlaytime(IAuxProtect plugin, List<DbEntry> entries,
      long startTimeMillis, long stopTimeMillis, String player, final boolean currentlyOnline) {
    TextComponent.Builder message = Component.text();

    final int limitDays = 60;
    final int hours = (int) Math.ceil((stopTimeMillis - startTimeMillis) / 3600000D);
    if (hours - 1 > limitDays * 24) {
      return Component.text(Language.L.COMMAND__LOOKUP__PLAYTIME__TOOLONG.translate(limitDays));
    }

    Component line = Component.text(String.valueOf((char) 65293).repeat(6))
        .color(NamedTextColor.DARK_GRAY)
        .decorate(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH);

    message.append(line)
        .append(Component.text("  "))
        .append(Component.text(Language.L.COMMAND__LOOKUP__PLAYTIME__HEADER.translate(player,
            Language.getOptionalS(player))))
        .append(Component.text("  "))
        .append(line);

    LocalDateTime startTime = Instant.ofEpochMilli(startTimeMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .withMinute(0).withSecond(0).withNano(0);

    long firstTime = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    long start = System.currentTimeMillis();

    long login = 0;
    long logout = 0;
    boolean newLogin = false;
    boolean newLogout = false;

    double[] counter = new double[hours];
    int hour = 0;

    final long DAY_MILLIS = 24 * 3600000;
    long[] recentPlaytimeCutoff = {14 * DAY_MILLIS, 7 * DAY_MILLIS, 3 * DAY_MILLIS};
    double[] recentPlaytime = new double[recentPlaytimeCutoff.length];

    for (int i = entries.size() - 1; i >= 0; i--) {
      DbEntry entry = entries.get(i);

        if (entry.getAction() != EntryAction.SESSION) {
            continue;
        }
        if (login == 0 && !entry.getState()) {
            continue;
        }

      if (entry.getState()) {
        login = entry.getTime();
        newLogin = true;
      } else {
        logout = entry.getTime();
        newLogout = true;
      }

      if (i == 0 && newLogin && !newLogout) {
        if (currentlyOnline) {
          newLogout = true;
          logout = start;
        }
      }

      if (newLogin && newLogout) {
        if (logout < login) {
          newLogout = false;
          continue;
        }

        for (int i1 = 0; i1 < recentPlaytime.length; i1++) {
          long cutoff = System.currentTimeMillis() - recentPlaytimeCutoff[i1];
          if (login > cutoff) {
            recentPlaytime[i1] += (logout - login) / 3600_000D;
          } else if (logout > cutoff) {
            recentPlaytime[i1] += (logout - cutoff) / 3600_000D;
          }
        }

        while (hour < counter.length) {
          long hourTime = firstTime + (long) hour * 3600 * 1000;
          long hourTimeEnd = hourTime + 3600 * 1000;

          long overlap;
          boolean time1 = (login < hourTime);
          boolean time2 = (logout > hourTimeEnd);

          if (login > hourTimeEnd || logout < hourTime) {
            hour++;
            continue;
          }

          boolean next = false;

          if (time1 && time2) {
            overlap = 3600 * 1000;
            next = true;
          } else if (time1) {
            overlap = logout - hourTime;
          } else if (time2) {
            overlap = hourTimeEnd - login;
            next = true;
          } else {
            overlap = logout - login;
          }

          counter[hour] += overlap / (3600.0 * 1000.0);

          if (next) {
            hour++;
            continue;
          }
          break;
        }

        newLogin = false;
        newLogout = false;
      }
    }

    message.append(Component.newline());

    DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("ddMMM");
    DateTimeFormatter formatterDateTime = DateTimeFormatter.ofPattern("ddMMM hh a");

    for (int i = 0; i < startTime.getHour(); i++) {
      message.append(Component.text(BLOCK).color(NamedTextColor.BLACK));
    }

    double hourCount = 0;

    for (int i = 0; i < counter.length; i++) {
      LocalDateTime time = startTime.plusHours(i);
      double count = counter[i];

      TextColor color;
        if (count > 0.99) {
            color = TextColor.fromHexString("#ffffff");
        } else if (count > 0.75) {
            color = TextColor.fromHexString("#00ccff");
        } else if (count > 0.5) {
            color = TextColor.fromHexString("#1ecb0d");
        } else if (count > 0.25) {
            color = TextColor.fromHexString("#f9ff17");
        } else if (count > 0.001) {
            color = TextColor.fromHexString("#c50000");
        } else {
            color = TextColor.fromHexString("#4e0808");
        }

      message.append(
          Component.text(BLOCK)
              .color(color)
              .hoverEvent(HoverEvent.showText(
                  Component.text(Language.L.COMMAND__LOOKUP__PLAYTIME__HOVER.translate(
                      time.format(formatterDateTime),
                      Math.round(count * 60.0)
                  ))
              ))
      );

      hourCount += count;

      if (time.getHour() == 23) {
        message.append(Component.text(" " + time.format(formatterDate)).color(NamedTextColor.BLUE));
        message.append(Component.text(" (" + (Math.round(hourCount * 10.0) / 10.0) + "h)")
            .color(NamedTextColor.GRAY));
        message.append(Component.newline());
        hourCount = 0;
      }
    }

    for (int i = counter.length; ; i++) {
      LocalDateTime time = startTime.plusHours(i);
        if (time.getHour() == 0) {
            break;
        }
      message.append(Component.text(BLOCK).color(NamedTextColor.BLACK));
    }

    message.append(Component.text(" " + startTime.plusHours(hours).format(formatterDate))
        .color(NamedTextColor.BLUE));
    message.append(Component.text(" - " + (Math.round(hourCount * 10.0) / 10.0) + "h)")
        .color(NamedTextColor.GRAY));

    for (int i = 0; i < recentPlaytime.length; i++) {
      double pt = recentPlaytime[i];
      int days = (int) (recentPlaytimeCutoff[i] / DAY_MILLIS);

      message.append(Component.newline());
      message.append(Component.text(days + " days. ").color(NamedTextColor.BLUE));
      message.append(Component.text(" - " + pt + "h").color(NamedTextColor.GRAY));
    }

    return message.build();
  }
}