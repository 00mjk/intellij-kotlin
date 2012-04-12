package kotlin

import java.util.Map as JMap
import java.util.HashMap
import java.util.Collections

// Temporary workaround: commenting out
//import java.util.Map.Entry as JEntry

// Map APIs

/** Returns the size of the map */
val JMap<*,*>.size : Int
get() = size()

/** Returns true if this map is empty */
val JMap<*,*>.empty : Boolean
get() = isEmpty()

/** Provides [] access to maps */
public fun <K, V> JMap<K, V>.set(key : K, value : V) : V? = this.put(key, value)

/** Returns the [[Map]] if its not null otherwise it returns the empty [[Map]] */
public inline fun <K,V> java.util.Map<K,V>?.orEmpty() : java.util.Map<K,V>
= if (this != null) this else Collections.EMPTY_MAP as java.util.Map<K,V>


/** Returns the key of the entry */
// Temporary workaround: commenting out
//val <K,V> JEntry<K,V>.key : K
//    get() = getKey().sure()

/** Returns the value of the entry */
// Temporary workaround: commenting out
//val <K,V> JEntry<K,V>.value : V
//    get() = getValue().sure()

/**
 * Returns the value for the given key or returns the result of the defaultValue function if there was no entry for the given key
 *
 * @includeFunctionBody ../../test/MapTest.kt getOrElse
 */
public inline fun <K,V> java.util.Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V {
    val current = this.get(key)
    if (current != null) {
        return current
    } else {
        return defaultValue()
    }
}

/**
 * Returns the value for the given key or the result of the defaultValue function is put into the map for the given value and returned
 *
 * @includeFunctionBody ../../test/MapTest.kt getOrElse
 */
public inline fun <K,V> java.util.Map<K,V>.getOrPut(key: K, defaultValue: ()-> V) : V {
    val current = this.get(key)
    if (current != null) {
        return current
    } else {
        val answer = defaultValue()
        this.put(key, answer)
        return answer
    }
}


/**
 * Returns an [[Iterator]] over the entries in the [[Map]]
 *
 * @includeFunctionBody ../../test/MapTest.kt iterate
 */
public inline fun <K,V> java.util.Map<K,V>.iterator(): java.util.Iterator<java.util.Map.Entry<K,V>> {
    val entrySet = this.entrySet()!!
    return entrySet.iterator()!!
}