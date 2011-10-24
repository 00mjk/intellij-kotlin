package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class FunctionGenTest extends CodegenTestCase {
    public void testDefaultArgs() throws Exception {
        blackBoxFile("functions/defaultargs.jet");
        System.out.println(generateToText());
    }

    public void testNoThisNoClosure() throws Exception {
        blackBoxFile("functions/nothisnoclosure.jet");
        System.out.println(generateToText());
    }

    public void testAnyToString () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.toString()");
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertEquals("something", foo.invoke(null, "something"));
        assertEquals("null", foo.invoke(null, new Object[]{null}));

    }

    public void testAnyEqualsNullable () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.equals(\"lala\")");
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testAnyEquals () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.equals(\"lala\")");
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }
}
