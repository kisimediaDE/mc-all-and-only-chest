# All and Only Chests – Arbeitsliste

Stand: 23. Juli 2026

## Nächster Schritt

- [ ] Prüfungskammer vollständig testen:
  - [ ] normale Trial Spawner
  - [ ] ominöse Trial Spawner
  - [ ] normale Vaults
  - [ ] ominöse Vaults
  - [ ] Trial Keys und Ominous Trial Keys
  - [ ] Tränke und Effektpfeile korrekt unterscheiden
  - [ ] verzauberte Diamantaxt korrekt erkennen
  - [ ] eindeutigen Quellenzähler für Vaults und Spawner prüfen
  - [ ] regulären Abschluss aller 64 Ziele prüfen

## Kritische Gameplay-Punkte

- [ ] Bastionsruine vollständig mit dem Original und Minecraft 26.1/26.2
      abgleichen.
  - [ ] normale und verzauberte Gegenstandsvarianten getrennt abbilden
  - [ ] Diamantspeer und Eisenkette aus 26.2 prüfen
  - [ ] endgültige Zielanzahl bestimmen und absichern
- [ ] Loot-Funktionen aller übrigen Struktur-Tabellen semantisch prüfen.
  - [ ] verzauberte Bücher
  - [ ] verzauberte Ausrüstung
  - [ ] Tränke und Effektpfeile
  - [ ] besondere Komponenten und Itemzustände
- [ ] Indirekte Itemquellen prüfen und entsprechend der Challenge sperren.
  - [ ] Angeln
  - [ ] Item Frames
  - [ ] Gemälde und andere Hanging Entities
  - [ ] Blattverfall
  - [ ] von lebenden Entities fallengelassene Gegenstände
  - [ ] natürliche gemeißelte Bücherregale
  - [ ] natürliche Crafter
  - [ ] Scheren
  - [ ] Bürsten und Archäologie

## Container- und Persistenz-Edge-Cases

- [ ] Doppelkisten als genau eine besuchte Kiste testen.
- [ ] Kistenloren testen.
- [ ] Fässer testen.
- [ ] bereits generierte, geöffnete oder geleerte Strukturkisten testen.
- [ ] erkannte Strukturkisten nach Serverneustart testen.
- [ ] Container nach Entladen und erneutem Laden des Chunks testen.
- [ ] natürliche leere Container testen.
- [ ] Abbau einer gefüllten Strukturkiste auf mögliche Drops prüfen.
- [ ] Hopper und Hopper-Loren mit allen Containerarten testen.
- [ ] Doppelkiste aus einer natürlichen und einer platzierten Hälfte testen.
- [ ] eindeutigen Kistenzähler nach Serverneustart prüfen.

## Fehlende Challenge-Funktionen

- [ ] Gesamtsieg nach Abschluss aller 18 Kategorien implementieren.
  - [ ] dauerhaften gewonnenen Status speichern
  - [ ] Broadcast
  - [ ] Titel oder Toast
  - [ ] Sound
- [ ] `/structures` implementieren.
  - [ ] Gesamtfortschritt anzeigen
  - [ ] aktive Struktur anzeigen
  - [ ] abgeschlossene Strukturen auflisten
  - [ ] offene Strukturen auflisten
- [ ] Verhalten abgeschlossener Strukturen vollständig testen.
  - [ ] Container bleiben gesperrt
  - [ ] GUI-Details bleiben einsehbar
  - [ ] Zustand bleibt nach Neustart erhalten
  - [ ] nächste Struktur kann ausgewählt werden
  - [ ] Abschluss der letzten Struktur funktioniert

## Struktur-Testmatrix

Das Iglu wurde als Smoke-Test geprüft, aber noch nicht regulär bis 8/8
durchgespielt. Beim Dorf wurde die Sperre einer nicht aktiven Struktur geprüft.

- [ ] Antike Stadt
- [ ] Vergrabener Schatz
- [ ] Wüstenpyramide
- [ ] Endsiedlung
- [ ] Netherfestung
- [ ] Iglu vollständig
- [ ] Dschungelpyramide
- [ ] Ozeanruine
- [ ] Plünderer-Außenposten
- [ ] Ruinenportal
- [ ] Schiffswrack
- [ ] Festung
- [ ] Minenstollen
- [ ] Dorf mit unterschiedlichen Berufen und Biomen
- [ ] Waldanwesen
- [ ] Verlies
- [ ] Bastionsruine
- [ ] Prüfungskammern

## Kompatibilität und Qualität

- [ ] echten Start- und Gameplay-Test mit Paper 26.1/26.1.2 durchführen.
- [ ] Windows-Test durchführen.
  - [ ] Java 25
  - [ ] Gradle-Wrapper und Build
  - [ ] Windows-Startskript
  - [ ] SQLite-Treiber
  - [ ] Paper-Start und Plugin-Laden
- [ ] automatisierte Tests ergänzen.
  - [ ] SQLite-Persistenz
  - [ ] Auswahl- und Abschlusslogik
  - [ ] Reset
  - [ ] eindeutige Quellenzählung
  - [ ] Item-Matcher
  - [ ] Kataloggrößen für 26.1 und 26.2
  - [ ] Trial- und Bastion-Sonderfälle
- [ ] Sidebar auf die aktuelle Component-API umstellen.
- [ ] README vervollständigen.
  - [ ] vollständige Command-Liste
  - [ ] Installation für macOS und Windows
  - [ ] Ablauf nach einem Hardcore-Tod
  - [ ] Datenbank-, Backup- und Reset-Hinweise
  - [ ] `structure-goals.yml` erklären
  - [ ] Kompatibilitätsstatus dokumentieren
- [ ] Release vorbereiten.
  - [ ] Änderungen bewusst committen
  - [ ] Snapshot-Version ersetzen
  - [ ] frische Installation ohne Datenbank testen
  - [ ] Upgrade mit bestehender Datenbank testen
  - [ ] finales Release-JAR und Prüfsumme erzeugen

## Optionale spätere Verbesserungen

- [ ] BossBar als Alternative zur Sidebar anbieten.
- [ ] optionales Resource-Pack-HUD prüfen.
- [ ] Texte und Farben konfigurierbar machen.
- [ ] Admin-Befehl zum temporären Umschalten normaler Drops prüfen.
- [ ] Fortschritts-Backup oder Export anbieten.
- [ ] Mehrspielerbetrieb mit globalem oder getrenntem Fortschritt definieren.

## Festgehaltene Entscheidungen

- Die verbindliche Strukturmenge entspricht den 18 Kategorien des
  Original-Plugins.
- Ozeanruinen und Verliese bleiben enthalten. Ozean-Monument und Hexenhütte
  aus `message.txt` ersetzen diese Kategorien nicht.
- Während einer aktiven Kategorie kann keine andere ausgewählt werden.
- Funde werden wie im Original beim erlaubten Öffnen beziehungsweise bei der
  Loot-Ausgabe erkannt, nicht erst beim Aufheben.
- Vaults und Trial Spawner zählen als legale Prüfungskammer-Quellen.
- Besuche werden pro eindeutiger Loot-Quelle gezählt; erneutes Öffnen derselben
  Kiste erhöht den Zähler nicht.
- Die Sidebar bleibt vorerst unverändert. Eine BossBar oder ein Resource Pack
  ist nur eine optionale spätere Erweiterung.
