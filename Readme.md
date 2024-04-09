
# v-pipe

Das V&S-Pipeline-Werkzeug macht ein Projekt-Portfolio    
sichtbar. Und zwar in Echtzeit - also beim Tippen sozusagen.
So können Führungsteams oder Projekt-Manager gemeinsam sehen,
was wirklich los ist, schnelle Szenarien durchspielen und
gemeinsam entscheiden.

Wie das geht? Siehe Teaser-Video und Tutorial:
[Alle Featueres auf einen Blick](https://bennoloeffler.github.io/v-pipe/)

v-pipe liest einfache Text-Dateien mit Projekt-Daten  
und Abteilungs-Kapazitäten ein und erzeugt eine  
Übersicht der Belastung aller beteiligten Abteilungen.  
Es gibt die Möglichkeit, Projekte per Pfeiltasten  
wochenweise zu verschieben und die Auswirkungen  
in Echtzeit zu sehen. Alle Tasten-Kürzel  
sind in der Kopfzeile von v-pipe zu sehen.

## Installation

### Download
Download aktuelle Versionen für macOS, Windows und Linux hier:  
[download from dropbox-share](https://www.dropbox.com/sh/0ns0bkkz7g2dua2/AABMMgMW0crOoZYfQcOEu30Sa?dl=0)

### Install
Die Distribution ist ein Zip-File mit dem Namen:   
  
`v-pipe-major.minor.bugfix-ReleaseName-OperatingSystem.zip`.    
`v-pipe-1.5.0-average-load-windows.zip` (Beispiel)  
  
Aktuelles **zip-file runterladen und entpacken** - z. B. in das lokale Verzeichnis:  
`c:\v-pipe-1.5.0-average-load`. Aber auch gerne auf einen Memory-Stick oder ins eigene Home.  
Oder nach `Programme` (als admin). Tut auch.  

Wenn die Dateien kopiert sind, befinden sich in ihrem Installationsverzeichnis  
auf ihrem Rechner (also z.B. in C:\v-pipe-2.2.0-update-by-id)  
folgende Verzeichnisse und Dateien:

- `Readme.md` (diese Betriebsanleitung als Markdown - falls offline)  
- `v-pipe-macos.sh.command` (macOS-Starter - Doppelklick)
- `v-pipe-linux.sh` (Linux-Starter - in Terminal starten)
- `v-pipe-win.bat` (Windows-Starter - Doppelklick)
- `lib` (ein paar Java-Bibliotheken. Finger weg ;-))  
- `jre` (eine Java-Laufzeit-Umgebung. Nicht anfassen...)
- `bsp-daten` (Unterordner sind gefüllt mit Daten-Dateien als funktionierende Beispiele)
    - Grunddaten
        - `Projekt-Start-End-Abt-Kapa.txt` (ERFORDERLICH: Alle Tasks)
        - `Abteilungs-Kapazitaets-Angebot.txt` (OPTIONAL: Kapa Abteilungen)
        - `Integrations-Phasen.txt` (OPTIONAL: Integrationsphasen der Projekte)
    - Vorlagen
        - `Vorlagen-Projekt-Start-End-Abt-Kapa.txt` (OPTIONAL: Alle Vorlagen, wie Tasks)
        - `Vorlagen-Integrations-Phasen.txt` (OPTIONAL: Integrationsphasen der Vorlagen  
          nur erforderlich, wenn: Vorlagen-Projekt-Start-End-Abt-Kapa.txt UND Integrations-Phasen.txt 
    - Szenarien        
        - `Projekt-Verschiebung.txt` (OPTIONAL: Projekte schieben)
        - `Szenario-Kopie-Original-Verschiebung.txt` (OPTIONAL: Projekte kopieren+schieben)


## Release History

### 2024-04-09 `2.4.0-better-details`
- make details in load more understandable
- fix bug in average load calculation 

### 2024-01-31 `2.3.2-filter-and-export`
- export load view as pdf and data into an folder called "export"
- filter the visual view and export data by shrinking the range of aktive calendar weeks 

### 2022-08-12 `2.2.0-update-by-id`
- release and starter for win, linux and macos
- error checking mode - work on the data-files and see error messages in realtime in  gui
- plugin for getting data transformed (e.g out of excel), when reading delta-data
- merging updates based on ID: tags residing in comment field of tasks

### 2022-04-17 `2.1.0-copy-and-update`
- have a directory scanned for delta-data in order to update the model.
- copy a project (not from template - just plain copy)

### 2022-04-16 `2.0.0-all-in-gui`
- linux, windows and mac
- month load
- project comments/history

### 2022-03-16 `1.9.0-tidy-code`
- moved and renamed many classes / packages for more clarity

### 2022-03-14 `1.8.0-beta-all-gui`
- everything can be changed in gui 

### 2022-03-12 `1.7.0-ip-in-gui`
- pipeline (integration phases) can be created / deleted / hidden

### 2022-03-12 `1.6.0-ipco-pax-edition`
- can only open valid data folders
- simplified creation of capacity model
- can create new model from GUI

### 2022-02-27 `1.5.0-average-load`
- show load average (5 weeks moving average) by pressing 'a'
- created starter for dropbox

### 2022-02-13 `1.4.4-delivery-date-macos`
- create delivery date from last task, if not available
- save, change and visualize delivery date 

### 2021-08-26 `1.3.0-Create-Project`
- create new Project from "nothing"
- show task description in project tooltip

### 2021-02-07 `1.2.0-Szenario-Templates`
- can model and use templates (Vorlagen)
- delete projects in Detail-View (delete-Button)
- vpipeGui does not open current working directory at startup any more 
- fixed bug: caching problem with recalc of week-patterns and capa-model after model reloading fixed 

### 2020-11-03 `1.1.0-Szenario-Differenzen`
- compare files

### 2020-10-30 `1.0.3-Details-Editor-Bugfixes`
- increase / decrease tasks with CTRL-arrow (--> = inc, <-- = dec)

### 2020-10-20 `1.0.2-Details-Editor-Bugfixes`
- Datenkonsistenz-Prüfung Abteilungen geht wieder

### 2020-10-19 `1.0.1-Details-Editor-Bugfixes`
- duplizieren
- Änderungen annehmen
- CTRL-Pfeil vergrößert / verkleinert die Projekt-Tasks
- Löschen des letzten Task in Detailansicht nicht möglich

### 2020-10-19 `1.0.0-RELEASE-Projekt-Editor`
- Editor für Projekt-Daten (Alle Felder bearbeiten, Copy und Cut von Tasks)
- Reihenfolge der Tasks in der Projektansicht wie in Abteilungs-Kapa-Angebot, falls vorhanden  

### 2020-09-16 `0.9.13-Beta-3-Bugfixes`  
- Reihenfolge der Projekttasks nach End-Termin
- Reihenfolge der Auslastung wie in Abteilungs-Kapa-Angebot, falls vorhanden  
- alle Angaben in Auslastungs-Tooltip auf eine Stelle nach Komma runden

### 2020-09-15 `0.9.12-Beta-2-Bugfixes`
- Kapabedarf der Tasks und Projekte sichtbar  

### 2020-08-30 `0.9.11-Beta-1-Bugfixes`
- Cursor sprang 

### 2020-08-29 `0.9.10-LoadSomeDetails`
- drei Modi der Tooltips im Auslastungsfenster: no, some, details
- vollständige Synchronistation der Scrollbars (Staffelung - Auslastung)
- Cursor mit Fadenkreuz in der Staffelung und Projektsicht
 
### 2020-08-07 `0.9.7-ScrollCenter`
- zentrieren beim Scrollen mit Maus: Element des Mouse-Cursors bleibt sichtbar
- zentrieren beim Scrollen mit +/-: Tasten-Cursor bleibt sichtbar

### 2020-08-07 `0.9.6-ScrollNice`
- Scrollt zu 'jetzt' beim öffnen
- sprint nicht mehr beim umschalten der Detail-Tooltips
- Bug bei Anzeige der Skalierung der Kapa-Belastung gefixt
- mit 'n' kann man den Cursor zu 'jetzt' fahren (_n_ow)

### 2020-07-09 `0.9.5-BetaPhase-Bug`
- Sort ToolTip + 2-Stellen-Jahres-Bug raus

### 2020-07-08 `0.9.4-BetaPhaseNice`
- Markierungen, Doku

### 2020-07-06 `0.9.3-BetaPhase`
- minor changes like examples, docu, marker, fucus by tab and mouse, mouse-cursor, tooltips (d)
 
### 2020-06-30 `0.9.1-TePiLoSa`
- Pipeline-View: Verschieben auch ohne piplining Daten crasht nicht mehr

### 2020-06-30 `0.9.0-TePiLoSa`
- visuelles Pipelining

### 2020-06-21 `0.8.0-LoadSave-Template`
- Speichern der Daten (in eigenem Daten-Ordner) - öffnen von Datenordnern
- Szeanrien im Portfolio auf Basis von Templates

### 2020-06-14 `0.7.0-GUI-Project-Editor`
- Tasks eines Projektes auch verschieben.

### 2020-06-01 `0.6.0-GUI-Project-View`
- Tasks eines Projektes visualisieren (noch nicht verschieben).

### 2020-05-25 `0.5.0-GUI-Project`
- Belastung durch ein Projekt visualisieren.

### 2020-05-23 `0.4.0-GUI-Pipeliner`
- Pipeline visualisieren, editieren.

### 2020-05-13 `0.3.1-Pipeliner`
- Performance besser beim schreiben  
 (größer 40s --> deutlich kleiner 10s mit Ruhlamat-Daten)

### 2020-05-10 `0.3.0-Pipeliner`
- Pipeliner - lesen von Integrations-Phasen.txt
- Browser mit Hilfe nur beim ersten Start
- Beispiel-Daten in Ordner bsp-daten, damit Update keine Daten zerstört

### 2020-05-07 `0.2.0-Monats-Belastung`
- Option -s startet im Single-Modus = kein Deamon.  
Nur ein Durchlauf.
- Option -m startet im Multi-Instance-Modus.  
Es können mehrere v-pipes gleichzeitig laufen.  
Das ist nur vernünftig in verschiedenen Verzeichnissen!  
- beim öffnen von v-pipe im deamon-mode geht beim Start ein Browser mit Hilfe auf   
(essentials zu den Daten-Formaten)
- Monats-Auswertung auch... Abteilungs-Kapazitäts-Belastung-Woche.txt und -Monat.txt
- Abteilungs-Kapazitäts-Belastung.txt enthält immer die aktuelle Ergebnisse.  
 Falls Überschreiben droht wird vor dem Überschreiben eine Backup-Datei erzeugt.

### 2020-05-06 `0.1.0-Vor-Ruhlamat`  
- Readme.md :-)
- Beispiel-Dateien.
- Lesen von Projekt-Verschiebung.txt  
- Automatisches Neu-Generieren der Ergebnisse, wenn Datendateien sich ändern.   

### 2020-04-25 `0.0`
- Lesen von Projekt-Start-End-Abt-Kapa.txt   
- Schreiben von Department-Load-Result.txt

## Ideen und zukünftige Features 
Das hier ist keine Roadmap, sondern eine Ideen-Sammlung.  
Nützlichkeit in absteigender Reihenfolge:
- Automatisches Pipelining (entweder an der Pipeline oder an einer Abteilung)
- Monte-Carlo Simulation für "wenns gut läuft / wenns schlecht läuft"
- settings file für user: aktuelles Verzeichnis, details, offene Fenster, Window-Size, Splitter, ...
- Finanz-Zahlen und damit Durchsatz-Kennzahlen
- adapter elegantly import project data
- backlog of department for X weeks (in order to simulate "additional work")
- AutoSave bei jedem "setDirty - in eigenem Thread auf einer Kopie des Modells" - damit gäbe es ein UNDO.

### bekannte BUGS
**noch nicht gefixt**
- macOS: Schließen mit dem System-Menu oder Command-Q ohne Abfrage nach Speichern. May be fixed [with this hint](https://alvinalexander.com/java/java-mac-osx-about-preferences-quit-application-adapter/)  
- macOS: two finger zoom and scroll does not work. [maybe this helps](https://github.com/mcourteaux/MultiTouch-Gestures-Java)  
- beim Öffnen eines fehlerhaften Modells bricht der Korrektur-Modus ab und verlangt speichern

**gefixt**
- macOS: two-finger-zoom lead to java.lang.NoSuchMethodError: handleGestureFromNative  
Fix: used java11.0.15

## Feedback
an: benno.loeffler AT gmx.de