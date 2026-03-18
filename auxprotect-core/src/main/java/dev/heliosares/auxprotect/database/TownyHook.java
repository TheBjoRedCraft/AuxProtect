package dev.heliosares.auxprotect.database;

import dev.kshl.kshlib.exceptions.BusyException;

import javax.annotation.Nullable;
import java.sql.SQLException;

/**
 * Interface for Towny integration, allowing AuxProtect to run without Towny installed.
 * Implementations must not be referenced directly by core classes; instead, the active
 * hook is obtained via {@link SpigotSQLManager#getTownyHook()}.
 */
public interface TownyHook {

  /**
   * Called after the database has been fully initialized.
   * Implementations may use this to sync town/nation names.
   */
  void initPostInit();

  /**
   * Called periodically to log town/nation bank balance changes.
   */
  void run();

  /**
   * Called to clean up internal caches.
   */
  void cleanup();

  /**
   * Resolves a database UID to a town or nation name.
   *
   * @param uid  the internal UID
   * @param wait whether to wait for the result
   * @return the name, or {@code null} if not found
   */
  @Nullable
  String getNameFromID(int uid, boolean wait) throws SQLException, BusyException;
}
