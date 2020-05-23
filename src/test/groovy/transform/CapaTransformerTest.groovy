package transform

import core.VpipeException
import groovy.json.JsonSlurper

import static testdata.TestDataHelper.*

class CapaTransformerTest extends GroovyTestCase {

    void testSlurper() {
        def slurper = new JsonSlurper()
        def result = slurper.parseText('{"person":{"name":"Guillaume","age":33,"pets":["dog","cat"]}}')
        assert result.person.name == "Guillaume"
        assert result.person.age == 33
        assert result.person.pets.size() == 2
        assert result.person.pets[0] == "dog"
        assert result.person.pets[1] == "cat"
    }

    void testSlurperCapaFormat() {
        def text =
        """
                {
                 "Kapa_Gesamt": {
                  "Feiertage": [ "1.1.2020" ],
                  "Kapa_Profil": {
                   "2020-W01": 80,
                   "2020-W02": 90
                  }
                 },
                 
                 "Kapa_Abteilungen": {
                  
                  "Konstruktion": {
                   "Kapa": {
                    "gelb": 140,
                    "rot": 190
                   },
                   "Kapa_Profil": {
                    "2020-W04": { "gelb": 180, "rot": 220 }
                   }
                  },
                  
                  "Montage": {
                   "Kapa": {
                    "gelb": 240,
                    "rot": 490
                   },
                   "Kapa_Profil": {
                   "2020-W01": 100,
                   "2020-W02": 100
                   
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
        """


        def capa = slurpTextAndCalc(text)
        assert capa['IBN']['2020-W01'].yellow - 340 * 0.8 * 0.8 < 0.0001// pub holiday and 0.8 profile
        assert capa['IBN']['2020-W01'].red - 500 * 0.8 * 0.8 < 0.0001 // pub holiday and 0.8 profile
        assert capa['Konstruktion']['2020-W04'].red == 220 // increase
        assert capa['Konstruktion']['2020-W05'].red == 220 // increase the week before

        assert capa['Montage']['2020-W01'].yellow == 240
        assert capa['Montage']['2020-W02'].yellow == 240
        assert capa['Montage']['2020-W03'].yellow == 240
        assert capa['Montage']['2020-W04'].yellow == 240
        assert capa['Montage']['2020-W05'].yellow == 240



    }

    void testSlurperFormatErrors() {
        String text ="""
                {
                 "Kapa_Gesamt": {
                  "Feiertage": [ "1.1.2020" ],
                  "Kapa_Profil": {
                   "2020-W01": 8x0,
                   "2020-W02": 90
                  }
                 },
                 
                 "Kapa_Abteilungen": {
                  
                  "Konstruktion": {
                   "Kapa": {
                    "gelb": 140,
                    "rot": 190
                   },
                   "Kapa_Profil": {
                    "2020-W04": { "gelb": 180, "rot": 220 }
                   }
                  },
                  
                  "Montage": {
                   "Kapa": {
                    "gelb": 240,
                    "rot": 490
                   },
                   "Kapa_Profil": {
                   "2020-W01": 100,
                   "2020-W02": 100
                   
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
        """

        def msg = shouldFail (VpipeException) {
            def capa = slurpTextAndCalc(text)
            println(capa)
        }
        assert msg == "Problem in JSON-Format von Datei Abteilungs-Kapazitäts-Angebot.txt: unexpected character x"

        text = ""
        msg = shouldFail (VpipeException) {
            def capa = slurpTextAndCalc(text)
            println(capa)
        }
        assert msg == "Problem in JSON-Format von Datei Abteilungs-Kapazitäts-Angebot.txt: Text must not be null or empty"

        text = "{}"
        msg = shouldFail (VpipeException) {
            def capa = slurpTextAndCalc(text)
            println(capa)
        }
        assert msg == "Problem in JSON-Format von Datei Abteilungs-Kapazitäts-Angebot.txt: Cannot get property 'Feiertage' on null object"

        text = """{
                 "Kapa_Gesamt": {
                        "Feiertage": [ "1.1.2020" ],
                        "Kapa_Profil": {
                            "2020-W01": 80,
                            "2020-W02": 90
                        }
                 }
                 }
                """
        msg = shouldFail (VpipeException) {
            def capa = slurpTextAndCalc(text)
            println(capa)
        }
        assert msg == "Problem in JSON-Format von Datei Abteilungs-Kapazitäts-Angebot.txt: Keine Abteilungen definiert in Abteilungs-Kapazitäts-Angebot.txt"


    }

    def slurpTextAndCalc(String text) {
        def ct = new CapaTransformer(populatedCalculator)
        ct.slurpAndCalc(text)
    }

    void testTransform() {

    }

    void testUpdateConfiguration() {
    }
}
