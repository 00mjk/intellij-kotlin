trait T {
    val x: Int
        <!CONFLICTING_PLATFORM_DECLARATIONS!>get()<!> = 1
    <!CONFLICTING_PLATFORM_DECLARATIONS!>fun getX()<!> = 1
}