package application

import core.AbsoluteLoadCalculator
import core.CapaNeedDetails
import groovy.beans.Bindable
import model.Model
import model.WeekOrMonth
import newview.AbstractGridLoadModel
import newview.GridLoadElement
import utils.RunTimer

import java.beans.PropertyChangeEvent

import static extensions.DateHelperFunctions._getStartOfWeek

class GridPipelineLoadModel extends AbstractGridLoadModel  {

    Model model

    double maxVal = 0

    int nowXRowCache = -1

    WeekOrMonth weekOrMonth = WeekOrMonth.WEEK

    Map<String, GridLoadElement> gridElements = [:]

    def updateCallback = {
        updateAllFromModelData()
    }

    GridPipelineLoadModel(Model model, WeekOrMonth weekOrMonth = WeekOrMonth.WEEK) {
        this.model = model
        this.weekOrMonth = weekOrMonth
        model.addPropertyChangeListener('updateToggle', updateCallback)
        updateAllFromModelData()
    }

    void updateAllFromModelData() {
        RunTimer.getTimerAndStart('GridPipelineLoadModel::updateAllFromModelData').withCloseable {
            calcGridElements()
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

                        Date start = timeStr.toDateFromYearWeek()
                        Date end = start + 7
                        long overlap = element.getDaysOverlap(start, end)
                        double capaNeeded = ((double) overlap / 7) * element.pipelineSlotsNeeded
                        //println("$overlap $capaNeeded $timeStr --> $element")
                        //if(capaNeeded > 0) {
                        if (gridElements[timeStr]) {
                            gridElements[timeStr].load += capaNeeded
                        } else {
                            gridElements[timeStr] = new GridLoadElement('', timeStr, capaNeeded, 0, model.maxPipelineSlots, model.maxPipelineSlots, [])
                        }
                        //}
                    }
                    maxVal = Math.max(gridElements[timeStr]?.load ?: 0, maxVal)
                }
            }
        }
    }

    private void calcRowX() {
        RunTimer.getTimerAndStart('GridPipelineLoadModel::calcRowX').withCloseable {
            nowXRowCache = -1
            Date startOfGrid = _getStartOfWeek(model.getStartOfTasks())
            Date endOfGrid = _getStartOfWeek(model.getEndOfTasks()) + 7

            Date now = new Date() // Date.newInstance()
            int row = 0
            for (Date w = startOfGrid; w < endOfGrid; w += 7) {
                if (w <= now && now < w + 7) {
                    nowXRowCache = row
                }
                row++
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
        return  model.pipelineElements ? ['IP-Belastung'] : []

    }

    @Override
    List<String> getXNames() {
        return model.getFullSeriesOfTimeKeys(weekOrMonth)
    }
}
