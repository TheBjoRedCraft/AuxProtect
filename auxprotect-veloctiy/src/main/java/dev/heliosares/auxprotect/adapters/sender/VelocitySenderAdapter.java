package dev.heliosares.auxprotect.adapters.sender;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.heliosares.auxprotect.velocity.AuxProtectVelocity;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public class VelocitySenderAdapter extends
    SenderAdapter<CommandSource, AuxProtectVelocity> implements KyoriSender {

  public VelocitySenderAdapter(AuxProtectVelocity plugin, CommandSource sender) {
    super(sender, plugin);
  }

  public void sendMessageRaw_(String message) {
    sender.sendMessage(Component.text(message));
  }

  public boolean hasPermission(String node) {
    return sender.hasPermission(node);
  }

  public String getName() {
    return sender instanceof Player player ? player.getUsername() : sender.getClass().getName();
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
    plugin.getProxy().getCommandManager().executeAsync(sender, command);
  }

  @Override
  public void teleport(double x, double y, double z, String world) {
    // Not supported
  }

  @Override
  public void teleport(double x, double y, double z, String world, float pitch, float yaw) {
    // Not supported
  }

  @Override
  public boolean isConsole() {
    return sender.equals(plugin.getProxy().getConsoleCommandSource());
  }

}
