package learn

import groovy.transform.CompileStatic

@CompileStatic
class LearnClojures {

    static void main(String[] args) {
        testContextMotherDefault()
        testContextWithWith("birth-arg")
    }

    static def property = "learn-clojure-property"

    static def method() { "learn-clojure-method" }

    static class Mother {

        def property = 'mother-property'

        def method() { "mother-method" }

        @CompileStatic
        Closure<Map> birth(birthArg) {
            def birthProperty = "birth-property"
            Closure<Map> closure = { closureArg ->
                def closureProperty = "closure-property"
                Map context = [this           : this,
                               closureProperty: closureProperty,
                               closureArg     : closureArg,
                               birthProperty  : birthProperty,
                               birthArg       : birthArg,
                               property       : property,
                               method         : method()]
            } as Closure<Map>
            closure
        }
    }

    static void testContextMotherDefault() {
        def m = new Mother()
        Closure<Map> c = m.birth("birth-arg")
        Map context = c.call("closure-arg")

        assert context.method == "mother-method"
        assert context.property == "mother-property"
        assert context.birthArg == "birth-arg"
        assert context.birthProperty == "birth-property"
        assert context.closureArg == "closure-arg"
        assert context.closureProperty == "closure-property"
        assert context.this == m

        assert c.thisObject == m
        assert c.owner == m
        assert c.delegate == m
        assert c.resolveStrategy == Closure.OWNER_FIRST
    }





    static void testContextWithWith(birthArg) {
        def m = [a: 17, b: 19]
        def context = [empy: "initial"]
        m.with {
            a = 19 // delegate to m
            context = [this           : this,
                       closureProperty: size(),
                       closureArg     : it,
                       birthProperty  : context,
                       birthArg       : birthArg,
                       property       : property,
                       method         : method(),
                       owner          : owner,
                       delegate       : delegate,
            resolveStrategy: resolveStrategy]
        }

        println context
    }

    // https://musketyr.medium.com/groovy-dsl-builders-1-the-concept-2d5a97fa0a51
    // TODO: @ClosureParams in @DelegatesTo
}
