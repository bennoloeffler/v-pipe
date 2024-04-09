package application

import core.MovingAverage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class MovingAverageTest extends Assertions {


    class MovingAverageMock implements MovingAverage {
        List<Double> getBaseValues() { [1, 2, 3, 4, 5, 6] }
    }
    MovingAverage longestMA = new MovingAverageMock()

    MovingAverage shorterMA = { [0, 3, 3, 6, 12, 24] } //as MovingAverage

    def maSimple = { -> [1, 1, 1, 1, 1, 1] } as MovingAverage
    def ma = { -> [1, 2, 3, 4, 5, 6] } as MovingAverage

    def maEmpty = { -> [] } as MovingAverage

    def maNull = { -> null } as MovingAverage

    @Test
    void testSimple() {
        maSimple.avgWindow = 2
        assert maSimple.getAverageValues() == [1, 1, 1, 1, 1, 1]
    }

    @Test
    void testGetResult() {
        ma.avgWindow = 1
        // [1, 2, 3, 4, 5, 6]
        assert ma.getAverageValues() == [(1.0+1+2)/3.0d, 2, 3, 4, 5, (5+6+6.0)/3.0d]
    }

    @Test
    void testGetResult2() {
        shorterMA.avgWindow = 20
        // [0, 3, 3, 6, 12, 24]
        assert shorterMA.getAverageValues() == [9.951219512195122, 10.536585365853659, 11.121951219512194, 11.707317073170731, 12.292682926829269, 12.878048780487806]
    }

    @Test
    void testGetResultFour() {
        shorterMA.avgWindow = 2
        //[0, 3, 3, 6, 12, 24] [0+0+0+3+3/5, 0+0+3+3+6/5, 0+3+3+6+12/5, 3+3+6+12+24/5, 3+6+12+24+24/5, 6+12+24+24+24/5]
        assert shorterMA.getAverageValues() == [(0+0+0+3+3)/5.0d, (0+0+3+3+6)/5.0d, (0+3+3+6+12)/5.0d, (3+3+6+12+24)/5.0d, (3+6+12+24+24)/5.0d, (6+12+24+24+24)/5.0d]
        //[1.2, 2.4, 4.8, 9.6, 13.8, 18.0]
    }

    @Test
    void testGetResultHowMany10() {
        shorterMA.avgWindow = 6
        shorterMA.getAverageValues()
    }

    @Test
    void testGetResultHowMany0() {
        shorterMA.avgWindow = 0
        assertThrows (Throwable.class) { shorterMA.getAverageValues() }
    }

    @Test
    void testGetResultHowManyHuge() {
        longestMA.avgWindow = 100
        longestMA.getAverageValues()
    }

    @Test
    void testGetResultEmpty() {
        assert maEmpty.getAverageValues() == []
    }

    @Test
    void testGetResultNull() {
        assert maNull.getAverageValues() == []
    }

}
