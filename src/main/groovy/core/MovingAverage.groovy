package core
/**
 * based on a series of doubles, calc a moving average.
 * default: use 5 values to calc average.
 *
 *     example 1:
 *     def ma = {-> [1, 2, 3, 4, 5, 6]}  as MovingAverage
 *     assert ma.getAverageValues() == [2, 3, 4, 5, 5.5, 6]
 *
 */
//@CompileStatic //tests fail
trait MovingAverage {

    int avgWindow = 3

    abstract List<Double> getBaseValues()

    List<Double> getAverageValues() {
        assert avgWindow >= 1, "avgWindow should be 1 or bigger"

        //assert getBaseValues()?.size() >= 0, "baseValues should exist"
        if( ! getBaseValues()) return []

        def startVal = 0.0
        def endVal = 0.0
        if (getBaseValues().size() > 0) {
            startVal = getBaseValues().get(0)
            endVal = getBaseValues().get(getBaseValues().size() - 1)
        }

        // add the starting number howMany times
        def startArray = new double[avgWindow]
        Arrays.fill(startArray, startVal)
        def startVals = startArray.toList();

        // add the ending number howMany times
        def endArray = new double[avgWindow]
        Arrays.fill(endArray, endVal)
        def endVals = endArray.toList()

        def allVals = [startVals, getBaseValues(), endVals].flatten()


        def result = new double[0].toList()
        for (idx in avgWindow..allVals.size() - 1 - avgWindow) {
            def subVals = allVals.subList(idx-avgWindow, idx + avgWindow + 1)
            result << subVals.sum() / subVals.size()
        }
        result
    }
}

