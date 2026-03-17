package dev.heliosares.auxprotect.core;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public record ActivityRecord(@Nonnull List<Activity> activities, double andScore,
                             double distanceMoved) {

  public static ActivityRecord parse(String data) throws IllegalArgumentException {
      if (data == null) {
          return null;
      }

    if (data.matches("\\d+")) {
      throw new IllegalArgumentException("Legacy activity");
    }
    if (!data.matches("[^;+]*(\\+\\d+(\\.\\d+)?)?;(\\d+(\\.\\d)?)?")) {
      throw new IllegalArgumentException("Invalid activity string");
    }

    List<Activity> activities = new ArrayList<>();
      if (data.startsWith(";")) {
          data = " " + data;
      }
      if (data.endsWith(";")) {
          data += " ";
      }
    String[] parts = data.split(";");

    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid activity string format");
    }

    String[] scoreParts = parts[0].split("\\+");

    for (char c : scoreParts[0].trim().toCharArray()) {
      activities.add(Activity.getByChar(c));
    }

    double andScore = 0;
    if (scoreParts.length == 2) {
      andScore = Double.parseDouble(scoreParts[1]);
    }

    double distance = parts[1].isBlank() ? 0 : Double.parseDouble(parts[1].trim());

    return new ActivityRecord(activities, andScore, distance);
  }

  @Override
  public String toString() {
    StringBuilder activityString = new StringBuilder(getActivityString());

    activityString.append(";");
    final double moved = distanceMoved();
    if (moved > 1E-6) {
      if (moved >= 10) {
        activityString.append((int) Math.round(moved));
      } else {
        activityString.append(Math.round(moved * 10) / 10D);
      }
    }
    return activityString.toString();
  }

  public String getActivityString() {
    StringBuilder activityString = new StringBuilder();
    double and = 0;
    for (Activity a : activities()) {
      if (activityString.length() >= 60) {
        and += a.score;
      } else {
        activityString.append(a.character);
      }
    }
    if (and >= 0.5) {
      activityString.append("+").append((int) Math.round(and));
    }

    return activityString.toString();
  }

  public double countScore() {
    double score = Math.floor((distanceMoved()) / 10);

      if (distanceMoved() > 1E-6) {
          score++;
      }

    for (Activity activity : activities()) {
      score += activity.score;
    }

    score += andScore;

    return score;
  }

  public Component getHoverComponent() {
    TextComponent.Builder hover = Component.text();

    hover.append(Component.newline());
    hover.append(Component.newline());

    hover.append(Component.text("Activity: ", NamedTextColor.GRAY))
        .append(Component.text(getActivityString(), NamedTextColor.BLUE));

    if (andScore() > 1E-6) {
      hover.append(
          Component.text("... +" + (int) Math.round(andScore()) + " points", NamedTextColor.GRAY));
    }

    hover.append(Component.newline());

    for (Activity activity : new HashSet<>(activities())) {
      hover.append(
          Component.text("  ", NamedTextColor.GRAY)
              .append(Component.text(String.valueOf(activity.character), NamedTextColor.GRAY))
              .append(Component.text(" = ", NamedTextColor.GRAY))
              .append(Component.text(activity.toString().toLowerCase(), NamedTextColor.GRAY))
              .append(Component.text(" (" + activity.score + ")", NamedTextColor.GRAY))
      );
      hover.append(Component.newline());
    }

    hover.append(Component.text("Moved ", NamedTextColor.GRAY))
        .append(Component.text(distanceMoved(), NamedTextColor.BLUE))
        .append(Component.text(" Blocks", NamedTextColor.GRAY));

    return hover.build();
  }
}