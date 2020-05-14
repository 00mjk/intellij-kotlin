/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.TestRoot;
import org.junit.runner.RunWith;

/*
 * This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}.
 * DO NOT MODIFY MANUALLY.
 */
@TestRoot("idea")
@SuppressWarnings("all")
@RunWith(JUnit3RunnerWithInners.class)
public class ScriptConfigurationHighlightingTestGenerated extends AbstractScriptConfigurationHighlightingTest {
    @TestMetadata("testData/script/definition/highlighting")
    @TestDataPath("$CONTENT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Highlighting extends AbstractScriptConfigurationHighlightingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("acceptedAnnotations")
        public void testAcceptedAnnotations() throws Exception {
            runTest("testData/script/definition/highlighting/acceptedAnnotations/");
        }

        @TestMetadata("additionalImports")
        public void testAdditionalImports() throws Exception {
            runTest("testData/script/definition/highlighting/additionalImports/");
        }

        @TestMetadata("asyncResolver")
        public void testAsyncResolver() throws Exception {
            runTest("testData/script/definition/highlighting/asyncResolver/");
        }

        @TestMetadata("conflictingModule")
        public void testConflictingModule() throws Exception {
            runTest("testData/script/definition/highlighting/conflictingModule/");
        }

        @TestMetadata("customBaseClass")
        public void testCustomBaseClass() throws Exception {
            runTest("testData/script/definition/highlighting/customBaseClass/");
        }

        @TestMetadata("customExtension")
        public void testCustomExtension() throws Exception {
            runTest("testData/script/definition/highlighting/customExtension/");
        }

        @TestMetadata("customJavaHome")
        public void testCustomJavaHome() throws Exception {
            runTest("testData/script/definition/highlighting/customJavaHome/");
        }

        @TestMetadata("customLibrary")
        public void testCustomLibrary() throws Exception {
            runTest("testData/script/definition/highlighting/customLibrary/");
        }

        @TestMetadata("customLibraryInModuleDeps")
        public void testCustomLibraryInModuleDeps() throws Exception {
            runTest("testData/script/definition/highlighting/customLibraryInModuleDeps/");
        }

        @TestMetadata("doNotSpeakAboutJava")
        public void testDoNotSpeakAboutJava() throws Exception {
            runTest("testData/script/definition/highlighting/doNotSpeakAboutJava/");
        }

        @TestMetadata("doNotSpeakAboutJavaLegacy")
        public void testDoNotSpeakAboutJavaLegacy() throws Exception {
            runTest("testData/script/definition/highlighting/doNotSpeakAboutJavaLegacy/");
        }

        @TestMetadata("emptyAsyncResolver")
        public void testEmptyAsyncResolver() throws Exception {
            runTest("testData/script/definition/highlighting/emptyAsyncResolver/");
        }

        @TestMetadata("errorResolver")
        public void testErrorResolver() throws Exception {
            runTest("testData/script/definition/highlighting/errorResolver/");
        }

        @TestMetadata("implicitReceiver")
        public void testImplicitReceiver() throws Exception {
            runTest("testData/script/definition/highlighting/implicitReceiver/");
        }

        @TestMetadata("javaNestedClass")
        public void testJavaNestedClass() throws Exception {
            runTest("testData/script/definition/highlighting/javaNestedClass/");
        }

        @TestMetadata("multiModule")
        public void testMultiModule() throws Exception {
            runTest("testData/script/definition/highlighting/multiModule/");
        }

        @TestMetadata("noResolver")
        public void testNoResolver() throws Exception {
            runTest("testData/script/definition/highlighting/noResolver/");
        }

        @TestMetadata("propertyAccessor")
        public void testPropertyAccessor() throws Exception {
            runTest("testData/script/definition/highlighting/propertyAccessor/");
        }

        @TestMetadata("propertyAccessorFromModule")
        public void testPropertyAccessorFromModule() throws Exception {
            runTest("testData/script/definition/highlighting/propertyAccessorFromModule/");
        }

        @TestMetadata("simple")
        public void testSimple() throws Exception {
            runTest("testData/script/definition/highlighting/simple/");
        }

        @TestMetadata("throwingResolver")
        public void testThrowingResolver() throws Exception {
            runTest("testData/script/definition/highlighting/throwingResolver/");
        }
    }

    @TestMetadata("testData/script/definition/complex")
    @TestDataPath("$CONTENT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Complex extends AbstractScriptConfigurationHighlightingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doComplexTest, this, testDataFilePath);
        }

        @TestMetadata("errorResolver")
        public void testErrorResolver() throws Exception {
            runTest("testData/script/definition/complex/errorResolver/");
        }
    }
}
