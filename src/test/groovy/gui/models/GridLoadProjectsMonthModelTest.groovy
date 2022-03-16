package gui.models


import model.Model
import model.WeekOrMonth
import spock.lang.Specification

import static testdata.TestDataHelper.getPopulatedModel

class GridLoadProjectsMonthModelTest extends Specification {

    Model model
    GridLoadProjectsModel pModel

    def "CalcGridElements"() {
        given:
        model = getPopulatedModel() // TODO a test with capa available - because we need month
        pModel = new GridLoadProjectsModel(model, WeekOrMonth.MONTH)

        when:
        //pModel.absoluteLoadCalculator = new AbsoluteLoadCalculator(model.taskList, WeekOrMonth.MONTH)
        pModel.updateAllFromModelData()


        then:
        pModel.getXNames() == ["2020-M01", "2020-M02"]
        pModel.getYNames() == ["d1", "d2"]
        pModel.gridElements["d1"]["2020-M01"].load == 40
        //println pModel.gridElements["d1"]["2020-M02"].toString()
        //println pModel.gridElements["d2"]["2020-M01"].toString()
        pModel.gridElements["d2"]["2020-M02"].load == 5

    }

    def "GetElement"() {
    }

    def "GetSizeY"() {
    }

    def "GetSizeX"() {
    }

    def "GetYNames"() {
    }

    def "GetXNames"() {
    }
}
