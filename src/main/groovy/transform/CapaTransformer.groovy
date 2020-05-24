package transform

import core.TaskInProject
import core.VpipeDataException
import groovy.json.JsonSlurper
import groovy.transform.Immutable
import groovy.transform.ToString
import utils.FileSupport

import static core.TaskInProject.WeekOrMonth.WEEK

@ToString
@Immutable
class YellowRedLimit {
    double yellow
    double red
}

/**
 * Can move all the starting and ending of core.TaskInProject.
 * Based on capacity of the departments.
 */
@groovy.transform.InheritConstructors
class CapaTransformer extends Transformer {

    static String FILE_NAME = "Abteilungs-Kapazitaets-Angebot.txt"

    /**
     * key: department -->
     * key: year-week-string 2020-W04 -->
     * value: the capaAvailable
     */
    Map<String, Map<String, YellowRedLimit>> capaAvailable
    //List<Date> publicHoliday

    /**
     * cache for recalc
     */
    def jsonSlurp

    /**
     * @return the core.TaskInProject List that is transformed
     */
    @Override
    List<TaskInProject> transform() {

        description="Dates transformed:\n"

        // DO NOTZING - just get the load
        return plc.taskList
    }

    def slurpAndCalc(String text) {
        try {
            def slurper = new JsonSlurper()
            def result = slurper.parseText(text)
            capaAvailable = calcCapa(result)
        } catch (VpipeDataException ve) {
            throw ve
        } catch (Exception e) {
            throw new VpipeDataException("Problem in JSON-Format von Datei $FILE_NAME:\n${e.getMessage()}")
        }
    }


    def reCalcCapa() {
        assert jsonSlurp
        capaAvailable = calcCapa(jsonSlurp)

    }

    Map<String, Map<String, YellowRedLimit>> calcCapa(def jsonSlurp) {
        this.jsonSlurp = jsonSlurp

        def fileErr = {"Fehler beim Lesen der Datei $FILE_NAME\n"}

        Map<String, Map<String, YellowRedLimit>> result = [:]

        def timeKeys = plc.getFullSeriesOfTimeKeys(WEEK)

        // get the public holiday of that week and create a percentage based on 5 days (5-h)/5 (ph)
        if( ! jsonSlurp.Kapa_Gesamt){throw new VpipeDataException("${fileErr()}Eintrag 'Kapa_Gesamt' fehlt.")}
        if( ! jsonSlurp.Kapa_Gesamt.Feiertage){throw new VpipeDataException("${fileErr()}Eintrag 'Feiertage' in 'Kapa_Gesamt' fehlt.")}
        List publicHolidays = jsonSlurp.Kapa_Gesamt.Feiertage

        // get the company percentage profile from holidayPercentProfile (ch)
        if( ! jsonSlurp.Kapa_Gesamt.Kapa_Profil){throw new VpipeDataException("${fileErr()}Eintrag 'Kapa_Profil' in 'Kapa_Gesamt' fehlt.")}
        Map percentProfile = jsonSlurp.Kapa_Gesamt.Kapa_Profil

        // create a map of year-week-strings with a percentage based  cp = ch * ph
        Map<String, Double> overallPercentageProfile = [:]
        for(week in timeKeys) {
            Double percentagePubHol = percentageLeftAfterPublicHoliday(week, publicHolidays)
            Double percentageProfile = (Double)(percentProfile[week] == null ? 1.0 : (Double)(percentProfile[week]/100))
            overallPercentageProfile[week] = percentagePubHol * percentageProfile
        }
        //println (overallPercentageProfile)

        // every department
        //println(jsonSlurp.Kapa_Abteilungen)
        Map departments = jsonSlurp.Kapa_Abteilungen
        if(!departments) {throw new VpipeDataException("${fileErr()}Kein Abschnitt 'Kapa_Abteilungen' definiert")}
        for(dep in departments) {
            if(dep.key == 'Konstruktion') {
                println()
            }
            Map<String, YellowRedLimit> capaMap = [:]
            // get the red and green limit
            if(! dep.value['Kapa']) {throw new VpipeDataException("${fileErr()}Kein Abschnitt 'Kapa' in Abteilung '$dep.key' definiert")}
            if(! dep.value['Kapa'].rot) {throw new VpipeDataException("${fileErr()}Kein Abschnitt 'rot' in 'Kapa' der Abteilung '$dep.key' definiert")}
            if(! dep.value['Kapa'].gelb) {throw new VpipeDataException("${fileErr()}Kein Abschnitt 'gelb' in 'Kapa' der Abteilung '$dep.key' definiert")}
            Double norm_red = dep.value['Kapa'].rot as Double
            Double norm_yellow = dep.value['Kapa'].gelb as Double
            //println("Norm-Kapa $dep.key yellow: $norm_yellow red: $norm_red")
            for (timeKey in timeKeys) {
                /*
                "Kapa_Profil": {
                    "2020-23": { "gelb": 140, "rot": 250 },
                    "2020-24": 100,
                    "2020-25": 80,
                    "2020-26": 100
                   }
                */
                def  p = 1.0
                // is a entry in the local Kapa_Profil. No? Then look for a cp --> value p other than 1
                //if(! dep.value['Kapa_Profil']) {throw new VpipeDataException("${fileErr()}Kein Abschnitt 'Kapa_Profil' in Abteilung '$dep.key' definiert")}
                def entry = dep.value['Kapa_Profil']?."$timeKey"
                if(entry) {
                    if (entry instanceof Number) {
                        p = entry / 100
                    } else {
                        // if there is a "increase or decreas of normal capa (gelb!=null) --> set new normal capa
                        norm_red = entry.rot
                        norm_yellow = entry.gelb
                    }
                } else {
                    p = overallPercentageProfile[timeKey]
                }

                // set yellow and red to p * normal
                capaMap[timeKey] = new YellowRedLimit(yellow: norm_yellow * p, red: norm_red * p)
            }
            //println("$dep.key $capaMap")
            result[(String)(dep.key)] = capaMap
        }
        result
    }

    Double percentageLeftAfterPublicHoliday(String week, List listOfPubHoliday) {
        def p = 1.0
        Date startOfWeek = week.toDateFromYearWeek()
        Date endOfWeek = startOfWeek + 5 // exclude Sa and Su
        for(String day in listOfPubHoliday) {
            def date = day.toDate()
            if(date >= startOfWeek && date < endOfWeek) {
                p -= 0.2
            }
        }
        p
    }

    @Override
    def updateConfiguration() {
        this.capaAvailable = [:]

            File f = new File(FILE_NAME)
            if(f.exists()) {
                slurpAndCalc(f.text)
            }
    }
}

