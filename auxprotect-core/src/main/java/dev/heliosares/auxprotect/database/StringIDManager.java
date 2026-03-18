package dev.heliosares.auxprotect.database;

import jakarta.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class StringIDManager {

  private final String table;
  private final Map<String, Integer> valueToId = new ConcurrentHashMap<>();
  private final Map<Integer, String> idToValue = new ConcurrentHashMap<>();
  private boolean initDone = false;

  public StringIDManager(String table) {
    this.table = table;
  }

  public synchronized void init(Connection conn) throws SQLException {
    if (initDone) {
      throw new IllegalStateException("Already initialized");
    }
    try (PreparedStatement stmt = conn.prepareStatement(
        "CREATE TABLE IF NOT EXISTS " + table + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "value VARCHAR(255) UNIQUE NOT NULL" +
            ")")) {
      stmt.execute();
    }
    try (PreparedStatement stmt = conn.prepareStatement("SELECT id, value FROM " + table)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int id = rs.getInt("id");
          String value = rs.getString("value");
          idToValue.put(id, value);
          valueToId.put(value.toLowerCase(), id);
        }
      }
    }
    initDone = true;
  }

  public synchronized int getIDOrInsert(@Nonnull Connection conn, @Nonnull String value)
      throws SQLException {
    if (!initDone) {
      throw new IllegalStateException("Not initialized");
    }
    String key = value.toLowerCase();
    if (valueToId.containsKey(key)) {
      return valueToId.get(key);
    }
    int generatedId;
    try (PreparedStatement stmt = conn.prepareStatement(
        "INSERT INTO " + table + " (value) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, value);
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          generatedId = rs.getInt(1);
        } else {
          throw new SQLException("Failed to get generated ID");
        }
      }
    }
    valueToId.put(key, generatedId);
    idToValue.put(generatedId, value);
    return generatedId;
  }

  public Map<String, Integer> getOrInsertAll(Connection connection, Collection<String> values)
      throws SQLException {
    Map<String, Integer> out = new HashMap<>();
    for (String v : values) {
      int id = getIDOrInsert(connection, v);
      out.put(v, id);
    }
    return out;
  }

  public synchronized Optional<String> getValue(int id) {
    return Optional.ofNullable(idToValue.get(id));
  }

  public synchronized Optional<Integer> getID(String value) {
    return Optional.ofNullable(valueToId.get(value.toLowerCase()));
  }

  public synchronized void clearCache() {
    valueToId.clear();
    idToValue.clear();
  }
}