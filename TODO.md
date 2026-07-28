# All and Only Chests – Arbeitsliste

Stand: 28. Juli 2026

## Automatisierte Tests

- [x] JUnit 5 und reproduzierbaren Testlauf in Gradle eingerichtet.
- [x] Auswahl einer Struktur und Sperre einer zweiten aktiven Struktur.
- [x] Fortschritt und aktive Struktur über SQLite-Neustart erhalten.
- [x] dieselbe Loot-Quelle wird nur einmal gezählt.
- [x] Abschluss einer Struktur entfernt die aktive Auswahl.
- [x] gezielter Strukturreset löscht nur Ziele und Quellen der Zielstruktur.
- [x] globaler Reset löscht Auswahl, Ziele, Quellen und Siegstatus.
- [x] Übergang der letzten von 18 Kategorien zum Gesamtsieg.
- [x] gezielter Reset nach Gesamtsieg öffnet die Challenge wieder.
- [x] Versionsfilter: Minenstollen hat unter 26.2 22 Ziele und unter 26.1
      ohne `music_disc_bounce` 21 Ziele.
- [x] doppelte Zielnamen in allen konfigurierten Kategorien ausgeschlossen.

## Offene Prüfungskammer-Nachtests

- [x] Prüfungskammer vollständig testen:
  - [x] normale Strukturkiste: Pfeil und Holzaxt erkannt, Quelle einmal gezählt
  - [x] normaler Trial Spawner: drei Brote als ein Ziel erkannt und Quelle gezählt
  - [x] ominöser Trial Spawner: Ofenkartoffeln erkannt und Quelle gezählt
  - [x] normaler Vault: Trial Key verbraucht, Armbrust und Eisenbarren erkannt
  - [x] ominöser Vault: mit kontrolliertem Schlüssel geöffnet; Windkugel und
        Fluss-Rüstungsbesatz erkannt
    - [x] natürlich erhaltener unheilvoller Prüfungsschlüssel öffnet einen
          unheilvollen Tresor; Diamantharnisch, Smaragd und
          Fluss-Bannervorlage erkannt, Quelle genau einmal gezählt
  - [x] Trial Key natürlich aus normalen Spawnern erhalten; zwei Keys zählen als ein Ziel
  - [x] Ominous Trial Key natürlich erhalten
    - [x] im Spiel als „Unheilvoller Prüfungsschlüssel“ erkannt
  - [x] Regeneration, Stärke und Schnelligkeit als getrennte Trankziele erkannt
  - [x] Vergiftung und Langsamkeit als getrennte Effektpfeilziele erkannt
  - [x] verzauberte und unverzauberte Diamantaxt als getrennte Ziele erkannt
  - [x] alle Trank-, Effektpfeil- und Axt-Sonderziele in der GUI grün dargestellt
  - [x] eindeutigen Quellenzähler für Vaults und Spawner prüfen
    - [x] zwei benachbarte Spawner erhöhen den Quellenzähler genau um zwei
  - [x] regulären Abschluss aller 64 Ziele nicht als separaten Testlauf
        erzwingen; alle Sonderquellen und Sonderziele wurden einzeln geprüft,
        der kontrollierte 64/64-Abschluss funktioniert
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

- [x] Doppelkisten als genau eine besuchte Kiste testen.
  - [x] Loot aus beiden Hälften wird gemeinsam erkannt
  - [x] beide Hälften zählen zusammen als genau eine Quelle
  - [x] Wiederöffnen erhöht den Quellenzähler nicht
  - [x] später ergänztes Ziel wird erkannt, ohne die Quelle erneut zu zählen
- [x] Kistenloren testen.
  - [x] Loot-Lore wird erkannt und als eindeutige Quelle gezählt
  - [x] Wiederöffnen erhöht den Quellenzähler nicht
  - [x] selbst gesetzte Kistenlore kann frei geöffnet werden
  - [x] Player-Placed-Markierung überlebt einen Serverneustart
  - [x] selbst gesetzte Lore droppt beim Abbau Lore und Inhalt
  - [x] nicht markierte Lore droppt weder Lore noch Inhalt
- [x] natürliches Prüfungskammer-Fass erkannt und genau einmal gezählt.
- [x] bereits generierte, geöffnete oder geleerte Strukturkisten testen.
  - [x] generierter Loot wird beim ersten Öffnen erkannt
  - [x] Wiederöffnen erzeugt weder Funde noch einen neuen Quellenbesuch
  - [x] vollständig geleerte Strukturkiste bleibt zugänglich und erkannt
- [x] erkanntes Prüfungskammer-Fass nach Serverneustart erneut geöffnet.
- [x] Container nach Entladen und erneutem Laden des Chunks testen.
  - [x] Kategorie- und Quellenmarkierung bleibt über mehrere Neustarts erhalten
- [x] natürliche leere Container testen.
  - [x] geleerte Doppelkiste öffnet ohne Fund und ohne erneute Quellenzählung
- [x] Endertruhe bleibt mit Struktur-Items und nach Strukturwechsel frei zugänglich.
- [x] Endertruhen-Abbau:
  - [x] natürlich generiert ohne Silk Touch: kein Drop
  - [x] natürlich generiert mit Silk Touch: kein Drop
  - [x] selbst platziert ohne Silk Touch: acht Obsidian
  - [x] selbst platziert mit Silk Touch: Endertruhe
- [x] Abbau einer gefüllten Strukturkiste auf mögliche Drops prüfen.
  - [x] nicht markierte gefüllte Kiste droppt weder Kiste noch Inhalt
  - [x] selbst platzierte gefüllte Kiste droppt Kiste und Inhalt normal
- [x] Hopper und Hopper-Loren mit allen Containerarten testen.
  - [x] nicht markierter Hopper bleibt geschlossen und transportiert nicht
  - [x] selbst platzierter Hopper öffnet und transportiert normal
  - [x] Hopper kann keine Items aus einer Strukturkiste absaugen
  - [x] selbst gesetzte Hopper-Lore öffnet und droppt Lore sowie Inhalt
  - [x] nicht markierte Hopper-Lore öffnet nicht und droppt weder Lore noch Inhalt
- [x] natürliche Prüfungskammer-Dispenser:
  - [x] manuelles Öffnen bleibt gesperrt
  - [x] natürlich generierte Dispenser besitzen einen eigenen Button und können
        unabhängig vom Prüfungs-Spawner ausgelöst werden
  - [x] Tränke wirken, Pfeile werden abgefeuert und bleiben unaufhebbar
  - [x] Feuerkugeln verursachen im Überlebensmodus Feuer- und Trefferschaden
  - [x] ausgelöste Fallen verändern weder Items noch Kistenzähler
- [x] natürlicher leerer Entsorgungs-Hopper in Prüfungskammern bleibt gesperrt
      und zählt nicht als Loot-Quelle.
- [x] selbst platzierte Dispenser bleiben normal benutzbar.
  - [x] öffnen, befüllen und auslösen funktioniert
  - [x] abgeschossene Pfeile bleiben aufhebbar
  - [x] Strukturfortschritt bleibt unverändert
- [x] selbst platzierte Hopper und Hopper-Loren bleiben normal benutzbar.
- [x] natürliche Dispenser-Fallen schießen weiterhin, frisch abgefeuerte
      Pfeile bleiben aber in Wand oder Boden stecken und sind nicht aufhebbar.
- [x] direktes Öffnen eines natürlichen Dschungelpyramiden-Dispensers bleibt
      gesperrt und verändert weder Item- noch Quellenfortschritt.
- [x] Doppelkiste aus einer natürlichen und einer platzierten Hälfte testen.
  - [x] Verbinden zählt die natürliche Hälfte nicht erneut
  - [x] Trennen und erneutes Öffnen behält denselben Quellenzähler
  - [x] die platzierte Hälfte wird nicht Bestandteil des natürlichen Quellenschlüssels
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
- [x] Verhalten abgeschlossener Strukturen vollständig testen.
  - [x] Container bleiben gesperrt
  - [x] GUI-Details bleiben einsehbar
  - [x] Zustand von Iglu und Prüfungskammern bleibt nach Neustart erhalten
  - [x] nächste Struktur kann nach einem Abschluss ausgewählt werden
  - [x] Abschluss der letzten Struktur funktioniert

## Struktur-Testmatrix

Teststrategie:

- Genau ein vollständiger natürlicher End-to-End-Durchlauf ist ausreichend:
  Vergrabener Schatz mit 17/17 Zielen aus 104 natürlichen Truhen.
- Weitere Strukturen benötigen keinen vollständigen Loot-Grind. Dort genügen
  repräsentative natürliche Container, korrekte Zielerkennung, eindeutige
  Quellenzählung, Wiederöffnen und die jeweiligen Struktur-Sonderfälle.
- Seltene Ziele und vollständige Listen werden zusätzlich über die
  abgeglichenen 26.1/26.2-Loot-Tabellen und kontrollierte Tests abgesichert.

Das Iglu wurde mit vier eindeutigen Kellertruhen bis 7/8 über natürliches
Loot geprüft. Der letzte Smaragd wurde kontrolliert in eine bereits erkannte
Kellertruhe gelegt. Fundmeldung, 8/8-Abschluss und Ausblenden der Sidebar
funktionierten. Beim Dorf wurde die Sperre einer nicht aktiven Struktur geprüft.

- [x] Antike Stadt
  - [x] mehrere natürliche Truhen und Doppelkisten mit echtem Loot erkannt
  - [x] Wiederöffnen und Neustart bleiben ohne erneute Quellenzählung
  - [x] natürliche und selbst platzierte Endertruhen-Regeln geprüft
  - [x] natürliche Hopper bleiben gesperrt und können Strukturloot nicht
        unzulässig transportieren
  - [x] gefüllte natürliche Container und gemischte Doppelkisten geprüft
- [x] Vergrabener Schatz
  - [x] als überschaubaren End-to-End-Test vollständig über natürliche
        Schatztruhen abschließen
  - [x] 17/17 Ziele aus 104 natürlichen Schatztruhen erkannt
  - [x] letzte natürliche Beute löst Abschlussmeldung und Strukturwechsel aus
- [x] Wüstenpyramide
  - [x] vier natürliche Truhen als vier eindeutige Quellen gezählt
  - [x] Wiederöffnen zählt eine Truhe nicht erneut
  - [x] repräsentatives Loot korrekt erkannt
  - [x] natürliche Druckplatten-/TNT-Falle funktioniert und erzeugt keine
        Block-, Truhen- oder Inhaltsdrops
    - [x] kontrolliert mit zuvor leerem Spielerinventar und gefüllter,
          natürlich generierter Truhe wiederholt
- [x] Endsiedlung: drei natürliche Kisten, ergänzte Diamantspitzhacke und
      kontrollierter 26/26-Abschluss
- [x] Netherfestung
  - [x] natürliche Truhe im Nether erkannt
  - [x] Netherfestungs-Loot korrekt zugeordnet
  - [x] Truhe genau einmal gezählt und Wiederöffnen stabil
- [x] Iglu: Loot-Fortschritt und kontrollierter 8/8-Abschluss
- [x] Dschungelpyramide: natürlicher Loot aus beiden Kisten, verzaubertes
      Buch, Dispenser-Regeln und kontrollierter 14/14-Abschluss
- [x] Ozeanruine
  - [x] natürliche Truhe einer warmen Ruine erkannt
  - [x] Ruinen-Loot korrekt zugeordnet und Truhe genau einmal gezählt
  - [x] Wiederöffnen bleibt ohne erneute Quellenzählung
  - [x] natürlicher „Seltsamer Sand“ lässt das Artefakt beim Bürsten sichtbar
        austreten, erzeugt aber keinen Drop und keinen Fortschritt
- [x] Plünderer-Außenposten
  - [x] natürliche Turmtruhe erkannt
  - [x] Außenposten-Loot korrekt zugeordnet
  - [x] zweifaches Öffnen zählt die Truhe nur einmal
- [x] Ruinenportal
  - [x] natürliche Truhe einer Ozean-/Unterwasservariante erkannt
  - [x] Ruinenportal-Loot korrekt zugeordnet
  - [x] Truhe genau einmal gezählt und Wiederöffnen stabil
- [x] Schiffswrack
  - [x] Versorgungstruhe erkannt
  - [x] Schatztruhe erkannt
  - [x] Kartentruhe erkannt
  - [x] drei natürliche Truhen über zwei Wracks als drei eindeutige Quellen
        gezählt
  - [x] Wiederöffnen zählt eine Truhe nicht erneut
- [x] Festung: beide Bibliothekskisten, gewöhnliche Kiste, normales und
      verzaubertes Buch getrennt sowie kontrollierter 27/27-Abschluss
- [x] Minenstollen
  - [x] natürlich generierte Güterlore mit `abandoned_mineshaft`-Loot erkannt
  - [x] Wiederöffnen zählt die Güterlore nicht erneut
  - [x] Abbau im Überlebensmodus droppt weder Lore noch Truhe oder Inhalt
- [x] Dorf mit unterschiedlichen Berufen und Biomen
  - [x] Ebenendorf mit Waffenschmied-Truhe geprüft
  - [x] Taigadorf mit biomeigener Wohnhaus-Truhe geprüft
  - [x] Taigadorf mit Gerberei-Truhe geprüft
  - [x] Taigadorf mit weiterer Waffenschmied-Truhe geprüft
  - [x] vier natürliche Truhen als vier eindeutige Quellen gezählt
  - [x] unterschiedliche Dorf-Loot-Tabellen gemeinsam derselben Kategorie
        zugeordnet
- [x] Waldanwesen
  - [x] natürliche Arena-/Galerietruhe erkannt
  - [x] Waldanwesen-Loot korrekt zugeordnet
  - [x] `music_disc_13` und `music_disc_cat` als getrennte Ziele erkannt
  - [x] Truhe genau einmal gezählt und Wiederöffnen stabil
- [x] Verlies
  - [x] über das originale Vanilla-Feature `minecraft:monster_room` erzeugt
  - [x] `simple_dungeon`-Loot erkannt und Truhe genau einmal gezählt
  - [x] normaler Monster-Spawner zählt nicht als Loot-Quelle
  - [x] natürliche gefüllte Truhe droppt beim Abbau weder Truhe noch Inhalt
- [x] Bastionsruine: 66er-Katalog, Sondervarianten, GUI und kontrollierter
      Abschluss
- [x] Prüfungskammern

## Kompatibilität und Qualität

- [x] echten Start- und Gameplay-Test mit Paper 26.1/26.1.2 durchführen.
  - [x] Paper 26.1.2 Build 74 mit Java 25 auf Port `25566` gestartet
  - [x] dieselbe für 26.2 getestete Plugin-JAR ohne API-Fehler geladen
  - [x] frische, getrennte SQLite-Datenbank angelegt
  - [x] 26.2-only-Ziel `music_disc_bounce` sauber übersprungen;
        Minenstollen zeigt auf 26.1.2 korrekt `0/21` statt `0/22`
  - [x] GUI, Strukturauswahl, Sidebar und BossBar geprüft
  - [x] natürliche Schatztruhe erkannt: neun Ziele und eine Quelle
  - [x] Wiederöffnen derselben Quelle verändert den Fortschritt nicht
  - [x] natürliche gefüllte Truhe droppt beim Abbau weder Truhe noch Inhalt
  - [x] selbst platzierte Truhe öffnet und droppt normal, ohne Fortschritt
  - [x] aktive Struktur, `9/17` Ziele und eine Quelle überleben den Neustart
  - [x] kontrollierter Strukturabschluss entfernt das HUD und gibt die nächste
        Auswahl frei
- [x] Windows-x64-Smoke-Test über GitHub Actions durchführen.
  - [x] Temurin Java 25
  - [x] Windows-Gradle-Wrapper, automatisierte Tests und Build
  - [x] Windows-Startskript
  - [x] eingebetteten Windows-x64-SQLite-Treiber und Datenbankinitialisierung
  - [x] Paper-26.2-Start, Plugin-Laden und sauberes Herunterfahren
- [x] automatisierte Tests ergänzen.
  - [x] SQLite-Persistenz
  - [x] Auswahl- und Abschlusslogik
  - [x] Reset
  - [x] eindeutige Quellenzählung
  - [x] Item-Matcher
  - [x] Kataloggrößen für 26.1 und 26.2 beim Pluginstart abgesichert
  - [x] Trial- und Bastion-Sonderfälle
- [x] Sidebar auf die aktuelle Component-API umstellen.
- [x] Itemvarianten in Chatmeldungen eindeutig benennen.
  - [x] unterschiedliche Schallplatten nicht beide nur als „Schallplatte“
        anzeigen, sondern beispielsweise „C418 – 13“ und „C418 – cat“
- [x] README vervollständigen.
  - [x] vollständige Command-Liste
  - [x] Installation für macOS und Windows
  - [x] Ablauf nach einem Hardcore-Tod
  - [x] Datenbank-, Backup- und Reset-Hinweise
  - [x] `structure-goals.yml` erklären
  - [x] Kompatibilitätsstatus dokumentieren
  - [x] finale Screenshots aufnehmen und vorbereitete Bildverweise aktivieren
- [x] Release vorbereiten.
  - [x] Änderungen bewusst committen
  - [x] Snapshot-Version durch `0.1.0-beta.1` ersetzen
  - [x] automatische Build-, Test- und Release-Aktionen einrichten
  - [x] frische Installation ohne Datenbank testen
  - [x] Upgrade mit bestehender Datenbank testen
  - [x] Release-JAR und SHA-256-Prüfsumme automatisch erzeugen

## Optionale spätere Verbesserungen

- [x] BossBar als Alternative zur Sidebar anbieten.
- [ ] optionales Resource-Pack-HUD prüfen.
- [ ] Texte und Farben konfigurierbar machen.
- [ ] Admin-Befehl zum temporären Umschalten normaler Drops prüfen.
- [x] Manuellen Fortschritts-Export und sichere Wiederherstellung dokumentieren.
  - [x] vollständigen Plugin-Zustand über `plugins/AllAndOnlyChests` sichern
  - [x] Welt und Plugin-Daten gemeinsam sichern und wiederherstellen
  - [x] SQLite-Hinweis für laufende Server dokumentieren
- [ ] Optionalen Admin-Befehl oder automatische versionierte Backups prüfen.
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
- Die eigenständige Neuimplementierung wird unter der möglichst freien
  Zero-Clause BSD License (`0BSD`) veröffentlicht.
- Die Sidebar verwendet die aktuelle Component-API. Über `/chesthud` kann
  zwischen Sidebar, BossBar und ausgeblendeter Anzeige gewechselt werden.
  Ein Resource Pack bleibt eine optionale spätere Erweiterung.
