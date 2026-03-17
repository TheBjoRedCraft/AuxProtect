package dev.heliosares.auxprotect.core.commands;

import dev.heliosares.auxprotect.adapters.sender.SenderAdapter;
import dev.heliosares.auxprotect.core.APPermission;
import dev.heliosares.auxprotect.core.APPlayer;
import dev.heliosares.auxprotect.core.Command;
import dev.heliosares.auxprotect.core.IAuxProtect;
import dev.heliosares.auxprotect.core.Language;
import dev.heliosares.auxprotect.exceptions.CommandException;
import dev.heliosares.auxprotect.exceptions.SyntaxException;
import dev.heliosares.auxprotect.utils.TimeUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class TimeCommand<S, P extends IAuxProtect, SA extends SenderAdapter<S, P>> extends
    Command<S, P, SA> {

  public TimeCommand(P plugin) {
    super(plugin, "time", APPermission.LOOKUP, false, "t");
  }

  @Override
  public void onCommand(SA sender, String label, String[] args) throws CommandException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMMyy HH:mm.ss v");

    if (args.length == 1 || args.length == 2) {
      long epochTime, relativeTime;
      boolean add = false;

      TextComponent.Builder builder = Component.text();

      if (args.length == 1) {
        builder.append(Component.text(Language.L.COMMAND__TIME__SERVER_TIME.translate()));
        epochTime = System.currentTimeMillis();
        relativeTime = 0;
      } else {
        String timeStr = args[1];
        add = timeStr.startsWith("+");
          if (add || timeStr.startsWith("-")) {
              timeStr = timeStr.substring(1);
          }
        boolean exact = timeStr.matches("\\d+e");

        try {
          if (exact) {
            epochTime = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
            relativeTime = System.currentTimeMillis() - epochTime;
          } else {
            relativeTime = TimeUtil.stringToMillis(timeStr);
              if (!add) {
                  relativeTime *= -1;
              }
            epochTime = System.currentTimeMillis() - relativeTime;
          }
        } catch (NumberFormatException e) {
          throw new SyntaxException();
        }

        builder.append(Component.text(Language.convert("&9" + timeStr + "&f ")));

        if (!exact) {
          builder.append(Component.text(add ? "from now" : "ago"));
        }
      }

      builder.append(
          Component.text(": (" + Language.L.COMMAND__TIME__CLICK_TO_COPY.translate() + ")",
              NamedTextColor.GRAY));

      BiConsumer<String, String> consume = (lineLabel, lineValue) -> {
        builder.append(Component.newline());

        builder.append(Component.text(Language.convert(lineLabel), NamedTextColor.WHITE))
            .append(Component.text(": "));

        builder.append(
            Component.text(Language.convert(lineValue), NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(
                    Component.text(Language.L.RESULTS__CLICK_TO_COPY.translate())))
                .clickEvent(ClickEvent.copyToClipboard(lineValue.replaceAll("§.", "")))
        );
      };

      TimeZone timeZone = Optional.ofNullable(plugin.getAPPlayer(sender))
          .map(APPlayer::getTimeZone)
          .orElse(TimeZone.getDefault());

      consume.accept(
          Language.L.COMMAND__TIME__FORMATTED.translate(),
          Instant.ofEpochMilli(epochTime).atZone(timeZone.toZoneId()).format(formatter)
      );

      if (relativeTime != 0) {
        Language.L fromNowAgo = (add ? Language.L.RESULTS__TIME_FROM_NOW
            : Language.L.RESULTS__TIME);

        consume.accept(
            Language.L.COMMAND__TIME__RELATIVE.translate(),
            fromNowAgo.translate(TimeUtil.millisToString(relativeTime))
        );

        consume.accept(
            Language.L.COMMAND__TIME__RELATIVE_EXPANDED.translate(),
            fromNowAgo.translate(TimeUtil.millisToStringExtended(relativeTime))
        );
      }

      consume.accept(
          Language.L.COMMAND__TIME__EPOCH_TIME.translate(),
          epochTime + "e"
      );

      if (relativeTime != 0) {
        consume.accept(
            Language.L.COMMAND__TIME__EPOCH_TIME.translate() + " +/- 1s",
            (epochTime - 1000) + "e-" + (epochTime + 1000) + "e"
        );
      }

      sender.sendMessage(builder.build());
      return;
    }

    throw new SyntaxException();
  }

  @Override
  public List<String> onTabComplete(SA sender, String label, String[] args) {
    return null;
  }

  @Override
  public boolean exists() {
    return true;
  }
}