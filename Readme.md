
# v-pipe

Das V&S Pipeline-Werkzeug liest ein Datenfile  
mit einer Projektlandschaft ein und erzeugt eine  
Übersicht der Belastung aller beteiligten Abteilungen.  
Es gibt verschiedene Optionen, Projekte zu verschieben  
und Belastungs-Szenarien zu modellieren.


## Installation

Die Distribution ist ein v-pipe.zip.  
Dieses Zip-File entpacken - ideal in das Verzeichnis:  
`c:\v-pipe`  

Dort liegen dann die Verzeichnisse und Dateien:

- `c:\v-pipe\v-pipe.exe // der Starter`  
- `c:\v-pipe\lib        // ein paar Java-Bibliotheken`  
- `c:\v-pipe\jre        // eine Java-Laufzeit-Umgebung`    


## Anwendung

#### Start von v-pipe:  

`Windows-Taste` und tippen: `c m d + Enter-Taste`  
Das startet einen Kommando-Zeilen-Prompt.  
Dort navigieren in das v-pipe-Verzeichnis.  
Also `cd \v-pipe`  
Dort starten des programms durch tippen von  
`v-pipe ENTER`
Jetzt kann das Fenster zur Seite geschoben werden.  
Wann immer die Grunddaten neu erzeugt, verändert oder  
gelöscht werden, rechnet v-pipe und erzeugt  
die Ergebnis-Dateien neu.

#### Daten-Dateien:

`Projekt-Start-End-Abt-Capa.txt`  
enthält beispielsweise   
`Projekt1 20.05.2020 31.5.2020 Abt3 45.6`  
`Projekt1 12.06.2020 31.6.2020 Abt4 45.6`  
`Projekt1 15.07.2020 19.7.2020 Abt3.3 45.6`  
`P2 20.04.2020 12.6.2020 Abt3 5.6`  
`P2 20.06.2020 31.7.2020 Abt3 4`  

Datei enthält alle Projekte mit allen Tasks,  
deren Zuordnung zu Abteilungen und die  
notwendige Kapazität als Komma-Zahl mit PUNKT.  
Komma, Strichpunkt, Tab, Leerzeichen sind Trennzeichen.  
KEINE Leerzeichen in den Projekt-und-Abteilungs-Bezeichnungen. 

`Projekt-Verschiebung.txt`  
enthält beispielsweise  
`P2 14`  
`Projekt1 -21`  

Datei enthält die Verschiebung der Projekte in Tagen.  
Es müssen nicht alle Projekte aufgeführt sein.   
`+` bedeutet: Verscheibung in die Zukunft.  
Der 15.7.2020 wird duch  `-5` zum 10.7.2020  
und durch  `5` zum 20.7.2020.  

#### Ergebnis-Dateien
Die Belastung der Abteilungen liegt in:  
`Department-Load-Result.txt`

**Überschreiben von Ergebnissen**  
Ergebnis-Dateien werden nie überschrieben.  
Wann immer eine Ergebnisdatei schon existiert,  
wird ersatzweise eine Datei erzeugt, die als  
Präfix das Datum und die Uhrzeit enthält.  
Z.B.  
`2020-05-05 09.31.59  Department-Load-Result.txt`


## Realease
6.5.2020 `0.1.0-Vor-Ruhlamat`


##### Features

Release 0.1.0  

- Readme.md :-)
- Beispiel-Dateien.
- Lesen von Projekt-Verschiebung.txt  
- Automatisches Neu-Generieren der Ergebnisse, wenn Datendateien sich ändern.   

Release 0.0
- Lesen von Projekt-Start-End-Abt-Kapa.txt   
- Schreiben von Department-Load-Result.txt

##### Bugs fixed
- Department-Load-Result.txt ist ne Sparce-Matrix

  
## Bekannte Bugs  

keine