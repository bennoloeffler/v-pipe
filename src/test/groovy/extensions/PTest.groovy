package extensions

import groovy.transform.Immutable
import org.pcollections.PMap
import org.pcollections.PVector
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

import static extensions.P.*

class PTest extends Specification {

    def "updateIn vec"() {

        given:
        final simpleVecData1 = pVec([1, 2, 3])

        when:
        final simpleVecData2 = updateIn(simpleVecData1, [1], { 5 * it })

        then:
        simpleVecData1[0] == 1
        simpleVecData1[1] == 2
        simpleVecData1[2] == 3
        simpleVecData2.size() == 3

        simpleVecData2[0] == 1
        simpleVecData2[1] == 10
        simpleVecData2[2] == 3
        simpleVecData2.size() == 3
    }


    def "updateIn map"() {

        given:
        final simpleMapData1 = pMap([a: 1, b: 2, c: 3])

        when:
        final simpleMapData2 = updateIn(simpleMapData1, ["a"], { it + 5 })

        then:
        simpleMapData1 == pMap([a: 1, b: 2, c: 3])
        simpleMapData2["a"] == 6
        simpleMapData2.size() == 3
    }


    def "updateIn vec vec vec"() {
        given:
        def nestedVecData =
                pVec([pVec([1, 2, 3]),
                      pVec([4, 5, pVec([6, 7, 8])])])

        when:
        def updated = updateIn(nestedVecData, [1, 2, 2], { it * 3 })

        then:
        updated[1][2][2] == 24
    }


    def "updateIn map map map"() {
        given:
        def nestedMapData = pMap(
                a: pMap([b: 1, c: 2, d: 3]),
                e: pMap([f: 4,
                         g: 5,
                         h: pMap([i: 6, j: 7, k: 8])]))
        when:
        def updated = updateIn(nestedMapData, ["e", "h", "k"], { it * 5 })

        then:
        updated["e"]["h"]["k"] == 40
    }


    def "assocIn map vec map"() {
        given:
        def vecMapMixedData =
                pMap([name    : "Benno",
                      age     : 53,
                      children: pVec([pMap([name: "Benno", age: 22]),
                                      pMap([name: "Paul", age: 18]),
                                      pMap([name: "Leo", age: 15])]),
                      steet   : "Adlerstr. 46",
                      born    : new Date()])
        when:
        def updated = assocIn(vecMapMixedData, ["children", 2, "age"], 16)

        then:
        updated["children"][2]["age"] == 16
    }


    def "dissoc key from map vec map"() {
        given:
        def vecMapMixedData =
                pMap([name    : "Benno",
                      age     : 53,
                      children: pVec([pMap([name: "Benno", age: 22]),
                                      pMap([name: "Paul", age: 18]),
                                      pMap([name: "Leo", age: 15])]),
                      steet   : "Adlerstr. 46",
                      born    : new Date()])
        when:
        def updated = updateIn(vecMapMixedData, ["children", 2], { it.minus("age") })

        then:
        updated["children"][2]["age"] == null
    }

    def "assocIn vec"() {
        given:
        def vecData = pVec([1, 2, 3, pVec([4, 5, pVec([6, 7, 8])])])

        when:
        def updated = assocIn(vecData, [3, 2, 2], 16) // 8 to 16

        then:
        updated[3][2][2] == 16
    }

    def "removeIn mixed"() {
        given:
        def vecMapMixedData =
                pMap([name    : "Benno",
                      age     : 53,
                      children: pVec([pMap([name: "Benno", age: 22]),
                                      pMap([name: "Paul", age: 18]),
                                      pMap([name: "Leo", age: 15])]),
                      street  : "Adlerstr. 46",
                      born    : new Date()])
        when:
        def updated = removeIn(vecMapMixedData, ["children", 2, "age"])
        updated = removeIn(updated, ["children", 0])
        updated = removeIn(updated, ["children", 0])
        updated = removeIn(updated, ["age"])
        updated = removeIn(updated, ["street"])
        updated = removeIn(updated, ["born"])

        then:
        updated["children"][0]["age"] == null
        updated["children"][0]["name"] == "Leo"
        updated["children"].size() == 1
        updated.name == "Benno"
        updated.size() == 2
    }

    static class Person {
        String name = "Original"
    }

    @Immutable
    static class PersonImmutable {
        String name = "Original"
    }

    def "change (mutable) object in pVec"() {
        given:
        def listOverwritten = [1, 2, 3, new Person()]
        def vecDataOverwritten = pVec(listOverwritten)
        def list = [1, 2, 3, new PersonImmutable()]
        def vecData = pVec(list)

        when:
        def updatedOverwritten = updateIn(vecDataOverwritten, [3], { Person p -> p.name = "Overwritten"; p }) // 8 to 16
        def updated = updateIn(vecData, [3], { new PersonImmutable(name: "Overwritten: $it.name") })

        then:

        // ATTENTION: PCollections with mutable Objects are ease to change...
        listOverwritten[3].name == "Overwritten" // NOT "Original"
        vecDataOverwritten[3].name == "Overwritten" // NOT "Original"
        updatedOverwritten[3].name == "Overwritten"

        // SOLUTION: @Immutable
        list[3].name == "Original"
        vecData[3].name == "Original"
        updated[3].name == "Overwritten: Original"
    }


    def "test swap"() {
        given:
        AtomicReference a = new AtomicReference<PMap>(
                pMap([a: pMap([b: 1, c: 2, d: 3]),
                      e: pMap([f: 4,
                               g: 5,
                               h: pMap([i: 6, j: 7, k: 0]),
                               i: pMap([l: 9, m: 10])])]))

        when:
        swap(a, { updateIn(a.get(), ["e"], { it.minus("g") }) })
        swap(a, { removeIn(a.get(), ["e", "i", "l"]) })
        swap(a, { a.get().plus("a", "something else") })
        def threads = (0..99).collect {
            Thread.start {
                swap(a, { updateIn(a.get() as PMap, ["e", "h", "k"], { n -> n + 2 }) })
            }
        }
        threads.each { it.join() }

        then:
        a.get()["e"]["h"]["k"] == 200
        a.get()["a"] == "something else"
        a.get()["e"]["f"] == 4
        a.get()["e"]["g"] == null
        //println "collisions: " + collisions
    }

    def "test delete too deep map"() {
        given:
        def p = p([:])
        when:
        def r = removeIn(p, [1, "a", 4])
        then:
        thrown(Exception)
    }

    def "test delete too deep vec"() {
        given:
        def p = p([a: [0, 1, 2, 3]])
        when:
        def r = removeIn(p, ["a", 4])
        then:
        thrown(Exception)
    }

    /**
     * this model shows 3 elements:
     * - a persistent data using PCollections model held by an AtomicReference
     * - an API, that relies on P helper functions inspired by clojure
     * - todo: rxgroovy to make it reactive
     *   https://github.com/Petikoch/Java_MVVM_with_Swing_and_RxJava_Examples
     */
    static class ReactiveModel {

        AtomicReference familyTree = new AtomicReference(pMap([:]))

        PMap getFamilyTree() { familyTree.get() }

        def init() {
            swap(familyTree, {
                p(
                        [Benno:
                                 [//parents        : [:],
                                  children: [Benno: [children: [:]],
                                             Paul : [children: [:]],
                                             Leo  : [children: [:]]],
                                  //characteristics: ["talks much", "lazy"]
                                 ]
                        ]
                )
            })
        }

        def addChildlessPersonIfMissing(String name) {
            if (!getFamilyTree()[name]) {
                swap(familyTree) {
                    assoc(familyTree, name, [children: [:]])
                }
                return true
            }
            return false
        }

        def addChildren(String name, String childsName) {
            swap(familyTree, {
                def r = updateIn(familyTree, [name, "children"], {
                    PMap m ->
                        m.plus(childsName, [:])
                })
                r
            })
        }

        def addGrandChild(String name, String childsName, String grandChildsName) {
            swap(familyTree, {
                def r = assocIn(familyTree, [name, "children", childsName, "children"], p([(grandChildsName): [children: [:]]]))
                r
            })
        }
    }

    def "test model"() {
        given:
        def m = new ReactiveModel()

        when:
        m.init()
        m.addChildlessPersonIfMissing("Sabine")
        m.addChildlessPersonIfMissing("Kuni")
        m.addChildlessPersonIfMissing("Hans")
        m.addChildren("Kuni", "Max")
        m.addGrandChild("Kuni", "Max", "Moritz")

        then:
        m.getFamilyTree()["Benno"].size() == 1
        m.getFamilyTree().size() == 4
        //println m.getFamilyTree()
    }

    def "test functional"() {
        // collect = map
        // inject  = reduce
        // findAll = filter
        given:
        def increase = { it * 77 }
        def by15or23 = { (it % 15 == 0 || it % 23 == 0) }
        PVector startWith = p(1..10)

        when:
        def result =
                p( // create a PersistentCollection at the end
                startWith // this is a PVector
                .collect(increase) //map (intermediates are NOT PCollections)
                .findAll(by15or23)) //filter
                //.inject(10000000, { acc, val -> acc - val })

        then:
        result instanceof PVector
    }

    def "test reactive"(){
        given:
        1
        when:
        1
        then:
        1
    }

}

