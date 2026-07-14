# RandomTP

Paper-Plugin für Minecraft **1.21.11** (Java 21).

Ein Admin führt `/start` aus – alle Online-Spieler werden **einmalig** an
zufällige, sichere Positionen teleportiert. Kein Scheduler, kein Intervall.

## Befehl

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/start` | Teleportiert sofort alle Online-Spieler an zufällige Positionen | `randomtp.admin` (Standard: nur Operatoren) |

## Funktionsweise

Beim Ausführen von `/start` wird für jeden Spieler aus `Bukkit.getOnlinePlayers()`:

- eine zufällige X/Z-Koordinate im Radius (Standard ±2000 Blöcke) um den
  Weltspawn gewürfelt – Positionen außerhalb der World Border werden verworfen,
- die sichere Y-Höhe per `World#getHighestBlockYAt` ermittelt und der Spieler
  auf den obersten festen Block gesetzt,
- Lava, Feuer, Magma, Kakteen, Pulverschnee und Void vermieden – bei
  ungeeigneter Position wird neu gewürfelt (max. 30 Versuche pro Spieler),
- per `player.teleportAsync(location)` teleportiert,
- optional eine kurze Nachricht angezeigt („Du wurdest teleportiert!“).

Der Admin bekommt eine Rückmeldung, wie viele Spieler teleportiert wurden.

## Konfiguration (`plugins/RandomTP/config.yml`)

```yaml
# Radius in Blöcken um den Weltspawn (±X/±Z)
radius: 2000

# Ob teleportierte Spieler eine Nachricht erhalten (true/false)
send-message: true
```

## Selbst bauen

Voraussetzungen: Java 21 und Maven.

```bash
mvn package
```

Die fertige Jar liegt danach unter `target/RandomTP-1.0.0.jar`.

Alternativ baut der GitHub-Actions-Workflow (`.github/workflows/build.yml`)
bei jedem Push automatisch und lädt die Jar als Artifact **RandomTP** hoch
(Repo → Actions → Build-Run → Artifacts).

## Installation auf dem Server

1. `RandomTP-1.0.0.jar` in den Ordner `plugins/` des Paper-Servers legen.
2. Server neu starten (oder das Plugin per Plugin-Manager laden).
3. Als Operator `/start` ausführen – fertig.
