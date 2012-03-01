/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.test;

import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.Scriptable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pavel Talanov
 */
public final class KotlinLibTest extends TranslationTest {

    final private static String MAIN = "kotlinLib/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    private void runPropertyTypeCheck(String objectName, Map<String, Class<? extends Scriptable>> propertyToType)
            throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoPropertyTypesChecker(objectName, propertyToType));
    }


    public void testKotlinJsLibRunsWithRhino() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoResultChecker() {
            @Override
            public void runChecks(Context context, Scriptable scope) throws Exception {
                //do nothing
            }
        });
    }

    //TODO: refactor
    public void testCreatedTraitIsJSObject() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("trait.js")),
                     new RhinoPropertyTypesChecker("foo", propertyToType));
    }


    public void testCreatedNamespaceIsJSObject() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespace.js")),
                     new RhinoPropertyTypesChecker("foo", propertyToType));
    }

    //
    // TODO:Refactor calls to function result checker with test
    public void testNamespaceHasDeclaredFunction() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespace.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testNamespaceHasDeclaredClasses() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespaceWithClasses.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testIsSameType() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isSameType.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testIsAncestorType() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isAncestorType.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testIsComplexTest() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isComplexTest.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testCommaExpression() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("commaExpression.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testArray() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("array.js")),
                     new RhinoFunctionResultChecker("test", true));
    }


    public void testHashMap() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("hashMap.js")),
                     new RhinoFunctionResultChecker("test", true));
    }

    @Override
    protected boolean shouldCreateOut() {
        return false;
    }

}
