package test.compare

import java.util.Comparator
import kotlin.test.*
import org.junit.Test

class Item(val name: String, val rating: Int) {
    fun toString() = "Item($name, $rating)"
}

class CompareTest {
    val v1 = Item("wine", 9)
    val v2 = Item("beer", 10)

    Test fun compareByNameFirst() {
        val diff = compareBy(v1, v2, {(i: Item) -> i.name}, {(i: Item) -> i.rating})
        assertTrue(diff > 0)
    }

    Test fun compareByRatingFirst() {
        val diff = compareBy(v1, v2, {(i: Item) -> i.rating}, {(i: Item) -> i.name})
        assertTrue(diff < 0)
    }

    Test fun compareSameObjectsByRatingFirst() {
        val diff = compareBy(v1, v1, {(i: Item) -> i.rating}, {(i: Item) -> i.name})
        assertTrue(diff == 0)
    }

    Test fun sortUsingComparatorHelperMethod() {
        val c = comparator<Item>({it.rating}, {it.name})
        println("Created comparator $c")

        todo {
        // TODO needs KT-729 before this code works
            val diff = c.compare(v1, v2)
            assertTrue(diff < 0)
            val items = arrayList(v1, v2)
            items.sort(c)
            println("Sorted list in rating order $items")
        }
    }

    Test fun sortUsingCustomComparator() {
        val c = object : Comparator<Item>{
            public override fun compare(o1: Item?, o2: Item?): Int {
                return compareBy(o1, o2, {(it: Item) -> it.name}, {(it: Item) -> it.rating})
            }
            public override fun equals(obj: Any?): Boolean {
                return this == obj
            }
        }
        println("Created comparator $c")

        val diff = c.compare(v1, v2)
        assertTrue(diff > 0)
        val items = arrayList(v1, v2)
        items.sort(c)
        println("Sorted list in rating order $items")
    }

}