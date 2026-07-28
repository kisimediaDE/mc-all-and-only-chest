# Release-Validierung 0.1.0-beta.1

Stand: 28. Juli 2026

## Testumgebung

- macOS 26.5.2 auf Apple Silicon
- Java 25.0.4
- Paper 26.2 Build 65
- All and Only Chests 0.1.0-beta.1
- geprüfter Commit: `62ad3f4`
- JAR-SHA-256:
  `6ef86375d8fd4f095fe82718225e696e2ba2a20d3a2b3c3e0d94d4a43e955ab5`

Beide Tests liefen in eigenen temporären Serververzeichnissen. Die regulären
Testserver und ihre Spielstände wurden weder gestartet noch verändert.

## Frische Installation ohne Datenbank

Ausgangszustand:

- Paper-JAR
- akzeptierte EULA und minimale Serverkonfiguration
- ausschließlich die Plugin-JAR im Pluginverzeichnis
- kein `AllAndOnlyChests`-Datenordner
- keine SQLite-Datenbank

Ergebnis:

- Paper erkennt und lädt `AllAndOnlyChests v0.1.0-beta.1`.
- Das Plugin wird ohne Warnung oder Ausnahme aktiviert.
- Der Datenordner und `data/challenge.db` werden selbstständig erzeugt.
- Alle fünf erwarteten Tabellen sind vorhanden:
  `challenge_state`, `found_structure_goals`, `completed_structures`,
  `visited_structure_sources` und `placed_blocks`.
- Ziele, Abschlüsse, Quellen und platzierte Blöcke starten jeweils bei `0`.
- `PRAGMA integrity_check` liefert `ok`.
- Der Server erreicht `Done` und fährt mit `stop` sauber herunter.

## Upgrade mit bestehender Datenbank

Als Upgrade-Basis diente eine Kopie der Sicherung
`run/backups/end-of-day-2026-07-27/challenge.db`, die zuvor mit der
Snapshot-JAR erstellt und vollständig geprüft worden war.

Zustand vor dem Upgrade:

| Wert | Inhalt |
| --- | ---: |
| `challenge_won` | `true` |
| `opened_sources` | `3` |
| gefundene Ziele | `542` |
| abgeschlossene Strukturen | `18` |
| gespeicherte Quellen | `1` |
| platzierte Blöcke | `64` |

Ergebnis mit der Beta-JAR:

- Das Plugin lädt die vorhandene Datenbank ohne SQL- oder Pluginfehler.
- Im Startprotokoll werden die `64` gespeicherten platzierten Blöcke erkannt.
- Alle oben aufgeführten Zustände und Zähler bleiben unverändert.
- `PRAGMA integrity_check` liefert weiterhin `ok`.
- Der Datenbank-SHA-256 bleibt vor und nach Start und Shutdown identisch:
  `0bdc30c5cefefeba6137d08c5d526747f34975f61d27cc30feccbc9a9e4c7281`.
- Der Server erreicht `Done` und fährt mit `stop` sauber herunter.

## Noch offen

Der reale Windows-Test mit Java 25, `gradlew.bat`, Windows-Startskript,
SQLite-Treiber und Paper-Start bleibt eine getrennte Plattformprüfung.
