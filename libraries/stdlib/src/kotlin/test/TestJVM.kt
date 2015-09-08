package kotlin.test

import java.util.ServiceLoader

/** Asserts that a [block] fails with a specific exception being thrown */
public fun <T: Throwable> failsWith(exceptionClass: Class<T>, block: ()-> Any): T {
    try {
        block()
        asserter.fail("Expected an exception to be thrown")
        throw IllegalStateException("Should have failed")
    } catch (e: T) {
        if (exceptionClass.isInstance(e)) {
            return e
        }
        throw e
    }
}

/**
 * Comments out a [block] of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
public inline fun todo(block: ()-> Any) {
    println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1))
}

private var _asserter: Asserter? = null

/**
 * The active implementation of [Asserter]. An implementation of [Asserter] can be provided
 * using the [Java service loader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) mechanism.
 */
public var asserter: Asserter
    get() {
        if (_asserter == null) {
            val klass = Asserter::class.java
            val loader = ServiceLoader.load(klass)
            _asserter = loader.firstOrNull { it != null } ?: DefaultAsserter()
            //debug("using asserter $_asserter")
        }
        return _asserter!!
    }

    set(value) {
        _asserter = value
    }


/**
 * Default [Asserter] implementation to avoid dependency on JUnit or TestNG.
 */
private class DefaultAsserter() : Asserter {

    public override fun assertEquals(message : String, expected : Any?, actual : Any?) {
        if (expected != actual) {
            fail("$message. Expected <$expected> actual <$actual>")
        }
    }

    override fun assertNotEquals(message : String, illegal: Any?, actual : Any?) {
        if (illegal == actual) {
            fail("$message. Illegal value: <$illegal>")
        }
    }


    public override fun fail(message : String) {
        throw AssertionError(message)
    }
}