package org.jetbrains.jet.codegen;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex.tkachman
 */
public class FunctionGenTest extends CodegenTestCase {
    public void testDefaultArgs() throws Exception {
        blackBoxFile("functions/defaultargs.jet");
//        System.out.println(generateToText());
    }

    public void testDefaultArgs1() throws Exception {
        blackBoxFile("functions/defaultargs1.jet");
//        System.out.println(generateToText());
    }

    public void testDefaultArgs2() throws Exception {
        blackBoxFile("functions/defaultargs2.jet");
//        System.out.println(generateToText());
    }

    public void testDefaultArgs3() throws Exception {
        blackBoxFile("functions/defaultargs3.jet");
//        System.out.println(generateToText());
    }

    public void testDefaultArgs4() throws Exception {
        blackBoxFile("functions/defaultargs4.jet");
//        System.out.println(generateToText());
    }

    public void testDefaultArgs5() throws Exception {
        blackBoxFile("functions/defaultargs5.jet");
//        System.out.println(generateToText());
    }

    public void testNoThisNoClosure() throws Exception {
        blackBoxFile("functions/nothisnoclosure.jet");
//        System.out.println(generateToText());
    }

    public void testAnyEqualsNullable () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any?) = x.equals(\"lala\")");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testAnyEquals () throws InvocationTargetException, IllegalAccessException {
        loadText("fun foo(x: Any) = x.equals(\"lala\")");
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, "lala"));
        assertFalse((Boolean) foo.invoke(null, "mama"));
    }

    public void testKt395 () {
        blackBoxFile("regressions/kt395.jet");
    }

    public void testKt785 () {
//        blackBoxFile("regressions/kt785.jet");
    }

    public void testKt873 () {
        blackBoxFile("regressions/kt873.kt");
    }

    public void testKt1199 () {
        blackBoxFile("regressions/kt1199.kt");
    }

    public void testFunction () throws InvocationTargetException, IllegalAccessException {
        blackBoxFile("functions/functionExpression.jet");
    }

    public void testLocalFunction () throws InvocationTargetException, IllegalAccessException {
        blackBoxFile("functions/localFunction.kt");
    }
}
