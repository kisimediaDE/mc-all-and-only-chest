# All and Only Chests – Arbeitsliste

Stand: 24. Juli 2026

## Aktueller Wiedereinstieg

- Der Paper-Testserver wurde zum Tagesabschluss sauber gestoppt.
- Gespeicherter Spielstand: `6/18`, Antike Stadt aktiv, `0/33` Items,
  `0` Kisten/Quellen, kein Gesamtsieg gesetzt.
- Abgeschlossen: Endsiedlung, Iglu, Dschungelpyramide, Festung,
  Bastionsruine und Prüfungskammern.
- Tagesabschluss-Sicherung:
  `run/backups/end-of-day-2026-07-24/`
  - `challenge.db`: geprüfte SQLite-Datenbank (`PRAGMA integrity_check = ok`)
  - `AllAndOnlyChests-0.1.0-SNAPSHOT.jar`: aktuell getestetes Plugin
- Zuletzt fertiggestellt und getestet:
  - Gesamtsieg bei `18/18`, einmalige Meldung und Persistenz nach Neustart
  - `/structurecomplete all`
  - `/structurereset <Kategorie> confirm`
  - Endertruhen als persönliche Lagerung ohne Strukturfortschritt
  - natürliche und selbst platzierte Endertruhen jeweils mit und ohne
    Silk Touch
- Empfohlener nächster Arbeitspunkt: indirekte Itemquellen systematisch
  prüfen und sperren, beginnend mit Angeln, Item Frames und Gemälden.
- Serverstart: `./scripts/start-test-server.sh`

## Offene Prüfungskammer-Nachtests

- [ ] Prüfungskammer vollständig testen:
  - [x] normale Strukturkiste: Pfeil und Holzaxt erkannt, Quelle einmal gezählt
  - [x] normaler Trial Spawner: drei Brote als ein Ziel erkannt und Quelle gezählt
  - [x] ominöser Trial Spawner: Ofenkartoffeln erkannt und Quelle gezählt
  - [x] normaler Vault: Trial Key verbraucht, Armbrust und Eisenbarren erkannt
  - [x] ominöser Vault: mit kontrolliertem Schlüssel geöffnet; Windkugel und
        Fluss-Rüstungsbesatz erkannt
  - [x] Trial Key natürlich aus normalen Spawnern erhalten; zwei Keys zählen als ein Ziel
  - [ ] Ominous Trial Key natürlich erhalten
  - [x] Regeneration, Stärke und Schnelligkeit als getrennte Trankziele erkannt
  - [x] Vergiftung und Langsamkeit als getrennte Effektpfeilziele erkannt
  - [x] verzauberte und unverzauberte Diamantaxt als getrennte Ziele erkannt
  - [x] alle Trank-, Effektpfeil- und Axt-Sonderziele in der GUI grün dargestellt
  - [ ] eindeutigen Quellenzähler für Vaults und Spawner prüfen
  - [ ] regulären Abschluss aller 64 Ziele prüfen
  - [x] kontrollierten Testabschluss auf 64/64 mit ausgeblendeter Sidebar und
        abgeschlossener GUI geprüft
  - [x] aktiven Stand mit 22/64 Items und 11 Quellen nach Neustart wiederhergestellt

## Kritische Gameplay-Punkte

- [x] Bastionsruine vollständig mit dem Original und Minecraft 26.1/26.2
      abgleichen.
  - [x] normale und verzauberte Gegenstandsvarianten getrennt abgebildet
  - [x] Diamantspeer und Eisenkette in 26.1, 26.1.1, 26.1.2 und 26.2 geprüft
  - [x] 66 Ziele bestimmt und beim Pluginstart abgesichert
  - [x] Bastionsziele und Sondervarianten im Spiel getestet
    - [x] natürliche Kiste erkannt; sechs Grundziele und eine Quelle verbucht
    - [x] normale Varianten von Goldhelm und Goldbeinschutz erkannt
    - [x] verzauberten Goldhelm getrennt von normalem Goldhelm erkannt
    - [x] Diamantspeer normal und verzaubert getrennt erkannt
    - [x] unverzauberte Diamantspitzhacke ignoriert und verzauberte erkannt
- [x] Loot-Funktionen aller Struktur-Tabellen für 26.1.2 und 26.2 semantisch
      geprüft und durch ein reproduzierbares Audit abgesichert.
  - [x] verzauberte Bücher
  - [x] verzauberte Ausrüstung
  - [x] Tränke und Effektpfeile
  - [x] besondere Komponenten und Itemzustände
  - [x] Endsiedlung: fehlende Diamantspitzhacke ergänzt
  - [x] Dschungelpyramide: Buch zu verzaubertem Buch korrigiert
  - [x] Festung: fehlendes verzaubertes Buch ergänzt
- [x] Indirekte Itemquellen prüfen und entsprechend der Challenge sperren.
  - [x] Angeln
    - [x] erfolgreicher Fang: kein Item, XP bleibt erhalten und Angel verliert
          normal Haltbarkeit
    - [x] gehaktes Lebewesen wird nur herangezogen und nicht gelöscht
  - [x] Item Frames
    - [x] natürlicher Elytra-Rahmen: Rechts- und Linksklick werden mit
          verständlicher Meldung gesperrt
    - [x] selbst platzierter normaler Item Frame funktioniert vollständig
    - [x] selbst platzierter Glow Item Frame funktioniert vollständig
    - [x] Player-Placed-Markierung bleibt nach Serverneustart erhalten
    - [x] natürlicher Rahmen bleibt bei Supportverlust gesperrt; Rahmen und
          Inhalt bleiben ohne Drop erhalten
  - [x] Gemälde und andere Hanging Entities
    - [x] per Command erzeugtes, nicht markiertes Gemälde bleibt geschützt
    - [x] selbst platziertes Gemälde droppt normal
    - [x] Player-Placed-Markierung eines Gemäldes überlebt einen Neustart
    - [x] selbst erzeugter Leinenknoten funktioniert normal
  - [x] Blattverfall
    - [x] natürliche Blätter technisch auf Verfall ohne Drops umgestellt
    - [x] im Spiel geprüft: Blätter verschwinden, aber Setzlinge, Stöcke und
          Äpfel werden nicht fallengelassen
  - [x] von lebenden Entities fallengelassene Gegenstände
    - [x] natürlich gelegtes Hühnerei wird verhindert
    - [x] vom Spieler fallengelassenes Ei bleibt normal aufhebbar
  - [x] natürliche gemeißelte Bücherregale
    - [x] Einlegen und Entnehmen werden mit verständlicher Meldung gesperrt
    - [x] Abbau ohne und mit Behutsamkeit erzeugt keinen Drop
    - [x] selbst platziertes Regal kann Bücher einlagern und entnehmen
    - [x] selbst platziertes Regal folgt beim Abbau dem Vanilla-Verhalten
  - [x] natürliche Crafter
    - [x] nicht markierter Crafter kann nicht geöffnet werden
    - [x] Redstone-Crafting bleibt ohne Output und verbraucht keine Zutaten
    - [x] selbst platzierter Crafter verarbeitet Zutaten normal
  - [x] Scheren
    - [x] Schaf wird geschoren, Schere verliert Haltbarkeit, keine Wolle
    - [x] Mooshroom wird zur Kuh, Schere verliert Haltbarkeit, keine Pilze
  - [x] Bürsten und Archäologie
    - [x] natürlicher verdächtiger Block wird vollständig gebürstet
    - [x] Bürstenhaltbarkeit sinkt und der Block wird zu normalem Sand
    - [x] sichtbarer Archäologie-Inhalt erzeugt keinen aufhebbaren Drop
  - [x] natürliche verzierte Krüge in Prüfungskammern: kein Krug-, Scherben-
        oder Inhaltsdrop und kein Strukturfortschritt

## Container- und Persistenz-Edge-Cases

- [ ] Doppelkisten als genau eine besuchte Kiste testen.
- [ ] Kistenloren testen.
- [x] natürliches Prüfungskammer-Fass erkannt und genau einmal gezählt.
- [ ] bereits generierte, geöffnete oder geleerte Strukturkisten testen.
- [x] erkanntes Prüfungskammer-Fass nach Serverneustart erneut geöffnet.
- [ ] Container nach Entladen und erneutem Laden des Chunks testen.
- [ ] natürliche leere Container testen.
- [x] Endertruhe bleibt mit Struktur-Items und nach Strukturwechsel frei zugänglich.
- [x] Endertruhen-Abbau:
  - [x] natürlich generiert ohne Silk Touch: kein Drop
  - [x] natürlich generiert mit Silk Touch: kein Drop
  - [x] selbst platziert ohne Silk Touch: acht Obsidian
  - [x] selbst platziert mit Silk Touch: Endertruhe
- [ ] Abbau einer gefüllten Strukturkiste auf mögliche Drops prüfen.
- [ ] Hopper und Hopper-Loren mit allen Containerarten testen.
- [ ] natürliche Prüfungskammer-Dispenser:
  - [ ] manuelles Öffnen bleibt gesperrt
  - [ ] ausgelöste Fallen verändern weder Items noch Kistenzähler
- [ ] natürlicher leerer Entsorgungs-Hopper in Prüfungskammern bleibt gesperrt
      und zählt nicht als Loot-Quelle.
- [ ] selbst platzierte Dispenser und Hopper bleiben normal benutzbar.
- [x] natürliche Dispenser-Fallen schießen weiterhin, frisch abgefeuerte
      Pfeile bleiben aber in Wand oder Boden stecken und sind nicht aufhebbar.
- [x] direktes Öffnen eines natürlichen Dschungelpyramiden-Dispensers bleibt
      gesperrt und verändert weder Item- noch Quellenfortschritt.
- [ ] Doppelkiste aus einer natürlichen und einer platzierten Hälfte testen.
- [x] eindeutigen Kistenzähler nach Serverneustart geprüft.

## Fehlende Challenge-Funktionen

- [x] Gesamtsieg nach Abschluss aller 18 Kategorien implementieren.
  - [x] dauerhaften gewonnenen Status speichern
  - [x] Broadcast
  - [x] Titel und Untertitel
  - [x] Sound
  - [x] `/structures` zeigt nach dem Sieg den Abschlussstatus
  - [x] kontrollierten 18/18-Übergang und Einmaligkeit nach Neustart prüfen
- [x] `/structures` implementiert und im Spiel geprüft.
  - [x] Gesamtfortschritt anzeigen
  - [x] aktive Struktur anzeigen
  - [x] abgeschlossene Strukturen auflisten
  - [x] offene Strukturen auflisten
- [x] Admin-Testbefehle im Spiel prüfen.
  - [x] `/structurecomplete all` schließt alle offenen Strukturen ab
  - [x] `/structurereset <Kategorie> confirm` setzt nur eine Struktur zurück
  - [x] gezielter Reset öffnet einen gesetzten Gesamtsieg wieder
- [ ] Verhalten abgeschlossener Strukturen vollständig testen.
  - [x] Container bleiben gesperrt
  - [x] GUI-Details bleiben einsehbar
  - [x] Zustand von Iglu und Prüfungskammern bleibt nach Neustart erhalten
  - [x] nächste Struktur kann nach einem Abschluss ausgewählt werden
  - [x] Abschluss der letzten Struktur funktioniert

## Struktur-Testmatrix

Das Iglu wurde mit vier eindeutigen Kellertruhen bis 7/8 über natürliches
Loot geprüft. Der letzte Smaragd wurde kontrolliert in eine bereits erkannte
Kellertruhe gelegt. Fundmeldung, 8/8-Abschluss und Ausblenden der Sidebar
funktionierten. Beim Dorf wurde die Sperre einer nicht aktiven Struktur geprüft.

- [ ] Antike Stadt
- [ ] Vergrabener Schatz
- [ ] Wüstenpyramide
- [x] Endsiedlung: drei natürliche Kisten, ergänzte Diamantspitzhacke und
      kontrollierter 26/26-Abschluss
- [ ] Netherfestung
- [x] Iglu: Loot-Fortschritt und kontrollierter 8/8-Abschluss
- [x] Dschungelpyramide: natürlicher Loot aus beiden Kisten, verzaubertes
      Buch, Dispenser-Regeln und kontrollierter 14/14-Abschluss
- [ ] Ozeanruine
- [ ] Plünderer-Außenposten
- [ ] Ruinenportal
- [ ] Schiffswrack
- [x] Festung: beide Bibliothekskisten, gewöhnliche Kiste, normales und
      verzaubertes Buch getrennt sowie kontrollierter 27/27-Abschluss
- [ ] Minenstollen
- [ ] Dorf mit unterschiedlichen Berufen und Biomen
- [ ] Waldanwesen
- [ ] Verlies
- [x] Bastionsruine: 66er-Katalog, Sondervarianten, GUI und kontrollierter
      Abschluss
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
  - [x] Kataloggrößen für 26.1 und 26.2 beim Pluginstart abgesichert
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
