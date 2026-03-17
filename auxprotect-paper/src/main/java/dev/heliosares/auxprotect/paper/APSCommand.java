package dev.heliosares.auxprotect.paper;

import dev.heliosares.auxprotect.AuxProtectPaper;
import dev.heliosares.auxprotect.adapters.sender.SpigotSenderAdapter;
import dev.heliosares.auxprotect.core.commands.APCommand;
import dev.heliosares.auxprotect.paper.commands.InvCommand;
import dev.heliosares.auxprotect.paper.commands.InventoryCommand;
import dev.heliosares.auxprotect.paper.commands.MoneyCommand;
import dev.heliosares.auxprotect.paper.commands.SaveInvCommand;
import dev.heliosares.auxprotect.paper.commands.SpigotLookupCommand;
import dev.heliosares.auxprotect.paper.commands.TpCommand;
import java.util.List;
import javax.annotation.Nonnull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class APSCommand extends Command {

  private final AuxProtectPaper plugin;
  private final APCommand<CommandSender, AuxProtectPaper, SpigotSenderAdapter> apcommand;

  public APSCommand(AuxProtectPaper plugin) {
    super(plugin.getCommandPrefix(), "AuxProtect main command", "/" + plugin.getCommandPrefix(),
        List.of(plugin.getCommandAlias()));
    this.plugin = plugin;
    this.apcommand = new APCommand<>(plugin, plugin.getCommandPrefix()) {
      {
        // TODO why are these warnings?
        add(new TpCommand(plugin).setTabComplete(false));
        add(new InvCommand(plugin).setTabComplete(false));
        add(new InventoryCommand(APSCommand.this.plugin));
        add(new MoneyCommand(plugin));
        add(new SaveInvCommand(plugin));
      }
    };
    apcommand.add(new SpigotLookupCommand(plugin)); // Done after to overwrite default LookupCommand
  }

  @Override
  public boolean execute(@Nonnull CommandSender sender, @Nonnull String label,
      @Nonnull String[] args) {
    apcommand.onCommand(new SpigotSenderAdapter(plugin, sender), label, args);
    return true;
  }

  @Nonnull
  @Override
  public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String alias,
      @Nonnull String[] args) {
    return apcommand.onTabComplete(new SpigotSenderAdapter(plugin, sender), alias, args);
  }

  protected APCommand<CommandSender, AuxProtectPaper, SpigotSenderAdapter> getAPCommand() {
    return apcommand;
  }
}
