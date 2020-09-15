package newview


/**
 * base model for grid - this one is for manual testing
 */
class GridDemoModel extends GridModel {

    List <List<String>> data =
            [
                    ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.mont',     'p1.mont.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         '',         'p2.k',     'p2.mont',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         'p3.k',     'p3.t',     't3.mont.i',       'p3.ibn.i',     'p3.ibn',           '',             '',         '',         '',         '',         ''      ],
                    ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.mont',     'p4.mont',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                    ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.mont',     'p1.mont.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         '',         'p2.k',     'p2.mont',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         'p3.k',     'p3.t',     't3.mont.i',       'p3.ibn.i',     'p3.ibn',           '',             '',         '',         '',         '',         ''      ],
                    ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.mont',     'p4.mont',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                    ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.mont',     'p1.mont.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         '',         'p2.k',     'p2.mont',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         'p3.k',     'p3.t',     't3.mont.i',       'p3.ibn.i',     'p3.ibn',           '',             '',         '',         '',         '',         ''      ],
                    ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.mont',     'p4.mont',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                    ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.mont',     'p1.mont.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         '',         'p2.k',     'p2.mont',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                    ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.mont',     'p4.mont',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                    ['',        '',         '',         '',         '',         '',         '',             'p4.k',         'p4.mont',             'p4.mont',         'p4.ibn',   'p4.ibn.i', 'p4.p',     'p4.p',     'p4.p'  ]
            ]

    /**
     *
     * @param x grid coordinates
     * @param y grid coordinates
     * @return an element of the grid (GridElement nullElement - if there is no element in the grid)
     */
    GridElement getElement(int x, int y) {
        assert y < data.size()
        assert x < data[y].size()
        def d = data[y][x]
        List<String> split = d.split(/\./)
        split.removeAll('')
        def size = split.size()
        if (size) {
            assert   1 < split.size() && split.size() <= 3 // 2 or 3
            return new GridElement(project: split[0], department: split[1], integrationPhase: split.size() == 3)
        } else {
            GridElement.nullElement
        }
    }


    /**
     * @return heigth or Y size of the grid
     */
    int getSizeY() {data.size()}


    /**
     * @return width or X size of the grid
     */
    int getSizeX() {data*.size().max()}


    /**
     * move the complete line in line y to the left
     * @param y line
     */
    def moveLeft(int y) {
        if( ! data[y][0]) {
            data[y].remove(0)
            data[y].add('')
        } else {
            for (line in 0..data.size()-1) {
                if(y==line) {
                    data[line].add('')
                } else {
                    data[line].add(0, '')
                }
            }
        }
    }


    /**
     * move the complete line in line y to the right
     * @param y line
     */
    def moveRight(int y) {

        if( ! data[y][data[y].size()-1]) {
            data[y].remove(data[y].size()-1)
            data[y].add(0, '')
        } else {
            for (line in 0..data.size()-1) {
                if(y==line) {
                    data[line].add(0, '')
                } else {
                    data[line].add('')
                }
            }
        }
    }

    @Override
    def toggleIntegrationPhase(int x, int y) {
        assert y < data.size()
        assert x < data[y].size()
        def d = data[y][x]
        if (d) {
            def end = d[-2..-1]
            if (end == '.i') {
                data[y][x] = d[0..-3]
            } else {
                data[y][x] = d + '.i'
            }
        }
        //e != GridElement.nullElement ? e.integrationPhase = !e.integrationPhase : ''
    }

    @Override
    def swap(int y, int withY) {
        data.swap(y, withY)
    }

    @Override
    def setSelectedElement(int x, int y) {

    }

    @Override
    int getNowX() {
        data[0].size() / 2
    }

    @Override
    List<String> getLineNames() {
        def r  = []
        data.size().times {r << it.toString()*20}
        r
    }

    @Override
    List<String> getColumnNames() {
        def r  = []
        data[0].size().times {r << it.toString()*20}
        r
    }

    @Override
    List<String> getDetailsForTooltip(int x, int y) {
        ["dummy", "0"]
    }
}
