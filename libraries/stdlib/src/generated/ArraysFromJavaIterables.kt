// NOTE this file is auto-generated from src/kotlin/JavaIterables.kt
package kotlin

import kotlin.util.*

import java.util.*

/**
 * Returns *true* if any elements match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt any
 */
public inline fun <T> Array<T>.any(predicate: (T) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns *true* if all elements match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt all
 */
public inline fun <T> Array<T>.all(predicate: (T) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns the number of elements which match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt count
 */
public inline fun <T> Array<T>.count(predicate: (T) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/CollectionTest.kt find
 */
public inline fun <T> Array<T>.find(predicate: (T) -> Boolean) : T? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Filters all elements which match the given predicate into the given list
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterIntoLinkedList
 */
public inline fun <T, C: Collection<in T>> Array<T>.filterTo(result: C, predicate: (T) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotIntoLinkedList
 */
public inline fun <T, L: List<in T>> Array<T>.filterNotTo(result: L, predicate: (T) -> Boolean) : L {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Filters all non-*null* elements into the given list
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotNullIntoLinkedList
 */
public inline fun <T, L: List<in T>> Array<T?>?.filterNotNullTo(result: L) : L {
    if (this != null) {
        for (element in this) if (element != null) result.add(element)
    }
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 *
 * @includeFunctionBody ../../test/CollectionTest.kt flatMap
 */
public inline fun <T, R> Array<T>.flatMapTo(result: Collection<R>, transform: (T) -> Collection<R>) : Collection<R> {
    for (element in this) {
        val list = transform(element)
        if (list != null) {
            for (r in list) result.add(r)
        }
    }
    return result
}

/**
 * Performs the given *operation* on each element
 *
 * @includeFunctionBody ../../test/CollectionTest.kt forEach
 */
public inline fun <T> Array<T>.forEach(operation: (T) -> Unit) : Unit = for (element in this) operation(element)

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt fold
 */
public inline fun <T> Array<T>.fold(initial: T, operation: (T, T) -> T): T {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt foldRight
 */
public inline fun <T> Array<T>.foldRight(initial: T, operation: (T, T) -> T): T = reverse().fold(initial, operation)

/**
 * Transforms each element using the result as the key in a map to group elements by the result
 *
 * @includeFunctionBody ../../test/CollectionTest.kt groupBy
 */
public inline fun <T, K> Array<T>.groupBy(result: Map<K, List<T>> = HashMap<K, List<T>>(), toKey: (T) -> K) : Map<K, List<T>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<T>() }
        list.add(element)
    }
    return result
}

/** Returns a list containing the first elements that satisfy the given *predicate* */
public inline fun <T, L: List<in T>> Array<T>.takeWhileTo(result: L, predicate: (T) -> Boolean) : L {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Reverses the order the elements into a list
 *
 * @includeFunctionBody ../../test/CollectionTest.kt reverse
 */
public inline fun <T> Array<T>.reverse() : List<T> {
    val answer = LinkedList<T>()
    for (element in this) answer.addFirst(element)
    return answer
}

/** Copies all elements into the given collection */
public inline fun <T, C: Collection<T>> Array<T>.to(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/** Copies all elements into a [[LinkedList]] */
public inline fun <T> Array<T>.toLinkedList() : LinkedList<T> = to(LinkedList<T>())

/** Copies all elements into a [[List]] */
public inline fun <T> Array<T>.toList() : List<T> = to(ArrayList<T>())

/** Copies all elements into a [[Set]] */
public inline fun <T> Array<T>.toSet() : Set<T> = to(HashSet<T>())

/** Copies all elements into a [[SortedSet]] */
public inline fun <T> Array<T>.toSortedSet() : SortedSet<T> = to(TreeSet<T>())

/**
  TODO figure out necessary variance/generics ninja stuff... :)
public inline fun <in T> Array<T>.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
    val answer = this.toList()
    answer.sort(transform)
    return answer
}
*/
