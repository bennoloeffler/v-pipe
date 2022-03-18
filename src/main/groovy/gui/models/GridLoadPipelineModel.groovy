package gui.models

import core.MovingAverage
import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import model.Model
import model.WeekOrMonth
import utils.RunTimer

import java.beans.PropertyChangeListener

import static extensions.DateHelperFunctions._getStartOfMonth
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.StringExtension.toDateFromYearWeek
import static model.WeekOrMonth.*

@CompileStatic
class GridLoadPipelineModel extends AbstractGridLoadModel {

    Model model

    double maxVal = 0

    int nowXRowCache = -1

    WeekOrMonth weekOrMonth = WEEK

    Map<String, GridLoadElement> gridElements = [:]

    PropertyChangeListener updateCallback = {
        updateAllFromModelData()
    }


    GridLoadPipelineModel(Model model, WeekOrMonth weekOrMonth = WEEK) {
        this.model = model
        this.weekOrMonth = weekOrMonth
        model.addPropertyChangeListener('updateToggle', updateCallback )
        updateAllFromModelData()
    }

    void updateAllFromModelData() {
        RunTimer.getTimerAndStart('GridPipelineLoadModel::updateAllFromModelData').withCloseable {
            calcGridElements()
            calcAverageValues()
            calcRowX()
        }
        setUpdateToggle(!getUpdateToggle())
    }


    void calcGridElements() {
        RunTimer.getTimerAndStart('GridPipelineLoadModel::calcGridElements').withCloseable {
            gridElements = [:]
            if (model.pipelineElements) {
                model.getFullSeriesOfTimeKeys(weekOrMonth).each { String timeStr ->
                    model.pipelineElements.each { element ->

                        Date start = toDateFromYearWeek(timeStr)
                        Date end = start + 7
                        long overlap = element.getDaysOverlap(start, end)
                        double capaNeeded = ((double) overlap / 7) * element.pipelineSlotsNeeded
                        //println("$overlap $capaNeeded $timeStr --> $element")
                        //if(capaNeeded > 0) {
                        if (gridElements[timeStr]) {
                            gridElements[timeStr].load += capaNeeded
                        } else {
                            gridElements[timeStr] = new GridLoadElement('', timeStr, capaNeeded, -1, model.maxPipelineSlots, model.maxPipelineSlots, [])
                        }
                        //}
                    }
                    maxVal = Math.max(gridElements[timeStr]?.load ?: 0, maxVal)
                }
            }
        }
    }

    @CompileDynamic
    private void calcRowX() {
        RunTimer.getTimerAndStart('GridPipelineLoadModel::calcRowX').withCloseable {
            nowXRowCache = -1
            if(weekOrMonth == WEEK) {
                Date startOfGrid = _getStartOfWeek(model.getStartOfProjects())
                Date endOfGrid = _getStartOfWeek(model.getEndOfProjects()) + 7
                Date now = new Date() // Date.newInstance()
                int row = 0
                for (Date w = startOfGrid; w < endOfGrid; w += 7) {
                    if (w <= now && now < w + 7) {
                        nowXRowCache = row
                    }
                    row++
                }
            } else {
                Date startOfGrid = _getStartOfMonth(model.getStartOfProjects())
                use(TimeCategory) {
                    Date endOfGrid = _getStartOfMonth(model.getEndOfProjects()) + 1.month
                    Date now = new Date() // Date.newInstance()
                    int row = 0
                    for (Date w = startOfGrid; w < endOfGrid; w += 1.month) {
                        if (w <= now && now < w + 1.month) {
                            nowXRowCache = row
                        }
                        row++
                    }
                }

            }
        }
    }

    @Override
    GridLoadElement getElement(int x, int y) {
        assert y == 0
        gridElements.values()[x]
    }

    @Override
    int getSizeY() {
        model.pipelineElements ? 1 : 0
    }

    @Override
    int getSizeX() {
        gridElements.values().size()
    }

    @Override
    int getNowX() {
        return nowXRowCache
    }

    @Override
    double getMaxValAndRed(int y) {
        return maxVal
    }

    @Override
    List<String> getYNames() {
        return model.pipelineElements ? ['IP-Belastung'] : []

    }

    @Override
    List<String> getXNames() {
        return model.getFullSeriesOfTimeKeys(weekOrMonth)
    }

    void calcAverageValues() {
        def ma = {
            gridElements.values()*.load
        } as MovingAverage
        ma.howMany = 5
        def maList = ma.getAverageValues()
        def i = 0
        gridElements.each {
            it.value.loadMovingAvg = maList[i++]
        }
    }


}
