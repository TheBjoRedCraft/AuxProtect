package dev.heliosares.auxprotect.towny;

import dev.heliosares.auxprotect.AuxProtectPaper;
import dev.heliosares.auxprotect.database.TownyHook;
import dev.heliosares.auxprotect.database.SpigotSQLManager;
import dev.kshl.kshlib.exceptions.BusyException;

import javax.annotation.Nullable;
import java.sql.SQLException;

/**
 * Real implementation of {@link TownyHook} that delegates to {@link TownyManager}.
 * This class is intentionally kept in the {@code towny} package so that all
 * Towny-specific imports are isolated here and never loaded when Towny is absent.
 *
 * <p>This class is instantiated via reflection by {@link SpigotSQLManager} only when Towny
 * is present and its required classes are available.</p>
 */
public class TownyHookImpl implements TownyHook {

  private final TownyManager townyManager;

  public TownyHookImpl(AuxProtectPaper plugin, SpigotSQLManager sql) throws ClassNotFoundException {
    this.townyManager = new TownyManager(plugin, sql);
  }

  @Override
  public void initPostInit() {
    townyManager.init();
  }

  @Override
  public void run() {
    townyManager.run();
  }

  @Override
  public void cleanup() {
    townyManager.cleanup();
  }

  @Override
  @Nullable
  public String getNameFromID(int uid, boolean wait) throws SQLException, BusyException {
    return townyManager.getNameFromID(uid, wait);
  }

  /**
   * Returns the underlying {@link TownyManager} for use by Towny-specific code
   * (e.g. {@link TownyListener}).
   */
  public TownyManager getTownyManager() {
    return townyManager;
  }
}
