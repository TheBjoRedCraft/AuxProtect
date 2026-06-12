package dev.heliosares.auxprotect.database;

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

  public int getIDOrInsert(Connection conn, String value) throws SQLException {
    String key = value.toLowerCase();

    Integer cached = valueToId.get(key);
    if (cached != null) {
      return cached;
    }

    try {
      try (PreparedStatement stmt = conn.prepareStatement(
          "INSERT INTO " + table + " (value) VALUES (?)")) {
        stmt.setString(1, value);
        stmt.executeUpdate();
      }
    } catch (SQLException ex) {
      if (!isUniqueViolation(ex)) {
        throw ex;
      }
    }

    try (PreparedStatement stmt = conn.prepareStatement(
        "SELECT id FROM " + table + " WHERE value=?")) {
      stmt.setString(1, value);

      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new SQLException("Failed to resolve id for value " + value);
        }

        int id = rs.getInt(1);

        valueToId.put(key, id);
        idToValue.put(id, value);

        return id;
      }
    }
  }

  private boolean isUniqueViolation(SQLException e) {
    return e.getMessage() != null
        && e.getMessage().contains("UNIQUE");
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