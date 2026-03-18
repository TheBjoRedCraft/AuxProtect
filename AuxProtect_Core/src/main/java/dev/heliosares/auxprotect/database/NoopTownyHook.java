package dev.heliosares.auxprotect.database;

import javax.annotation.Nullable;

/**
 * No-op implementation of {@link TownyHook} used when Towny is not installed or not enabled.
 */
public class NoopTownyHook implements TownyHook {

    @Override
    public void initPostInit() {
    }

    @Override
    public void run() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    @Nullable
    public String getNameFromID(int uid, boolean wait) {
        return null;
    }
}
