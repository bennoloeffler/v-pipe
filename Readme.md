
## v-pipe

Das V&S Pipeline-Werkzeug liest Daten-Dateien  
mit einer Projektlandschaft ein und erzeugt eine  
Übersicht der Belastung aller beteiligten Abteilungen.  
Es gibt die Möglichkeit, Projekte zu verschieben  
und Portfolio-Szenarien zu modellieren.  
Quickstart- Video? [Hier Klicken...](https://www.loom.com/share/84d7636a20e745d19ac0489aa4eba2fc)
## Installation

Die Distribution ist ein `v-pipe.zip`.  
Dieses Zip-File entpacken - ideal in das Verzeichnis:  
`c:\v-pipe`
In Win10 braucht man dazu keinen "Zipper".  
1. Zipfile v-pipe-release-xyz.zip 'irgendwo' ablegen.  
2. Per Explorer (WIN+E) das v-pipe.zip 'irgendwo öffnen'.
Einfach im Explorer einmal draufklicken.  
3. Inhalte werden wie ein File-System angezeigt.  
4. Oberes Verzeichnis (`v-pipe`) per Copy&Paste mit CTRL-C 'kopieren'  
und nach C: einfügen. Also im Explorer auf C: klicken und per CTRL-V entpacken.  
Fertig.
5. Das klappt auch für einen Programm-Update.  
eigene Datein-Dateien werden nicht überschrieben.  
Backup schadet trotzdem nicht.

In C:\v-pipe liegen dann die Verzeichnisse und Dateien:

- `README.html` (Link zur Betriebsanleitung auf Github)
- `Readme.md` (diese Betriebsanleitung als Markdown - falls offline)  
Markdown-Plugin für Chrome installieren...  
[MarkdownPreviewPlus](https://t.ly/xB5M)  
in den Einstellungen des Plugins unter  
Einstellungen/Erweiterungen/MarkdownPreviewPlus:  
Option: *Zugriff auf Datei-URLs zulassen* aktivieren 
- `v-pipe.exe` (der Starter - Doppelklick und los geht's.)
- `v-pipe-gui.exe` (visuelle Darstellung - Doppelklick und los.)  
- `lib` (ein paar Java-Bibliotheken. Finger weg ;-))  
- `jre` (eine Java-Laufzeit-Umgebung. Nicht anfassen...)
- `bsp-daten` (alle Daten-Dateien als funktionierende Beispiele)    
    - `Projekt-Start-End-Abt-Kapa.txt` (Grunddaten: Alle Tasks)
    - `Projekt-Verschiebung.txt` (Optional: Projekte schieben)
    - `Integrations-Phasen.txt` (Optional: Projekte staffeln)
    - `v-pipe-Auswertung.xlsx` (Vorlage für Visualisierung)


## Anwendung

### Start von v-pipe  

Einfach Doppelklick auf `v-pipe.exe`. Das startet den  
Deamon-Mode: v-pipe lauert jetzt auf Veränderungen  
der Daten-Dateien und zeigt Fehlermeldungen oder rechnet.  
Mit v-pipe-exe bekommt man die Daten schnell konsistent.  
Wenn die Daten konstistent sind, dann ansehen:  
`v-pipe-gui.exe`

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
die Ergebnis-Dateien neu - oder zeigt Datenfehler an.

### Ablage der Arbeits-Dateien
Alle Arbeits-Dateien (Grunddaten und Ergebnise der Berechnungen)  
werden im Installations-Verzeichnis abgelegt. 
Also z.B.: `c:\v-pipe`

### Daten-Dateien

`Projekt-Start-End-Abt-Capa.txt`  
**Grunddaten (erforderlich):** alle Tasks aller Projekte  
Enthält beispielsweise:   
`Projekt-1-Neu-Ulm 20.05.2020 31.5.2020 Abt3 45.6`  
`Projekt-1-Neu-Ulm 12.06.2020 31.6.2020 Abt4 45.6`  
`Projekt-1 15.06.2020 19.6.2020 Abt3.3 45.6`  
`P2 20.04.2020 12.6.2020 Abt3 5.6`  
`P2 20.06.2020 31.7.2020 Abt3 4`  

**Bedeutung:** Datei enthält alle Projekte mit allen Tasks,  
deren Zuordnung zu Abteilungen und die  
notwendige Kapazität als 'Komma-Zahl mit PUNKT' - z.B.: 14.2  
Komma, Strichpunkt, Tab, Leerzeichen sind Trennzeichen.  
KEINE Leerzeichen in den Projekt- und Abteilungs-Bezeichnungen.  
Falls dort welche sind: Ersetzen mit Unterstrich = _

`Projekt-Verschiebung.txt`  
**Grunddaten (optional):** Verschiebung aller Tasks einzelner Projekte  
Enthält beispielsweise:  
`P2 14`  
`Projekt1 -21`  

**Bedeutung:** Datei enthält die Verschiebung der Projekte in Tagen.  
Es müssen nicht alle Projekte aufgeführt sein.   
Nur die Verschobenen.  
`+` bedeutet: Verscheibung in die Zukunft.  
P2 wird um 2 Wochen geschoben.  
Der 31.7.2020 wird duch `14` zum 14.8.2020  
Projekt-1-Neu-Ulm wird durch `-21` vorgezogen:   
Aus 31.6.2020 wird 10.6.2020.  


`Integrations-Phasen.txt`
**Grunddaten (optional):** Integrations-Phasen aller Projekte für Staffelung  
Enthält beispielsweise:  
6  
p1 15.1.2020 18.1.2020 2  
p2 30.3.2020 1.4.2020 1  
**Bedeutung:** Die erste Zeile stellt die Anzahl der Slots  
in der Integrationsphase dar. Die Integrationsphasen von p1, p2 etc. sind  
untereinander aufgeführt. Die Zahl am Ende jeder Zeile ist der Bedarf an Slots.  
Dieser ist in der Regel 1 und maximal gleich der Anzahl der Slots.  

`Abteilungs-Kapazitäts-Angebot.txt`
**Grunddaten (optional):** Kapazitäts-Angebot aller Abteilungen für Auswertung  
Enthält beispielsweise:  

{
    "Kapa_Gesamt": {
    
       "Feiertage": ["1.1.2020", "1.6.2020"],
       
       "Kapa_Profil": {
          "2020-23": 80, // in Prozent!
          "2020-24": 60,
          "2020-25": 80
       }
       
},
   
   "Abteilungen": {   
      
      "Konstruktion": {

          "Kapa": {
             "gelb": 140,
             "rot": 190
          },

          "Kapa_Profil": {
             "2020-23": 100,
             "2020-24": { "gelb": 140, "rot": 250 },
             "2020-25": 80,
             "2020-26": { "gelb": 140, "rot": 190 },
             "2020-27": 100
          }
      },

      "Montage": {

         "Kapa": {
            "gelb": 240,
            "rot": 490
         },

         "Kapa_Profil": {
         }
      },

      "IBN": {
         "Kapa": {
            "gelb": 340,
            "rot": 500
         }
      }
   }
}



**Bedeutung:** Kapa_Gesamt beinhaltet Feiertage und ein Kapa_Profil  
in Prozent. Der Abschnitt Abteilungen beinhaltet Abteilungsnamen und  
darunter jeweils zwei Infos:  
`Kapa`: rote und gelbe Kapazitätsgrenze. Diese Einteilung dient lediglich der   
Visualisierung.  
`Kapa_Profil`: Dort kann das Kapa-Profil der Gesamt-Firma  
(in KWs und Prozent) überschrieben werden.  
Kapazitäts-Sprünge werden so beschrieben:  
`"2020-24": { "gelb": 140, "rot": 250 }`  
Das Kapa_Profil ist optional.  
Die Syntax der Datei ist JSON.  


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

### Ablage der Backup-Dateien
Alle Dateien, die überschrieben werden müssten, werden abgelegt in:   
Also z.B.: `c:\v-pipe\backup`


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

### Optionen 

-s  
Single-Run-Mode. Nur ein Rechendurchgang. Kein laufender Deamon,  
der auf veränderte Dateien reagiert.  

-m  
Multi-Instance-Mode. Mehrere v-pipe.exe können gleichzeitig laufen.  
Das ist nur hilfreich, wenn in unterschiedlichen Verzeichnissen  
gleichzeitig gearbeitet werden soll.

## Realease

### Features

TODO: 2020-06-XX `0.9.0-Template`
- Datenfile für Portfolio auf Basis von Templates

TODO: 2020-06-XX `0.8.0-LoadSave`
- Speichern der Daten (in eigenem Daten-Ordner) - öffnen von Datenordnern

2020-06-14 `0.7.0-GUI-Project-Editor`
- Tasks eines Projektes auch verschieben.

2020-06-01 `0.6.0-GUI-Project-View`
- Tasks eines Projektes visualisieren (noch nicht verschieben).

2020-05-25 `0.5.0-GUI-Project`
- Belastung durch ein Projekt visualisieren.

2020-05-23 `0.4.0-GUI-Pipeliner`
- Pipeline visualisieren, editieren.

2020-05-13 `0.3.1-Pipeliner`
- Performance besser beim schreiben  
 (größer 40s --> deutlich kleiner 10s mit Ruhlamat-Daten)

2020-05-10 `0.3.0-Pipeliner`
- Pipeliner - lesen von Integrations-Phasen.txt
- Browser mit Hilfe nur beim ersten Start
- Beispiel-Daten in Ordner bsp-daten, damit Update keine Daten zerstört

2020-05-07 `0.2.0-Monats-Belastung`
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
 
- Es scheint so, als ob v-pipe beim ersten Start zweimal nacheinander startet.  
bzw. als ob kurz nach dem Start ein Neustart erfolgt. Harmlos, aber irritierend.
- beim löschen von Datenfiles fehlt ein DELETE. util-packages Problem

#### Features (zukünftig, angedacht) 

wären nützlich - Nützlichkeit in absteigender Reihenfolge:  

- Pipelining ohne "quetschen" - mit Löchen, die Termin-orientiert entstehen.  
Also mäßige Auslastung. Dafür wäre es praktisch, 3 "Staffelungen" zu haben:
1.) OPTION FILL PIPELINE: _tight (also ohne Lücken), 2: _gap (also mit Lücken, wenn Termine dazu führen) 
2.) OPTION SORT_PROJECTS: sort_in_file, sort_pipeline_end_date, sort_project_end_date,  
_search_latest (search the next one, that is latest compared to its delivery date)
3:) vernünftig scheint: _gap und _search_latest 
- Kommentare in Datenfiles erlauben. Solche: //
- File-Polling in Excel (2s-readIfNewerThan10Sec and Paste) -   
damit es eine Online-Visualisierung gibt.
- Darstellung des Projektportfolios (kritischen Pfade, Staffelung, Kapa-Peaks)
- Logging mit einfacher Konfigurierbarkeit durch Property-File  
- Start Option Kommando-Zeile: Arbeitsverzeichnnis - eines oder mehrere
- Start Optionen auch in Konfig-File ablegbar - inbesondere Arbeitsverzeichnisse  
- Kapazitäts-Belastung auf Basis der Daten-Datei Abteilungs-Kapazitäts-Angebot.txt
- Ausgabe der Kapa-Belastung in der Ergebnis-Datei Abteilungs-Prozent-Auslastung.txt
- read by Json-Format:   
http://docs.groovy-lang.org/2.4.0/html/gapi/groovy/json/JsonSlurper.html
- verzeichnis help für Doku (bis auf Referenz.html)

## Feedback
an: [benno.loeffler@gmx.de](mailto:benno.loeffler@gmx.de)