
## v-pipe

Das V&S Pipeline-Werkzeug liest Daten-Dateien  
mit einer Projektlandschaft ein und erzeugt eine  
Übersicht der Belastung aller beteiligten Abteilungen.  
Es gibt die Möglichkeit, Projekte zu verschieben  
und Portfolio-Szenarien zu modellieren.

## Installation

Die Distribution ist ein `v-pipe.zip`.  
Dieses Zip-File entpacken - ideal in das Verzeichnis:  
`c:\v-pipe`
In Win10 braucht man dazu keinen "Zipper". Zipfile ablegen.  
Per Explorer (WIN+E) das v-pipe.zip 'öffnen'. Einfach draufklicken.  
Inhalte werden wie ein File-System angezeigt.  
Oberes Verzeichnis (`v-pipe`) per Copy&Paste mit CTRL-C 'kopieren'  
und nach C: per CTRL-V entpacken.  
Fertig.

In C:\v-pipe liegen dann die Verzeichnisse und Dateien:

- `README.html` (Link zur Betriebsanleitung auf Github)
- `Readme.md` (diese Betriebsanleitung als Markdown - falls offline)  
Markdown-Plugin für Chrome installieren...  
[MarkdownPreviewPlus](https://t.ly/xB5M)  
in den Einstellungen des Plugins unter  
Einstellungen/Erweiterungen/MarkdownPreviewPlus:  
Option: *Zugriff auf Datei-URLs zulassen* aktivieren 
- `c:\v-pipe\v-pipe.exe` (der Starter - Doppelklick und los geht's.)  
- `c:\v-pipe\lib` (ein paar Java-Bibliotheken. Finger weg ;-))  
- `c:\v-pipe\jre` (eine Java-Laufzeit-Umgebung. Nicht anfassen...)    
- `Projekt-Start-End-Abt-Kapa.txt` (Beispiel-Datei: hier wird gearbeitet!)
- `Projekt-Verschiebung.txt` (Beispiel-Datei: hier spielen und arbeiten!)


## Anwendung

### Start von v-pipe  

Einfach Doppelklick auf v-pipe.exe. Das startet den  
Deamon-Mode: v-pipe lauert jetzt auf Veränderungen  
der Daten-Dateien und zeigt Fehlermeldungen oder rechnet.  

**Alternative:** 
`Windows-Taste` und tippen: `cmd + ENTER-Taste`  
Das startet einen Kommando-Zeilen-Prompt.  
Dort navigieren in das v-pipe-Verzeichnis.  
Also je nach Installations-Verzeichnis: `cd \v-pipe`  
Dort Programm durch tippen von  
`v-pipe + ENTER-Taste` starten.  

Jetzt kann das Fenster zur Seite geschoben werden.  
Wann immer die Grunddaten neu erzeugt, verändert oder  
gelöscht werden, rechnet v-pipe und erzeugt  
die Ergebnis-Dateien neu.

### Ablage der Arbeits-Dateien
Alle Arbeits-Dateien (Grunddaten und Ergebnise der Berechnungen)  
werden im Installations-Verzeichnis abgelegt. 
Also z.B.: `c:\v-pipe`

### Daten-Dateien

`Projekt-Start-End-Abt-Capa.txt`  
enthält beispielsweise   
`Projekt-1-Neu-Ulm 20.05.2020 31.5.2020 Abt3 45.6`  
`Projekt-1-Neu-Ulm 12.06.2020 31.6.2020 Abt4 45.6`  
`Projekt-1 15.06.2020 19.6.2020 Abt3.3 45.6`  
`P2 20.04.2020 12.6.2020 Abt3 5.6`  
`P2 20.06.2020 31.7.2020 Abt3 4`  

Datei enthält alle Projekte mit allen Tasks,  
deren Zuordnung zu Abteilungen und die  
notwendige Kapazität als 'Komma-Zahl mit PUNKT' - z.B.: 14.2  
Komma, Strichpunkt, Tab, Leerzeichen sind Trennzeichen.  
KEINE Leerzeichen in den Projekt- und Abteilungs-Bezeichnungen.
Falls dort welche sind: Ersetzen mit Unterstrich = _

`Projekt-Verschiebung.txt`  
enthält beispielsweise  
`P2 14`  
`Projekt1 -21`  

Datei enthält die Verschiebung der Projekte in Tagen.  
Es müssen nicht alle Projekte aufgeführt sein.   
Nur die Verschobenen.  
`+` bedeutet: Verscheibung in die Zukunft.  
P2 wird um 2 Wochen geschoben.  
Der 31.7.2020 wird duch `14` zum 14.8.2020  
Projekt-1-Neu-Ulm wird durch `-21` vorgezogen:   
Aus 31.6.2020 wird 10.6.2020.  

### Ergebnis-Dateien
Die Belastung der Abteilungen liegt in:  
`Abteilungs-Kapazitäts-Belastung-Woche.txt`  
und   
`Abteilungs-Kapazitäts-Belastung-Monat.txt`  


### Überschreiben von Ergebnissen  
Ergebnis-Dateien werden nie überschrieben.  
Wann immer eine Ergebnisdatei schon existiert,  
wird ein Backup mit Datum und Uhrzeit erstellt.  
Z.B.  
`Abteilungs-Kapazitäts-Belastung backup vom 2020-05-05 09.31.59.txt`  
Die aktuellsten Ergebnisse liegen also immer in  
`Abteilungs-Kapazitäts-Belastung.txt`

### Terminfester 
Alle Termin-Fenster-Angaben sind hinten offene Intervalle.  
D.h. der letzte Tag des Intervalls gehört nicht zum Intervall dazu.  
Die zwei Intervalle  
20.10.2020 25.10.2020  
und  
25.10.2020 30.10.2020  
überlappen keinen Augenblick! Das erste endet im letzten Augenblick  
VOR dem 25.10.2020. Insofern reicht es genau bis zum 25.10.  
Das zweite beginnt mit dem ersten Augenblick des 25.  
D.h. Das erste Intervall besteht aus den ganzen Tagen:  
20 21 22 23 24 - also 5 Tage. Das zweite aus 25 26 27 28 29.  
Beide Intervalle hintereinander umfassen 10 kontinuierliche Tage.  
Der 20.10. gehört dazu. Der 30.10. nicht!

## Realease

### Features

2020-05-07 `0.2.0-Monats-Belastung`

- beim öffnen von v-pipe im deamon-mode geht ein Browser mit hilfe auf   
(essentials zu den Daten-Formaten)
- Monats-Auswertung auch... Abteilungs-Kapazitäts-Belastung-Woche.txt und -Monat.txt
- Abteilungs-Kapazitäts-Belastung.txt enthält immer die aktuelle Ergebnisse.  
 Fall Überschreiben droht wird vor dem Überschreiben eine Backup-Datei erzeugt.

2020-05-06 `0.1.0-Vor-Ruhlamat`  
- Readme.md :-)
- Beispiel-Dateien.
- Lesen von Projekt-Verschiebung.txt  
- Automatisches Neu-Generieren der Ergebnisse, wenn Datendateien sich ändern.   

2020-04-25 `0.0`
- Lesen von Projekt-Start-End-Abt-Kapa.txt   
- Schreiben von Department-Load-Result.txt

### Bugs fixed
- Department-Load-Result.txt ist ne Sparce-Matrix

  
### Bekannte Bugs und Feature-Requests  

#### Bugs  
 
- keine ;-)

#### Features (zukünftig, angedacht) 

wären nützlich - Nützlichkeit in absteigender Reihenfolge:  


- read by Json-Format:   
http://docs.groovy-lang.org/2.4.0/html/gapi/groovy/json/JsonSlurper.html
-  Logging mit einfacher Konfigurierbarkeit durch Property-File
- File-Polling in Excel (2s-readIfNewerThan10Sec and Paste) -   
damit es eine Online-Visualisierung gibt.
- Darstellung des Projektportfolios (kritischen Pfade, Staffelung, Kapa-Peaks)
- Modellierung der Integrations-Phase und Staffelung auf Basis von  
 "Max-Projekte" oder "Max-Kapa-Pro-Woche"
- Kapazitäts-Belastung auf Basis der Daten-Datei Abteilungs-Kapazitäts-Angebot.txt
- Ausgabe der Kapa-Belastung in der Ergebnis-Datei Abteilungs-Prozent-Auslastung.txt

## Feedback
an: [benno.loeffler@gmx.de](mailto:benno.loeffler@gmx.de)