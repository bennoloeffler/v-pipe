package application


class MovingAverageTest extends GroovyTestCase {


    class MovingAverageMock implements MovingAverage {
        List<Double> getBaseValues() { [1, 2, 3, 4, 5, 6] }
    }
    MovingAverage longestMA = new MovingAverageMock()

    MovingAverage shorterMA = { [0, 3, 3, 6, 12, 24] } //as MovingAverage

    def ma = { -> [1, 2, 3, 4, 5, 6] } as MovingAverage

    void testGetResult() {
        ma.howMany = 3
        assert ma.getAverageValues() == [2, 3, 4, 5, 5.5, 6]
    }

    void testGetResult2() {
        shorterMA.howMany = 3
        assert shorterMA.getAverageValues() == [2.0, 4, 7, 14, 18, 24]
    }

    void testGetResultFour() {
        shorterMA.howMany = 2
        assert shorterMA.getAverageValues() == [1.5, 3, 4.5, 9, 18, 24]
    }

    void testGetResultHowMany10() {
        shorterMA.howMany = 6
        shorterMA.getAverageValues()
    }

    void testGetResultHowMany0() {
        shorterMA.howMany = 1
        shouldFail { shorterMA.getAverageValues() }
    }

    void testGetResultHowManyHuge() {
        longestMA.howMany = 100
        longestMA.getAverageValues() // works...
    }
}
