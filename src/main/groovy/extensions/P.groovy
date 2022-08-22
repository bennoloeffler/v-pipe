package extensions

import org.pcollections.HashTreePMap
import org.pcollections.PMap
import org.pcollections.PVector
import org.pcollections.TreePVector

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * clojure inspired helper for nested persistent PCollections (PMap and PVector):
 *   assoc-in ("set new element down a associative key paths down to the element")
 *   update-in ("set new element down a associative key paths down to the element")
 *   swap (e.g. for atomic change to a PCollection-tree by a closure)
 *
 * @see
 *  class PTest @see https://github.com/hrldcpr/pcollections
 *  Collections https://docs.oracle.com/javase/8/docs/technotes/guides/collections/overview.html
 *  Collections https://www.falkhausen.de/Java-8/java.util/Collection-Hierarchy.html
 *  atomic without locking https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html
 *  clojure swap https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Atom.java#L33
 */
class P {

    /**
     * shorthand for creating a PVector from a Collection (SHALLOW ONLY)
     * @param c the Collection
     * @return PVector (impl: TreePVector)
     */
    static PVector pVec(Collection collection) { TreePVector.from(collection) }

    /**
     * shorthand for creating a PMap from a Map (SHALLOW ONLY)
     * @param map the Map
     * @return PMap
     */
    static PMap pMap(Map map) { HashTreePMap.from(map) }

    /**
     * helper for recursive calls to p - at the leaves, just return leaves.
     * @param v
     * @return
     */
    static Object p(Object v) {v}

    /**
     * shorthand for creating PCollection tree (DEEP)
     * Uses PMap (transformation of Map) and
     * PVector (transformation of List) only.
     * @param collection
     * @return the transformed PCollection tree
     */
    static PVector p(Collection collection) {
        pVec(collection.collect() { elem ->
            if (elem instanceof List) {
                pVec(elem)
            } else if (elem instanceof Map) {
                pMap(elem)
            } else {
                elem
            }
        })
    }


    /**
     * @see P::p(Collection collection)
     */
    static PMap p(Map map) {
        pMap(map.collectEntries { key, value ->
            if (value instanceof List) {
                [key, pVec(value)]
            } else if (value instanceof Map) {
                [key, pMap(value)]
            } else {
                // todo: check, if Mutable object...
                // meaning: something else than:
                // String, int, double, ... and NOT Immutable
                [key, value]
            }
        })
    }


    /**
     * assoc(iate) a int key (index) with an  value
     * @param pv the PVector
     * @return Vector
     */
    static PVector assoc(PVector pv, int k, v) { pv.with(k, p(v)) }

    /**
     * assoc(iate) a Object key with an value
     * @param pm the PMap
     * @return PMap
     */
    static PMap assoc(PMap pm, k, v) { pm.plus(k, p(v)) }

    /**
     * finds associated value of a chain of keys in a nested persistent
     * structure and updates the associated value of the final key by applying
     * fun to it.
     * @param col the original col (PVector)
     * @param keys chain of keys
     * @param fun transformation function {val -> (2* (it + 4)}* @return the adapted PVector
     */
    static PVector updateIn(PVector col, List keys, Closure fun) {
        //println "VEC:  " + col
        //println "keys: " + keys
        if (keys.size() > 1) {
            def res = updateIn(col[keys[0]], keys.drop(1), fun)
            assoc(col, keys[0], res)
        } else {
            def newVal = fun(col[keys[0]])
            //def res = col.plus(keys[0], newVal) // resolves to groovy GDK
            def res = assoc(col, keys[0], newVal)
            res
        }
    }

    /**
     * @see P::updateIn(PVector col, List keys, Closure fun)
     */
    static PMap updateIn(PMap map, List keys, Closure fun) {
        //println "MAP:  " + col
        //println "keys: " + keys
        if (keys.size() > 1) {
            if (map[keys[0]] != null) {
                def res = updateIn(map[keys[0]], keys.drop(1), fun)
                assoc(map, keys[0], res)
            } else {
                throw new RuntimeException("key does not exist: ${keys[0]}\nin PMap: ${map}")
            }
        } else {
            def newVal = fun(map[keys[0]])
            //def res = ((PMap) col).plus(keys[0], newVal) // resolves to groovy GDK
            def res = assoc(map, keys[0], newVal)
            res
        }
    }


    /**
     * finds associated value of a chain of keys in a nested persistent
     * structure col and associates value to last key (which may be an index).
     * @param col
     * @param keys
     * @param value
     * @return the col
     */
    static PVector assocIn(PVector col, List keys, Object value) { //{ updateIn(col, keys, { value }) }
        updateIn(col, keys) {
            if (value instanceof List || value instanceof Map) {
                p(value)
            } else {
                value
            }
        }
    }

    /**
     * @see P::assocIn(PVector col, List keys, Object value)
     * @param col
     * @param keys
     * @param value
     * @return
     */
    static PMap assocIn(PMap col, List keys, Object value) {
        updateIn(col, keys) {
            if (value instanceof List || value instanceof Map) {
                p(value)
            } else {
                value
            }
        }
    }


    /**
     * removes the last key of keys in a nested structure of PCollections
     * @param col
     * @param keys
     * @return col without last key of keys
     */
    static PVector removeIn(PVector col, List keys) {
        if (keys.size() > 1) {
            if (col[keys[0]] != null) {
                def res = removeIn(col[keys[0]], keys.drop(1))
                assoc(col, keys[0], res)
            } else {
                throw new RuntimeException("index does not exist: ${keys[0]}\nin PVec: ${col}")
            }
        } else {
            def res = col.minus(keys[0])
            res
        }
    }

    /**
     * @see P::removeIn(PVector col, List keys)
     */
    static PMap removeIn(PMap col, List keys) {
        if (keys.size() > 1) {
            if (col[keys[0]] != null) {
                def res = removeIn(col[keys[0]], keys.drop(1))
                assoc(col, keys[0], res)
            } else {
                throw new RuntimeException("key does not exist: ${keys[0]}\nin PMap: ${col}")
            }
        } else {
            def res = col.minus(keys[0])
            res
        }
    }


    /**
     * counter of collisions caused by swap
     */
    static AtomicInteger collisions = new AtomicInteger(0)


    /**
     * atomically change contents by ar by c
     * *******************************
     * ** inspired by clojures swap! **
     * *******************************
     * @see https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Atom.java#L33
     * @param ar
     * @param closure
     * @return the object in ar
     */
    static Object swap(AtomicReference ar, Closure closure) {
        for (; ;) {
            Object prev = ar.get();
            Object next = closure(prev);
            //validate(newv);
            if (ar.compareAndSet(prev, next)) {
                //notifyWatches(v, newv);
                return next;
            } else {
                collisions.getAndIncrement()
                //println "collision: " + collisions.get()
            }
        }
    }

}
