fun foo() {
    class C {
        <!CONFLICTING_PLATFORM_DECLARATIONS!>val x = 1<!>
        <!CONFLICTING_PLATFORM_DECLARATIONS!>fun getX() = 1<!>
    }
}