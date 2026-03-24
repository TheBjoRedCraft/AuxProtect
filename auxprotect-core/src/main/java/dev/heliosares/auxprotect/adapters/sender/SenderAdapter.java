package dev.heliosares.auxprotect.adapters.sender;

import dev.heliosares.auxprotect.core.IAuxProtect;
import dev.heliosares.auxprotect.core.Language;
import dev.heliosares.auxprotect.core.PlatformType;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public abstract class SenderAdapter<S, P extends IAuxProtect> {

  protected final S sender;
  protected final P plugin;

  protected SenderAdapter(S sender, P plugin) {
    this.sender = sender;
    this.plugin = plugin;
  }

  public final S getSender() {
    return sender;
  }

  public final P getPlugin() {
    return plugin;
  }

  public abstract String getName();

  public abstract UUID getUniqueId();

  public final PlatformType getPlatform() {
    return getPlugin().getPlatform();
  }

  public void sendLang(Language.L lang, Object... format) {
    sendMessageRaw(lang.translate(format));
  }

  public abstract void sendMessage(Component message);

  public final void sendMessageRaw(String message) {
    sendMessageRaw_(Language.convert(message));
  }

  protected abstract void sendMessageRaw_(String message);

  public abstract boolean hasPermission(String node);

  public abstract void executeCommand(String command);

  public abstract void teleport(double x, double y, double z, String world);

  public abstract void teleport(double x, double y, double z, String world, float pitch, float yaw);

  public abstract boolean isConsole();
}
