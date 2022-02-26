package application

import groovy.transform.CompileStatic

/**
 * based on a series of doubles, calc a moving average.
 * default: use 3 values to calc average.
 *
 *     example 1:
 *     def ma = {-> [1, 2, 3, 4, 5, 6]}  as MovingAverage
 *     assert ma.getAverageValues() == [2, 3, 4, 5, 5.5, 6]
 *
 *     example 2:
 */
//@CompileStatic //otherwise tests fail
trait MovingAverage {

    int howMany = 5

    abstract List<Double> getBaseValues()

    List<Double> getAverageValues() {
        assert howMany > 1, "howMany should be bigger than 1"
        //assert howMany <= baseValues.size() // not needed...
        def result = []
        def allVals = getBaseValues()
        def s = allVals.size()
        if(s) {
            for (idx in 0..s - 1) {
                def subVals = allVals.subList(idx, Math.min(idx + howMany, s))
                result << subVals.sum() / subVals.size()
            }
        }
        result as List<Double>
    }
}

