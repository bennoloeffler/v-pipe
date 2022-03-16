package model

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

@ToString
@Immutable
@CompileStatic
class YellowRedLimit {
    double yellow
    double red
}