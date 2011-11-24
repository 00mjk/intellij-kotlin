package org.jetbrains.jet.types;

import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

/**
 * @author abreslav
 */
public class JetOverridingTest extends JetLiteFixture {

    private ModuleDescriptor root = new ModuleDescriptor("test_root");
    private JetStandardLibrary library;
    private JetSemanticServices semanticServices;
    private DescriptorResolver descriptorResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        library          = JetStandardLibrary.getJetStandardLibrary(getProject());
        semanticServices = JetSemanticServices.createSemanticServices(library);
        descriptorResolver = semanticServices.getClassDescriptorResolver(JetTestUtils.DUMMY_TRACE);
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase();
    }

    public void testBasic() throws Exception {
        assertOverridable(
                "fun a() : Int",
                "fun a() : Int");

        assertOverridable(
                "fun a<T1>() : T1",
                "fun a<T>() : T");

        assertOverridable(
                "fun a<T1>(a : T1) : T1",
                "fun a<T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : Y");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        assertNotOverridable(
                "fun ab() : Int",
                "fun a() : Int");

        assertOverridable(
                "fun a() : Int",
                "fun a() : Any");

        // return types are not cheked in the utility
        /*
        assertNotOverridable(
                "fun a() : Any",
                "fun a() : Int");
        */

        assertNotOverridable(
                "fun a(a : Int) : Int",
                "fun a() : Int");

        assertNotOverridable(
                "fun a() : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a(a : Int?) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a<T>(a : Int) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y>(a : T) : T");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : Y) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : X",
                "fun a<T, Y : T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<in T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<*>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<out T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<*>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

    }

    private void assertOverridable(String superFun, String subFun) {
        assertOverridabilityRelation(superFun, subFun, false);
    }

    private void assertNotOverridable(String superFun, String subFun) {
        assertOverridabilityRelation(superFun, subFun, true);
    }

    private void assertOverridabilityRelation(String superFun, String subFun, boolean expectedIsError) {
        FunctionDescriptor a = makeFunction(superFun);
        FunctionDescriptor b = makeFunction(subFun);
        OverridingUtil.OverrideCompatibilityInfo overridableWith = OverridingUtil.isOverridableBy(a, b);
        assertEquals(overridableWith.getMessage(), expectedIsError, !overridableWith.isSuccess());
    }

    private FunctionDescriptor makeFunction(String funDecl) {
        JetNamedFunction function = JetPsiFactory.createFunction(getProject(), funDecl);
        return descriptorResolver.resolveFunctionDescriptor(root, library.getLibraryScope(), function);
    }
}
