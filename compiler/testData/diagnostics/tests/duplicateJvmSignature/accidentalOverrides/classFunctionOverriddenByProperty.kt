open class B {
    fun getX() = 1
}

class C : B() {
    val x: Int
        <!CONFLICTING_PLATFORM_DECLARATIONS!>get() = 1<!>
}