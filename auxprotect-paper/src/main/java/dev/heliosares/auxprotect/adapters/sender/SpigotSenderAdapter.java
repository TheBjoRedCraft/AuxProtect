package dev.heliosares.auxprotect.adapters.sender;

import com.palmergames.paperlib.PaperLib;
import dev.heliosares.auxprotect.AuxProtectPaper;
import dev.heliosares.auxprotect.adapters.location.LocationAdapter;
import dev.heliosares.auxprotect.adapters.location.SpigotLocationAdapter;
import dev.heliosares.auxprotect.exceptions.NotPlayerException;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpigotSenderAdapter extends SenderAdapter<CommandSender, AuxProtectPaper> implements
    PositionedSender, BungeeComponentSender {

  int tries;

  public SpigotSenderAdapter(AuxProtectPaper plugin, CommandSender sender) {
    super(sender, plugin);
  }

  public void sendMessageRaw_(String message) {
    sender.sendMessage(message);
  }

  public boolean hasPermission(String node) {
    return sender.hasPermission(node);
  }

  @Override
  public void sendMessage(BaseComponent... component) {
    getSender().spigot().sendMessage(component);
  }

  public String getName() {
    return sender.getName();
  }

  public UUID getUniqueId() {
    if (sender instanceof Player player) {
      return player.getUniqueId();
    }
    return UUID.fromString("00000000-0000-0000-0000-000000000000");
  }

  @Override
  public void sendMessage(Component message) {
    getSender().sendMessage(message);
  }

  @Override
  public void executeCommand(String command) {
    plugin.runSync(() -> plugin.getServer().dispatchCommand(sender, command));
  }

  @Override
  public boolean isConsole() {
    return sender.equals(plugin.getServer().getConsoleSender());
  }

  @Override
  public void teleport(String worldname, double x, double y, double z, int pitch, int yaw)
      throws NullPointerException, UnsupportedOperationException {
    if (sender instanceof Player player) {
      World world = plugin.getServer().getWorld(worldname);
      final Location target = new Location(world, x, y, z, yaw, pitch);
      PaperLib.teleportAsync(player, target);
      if (player.getGameMode() == GameMode.SPECTATOR) {
        AuxProtectPaper.getMorePaperLib().scheduling().entitySpecificScheduler(player)
            .runAtFixedRate(task -> {
              if (tries++ >= 5 || (player.getWorld().equals(target.getWorld())
                  && player.getLocation().distance(target) < 2)) {
                task.cancel();
                return;
              }
              PaperLib.teleportAsync(player, target);
            }, null, 2, 1);
      }
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public LocationAdapter getLocation() throws NotPlayerException {
    if (!(getSender() instanceof Player player)) {
      throw new NotPlayerException();
    }

    return new SpigotLocationAdapter(player.getLocation());
  }
}