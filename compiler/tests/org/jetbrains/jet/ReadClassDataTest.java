package org.jetbrains.jet;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.plugin.JetLanguage;
import org.junit.Assert;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Stepan Koltsov
 */
public class ReadClassDataTest extends UsefulTestCase {

    protected final Disposable myTestRootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    private JetCoreEnvironment jetCoreEnvironment;
    private File tmpdir;
    
    private final File testFile;

    public ReadClassDataTest(@NotNull File testFile) {
        this.testFile = testFile;
        setName(testFile.getName());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpdir = new File("tmp/" + this.getClass().getSimpleName() + "." + this.getName());
        JetTestUtils.recreateDirectory(tmpdir);
    }

    @Override
    public void tearDown() throws Exception {
        Disposer.dispose(myTestRootDisposable);
    }

    private void createMockCoreEnvironment() {
        jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable);

        final File rtJar = new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
        jetCoreEnvironment.addToClasspath(rtJar);
        jetCoreEnvironment.addToClasspath(new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
    }

    @Override
    public void runTest() throws Exception {
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        LanguageASTFactory.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, new JavaASTFactory());


        String text = FileUtil.loadFile(testFile);

        LightVirtualFile virtualFile = new LightVirtualFile(testFile.getName(), JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        BindingContext bindingContext = state.compile(psiFile);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironment.writeToOutputDirectory(classFileFactory, tmpdir.getPath());
        
        NamespaceDescriptor namespaceFromSource = (NamespaceDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, psiFile.getRootNamespace());

        Assert.assertEquals("test", namespaceFromSource.getName());

        Disposer.dispose(myTestRootDisposable);


        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdk(myTestRootDisposable);

        jetCoreEnvironment.addToClasspath(tmpdir);

        JetSemanticServices jetSemanticServices = JetSemanticServices.createSemanticServices(jetCoreEnvironment.getProject());
        JavaSemanticServices semanticServices = new JavaSemanticServices(jetCoreEnvironment.getProject(), jetSemanticServices, new BindingTraceContext());

        JavaDescriptorResolver javaDescriptorResolver = semanticServices.getDescriptorResolver();
        NamespaceDescriptor namespaceFromClass = javaDescriptorResolver.resolveNamespace("test");
        
        compareNamespaces(namespaceFromSource, namespaceFromClass);
    }

    private void compareNamespaces(@NotNull NamespaceDescriptor nsa, @NotNull NamespaceDescriptor nsb) {
        Assert.assertEquals(nsa.getName(), nsb.getName());
        System.out.println("namespace " + nsa.getName());
        for (DeclarationDescriptor ad : nsa.getMemberScope().getAllDescriptors()) {
            if (ad instanceof ClassifierDescriptor) {
                ClassifierDescriptor bd = nsb.getMemberScope().getClassifier(ad.getName());
                compareClassifiers((ClassifierDescriptor) ad, bd);
            } else if (ad instanceof FunctionDescriptor) {
                Set<FunctionDescriptor> functions = nsb.getMemberScope().getFunctions(ad.getName());
                Assert.assertTrue(functions.size() >= 1);
                Assert.assertTrue("not implemented", functions.size() == 1);
                FunctionDescriptor bd = functions.iterator().next();
                compareFunctions((FunctionDescriptor) ad, bd);
            } else {
                throw new AssertionError("Unknown member: " + ad);
            }
        }
    }

    private void compareClassifiers(@NotNull ClassifierDescriptor a, @NotNull ClassifierDescriptor b) {
        String as = serializeContent((ClassDescriptor) a);
        String bs = serializeContent((ClassDescriptor) b);

        Assert.assertEquals(as, bs);
        System.out.println(a);
    }
    
    private String serializeContent(ClassDescriptor klass) {

        StringBuilder sb = new StringBuilder();
        sb.append("class ");

        serialize(klass, sb);

        // TODO: supers
        // TODO: constructors

        sb.append(" {\n");

        if (false) {
            // TODO: for some reason I don't understand scope of ClassDescriptor came from source is empty
        
        List<TypeProjection> typeArguments = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor param : klass.getTypeConstructor().getParameters()) {
            typeArguments.add(new TypeProjection(Variance.INVARIANT, param.getDefaultType()));
        }

        JetScope memberScope = klass.getMemberScope(typeArguments);
        for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
            serialize(member, sb);
            sb.append("\n");
        }

        }

        sb.append("}\n");
        return sb.toString();
    }


    private void compareFunctions(@NotNull FunctionDescriptor a, @NotNull FunctionDescriptor b) {
        String as = serialize(a);
        String bs = serialize(b);
        
        Assert.assertEquals(as, bs);
        System.out.println(as);
    }
    

    private static Object invoke(Method method, Object thiz, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(thiz, args);
        } catch (Exception e) {
            throw new RuntimeException("failed to invoke " + method + ": " + e, e);
        }
    }
    
    
    private void serialize(FunctionDescriptor fun, StringBuilder sb) {
        sb.append("fun ");
        if (!fun.getTypeParameters().isEmpty()) {
            sb.append("<");
            serializeCommaSeparated(fun.getTypeParameters(), sb);
            sb.append(">");
        }
        sb.append(fun.getName());
        sb.append("(");
        serializeCommaSeparated(fun.getValueParameters(), sb);
        sb.append("): ");
        serialize(fun.getReturnType(), sb);
    }
    
    private void serialize(ValueParameterDescriptor valueParameter, StringBuilder sb) {
        sb.append(valueParameter.getName());
        sb.append(": ");
        if (valueParameter.getVarargElementType() != null) {
            sb.append("vararg ");
            serialize(valueParameter.getVarargElementType());
        } else {
            serialize(valueParameter.getOutType());
        }
        if (valueParameter.hasDefaultValue()) {
            sb.append(" = ?");
        }
    }
    
    private void serialize(Variance variance, StringBuilder sb) {
        if (variance == Variance.INVARIANT) {

        } else {
            sb.append(variance);
            sb.append(' ');
        }
    } 
    
    private void serialize(JetType type, StringBuilder sb) {
        serialize(type.getConstructor().getDeclarationDescriptor(), sb);
        if (!type.getArguments().isEmpty()) {
            sb.append("<");
            boolean first = true;
            for (TypeProjection proj : type.getArguments()) {
                serialize(proj.getProjectionKind(), sb);
                serialize(proj.getType(), sb);
                if (!first) {
                    sb.append(", ");
                }
                first = false;
            }
            sb.append(">");
        }
    }
    
    private String serialize(Object o) {
        StringBuilder sb = new StringBuilder();
        serialize(o, sb);
        return sb.toString();
    }
    
    private void serializeCommaSeparated(List<?> list, StringBuilder sb) {
        boolean first = true;
        for (Object o : list) {
            if (!first) {
                sb.append(", ");
            }
            serialize(o, sb);
            first = false;
        }
    }
    
    private void serialize(Object o, StringBuilder sb) {
        // TODO: cache
        for (Method method : ReadClassDataTest.class.getDeclaredMethods()) {
            if (!method.getName().equals("serialize")) {
                continue;
            }
            if (method.getParameterTypes().length != 2) {
                continue;
            }
            if (!method.getParameterTypes()[1].equals(StringBuilder.class)) {
                continue;
            }
            if (method.getParameterTypes()[0].equals(Object.class)) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(o)) {
                invoke(method, this, o, sb);
                return;
            }
        }
        throw new IllegalStateException("don't know how to serialize " + o + " (of " + o.getClass() + ")");
    }

    private void serialize(ModuleDescriptor module, StringBuilder sb) {
        // nop
    }
    
    private void serialize(ClassDescriptor clazz, StringBuilder sb) {
        serialize(clazz.getContainingDeclaration(), sb);
        sb.append(".");
        sb.append(clazz.getName());
    }
    
    private void serialize(NamespaceDescriptor ns, StringBuilder sb) {
        if (ns.getContainingDeclaration() == null) {
            // root ns
            return;
        }
        serialize(ns.getContainingDeclaration(), sb);
        sb.append(".");
        sb.append(ns.getName());
    }
    
    private void serialize(TypeParameterDescriptor param, StringBuilder sb) {
        serialize(param.getVariance(), sb);
        sb.append(param.getName());
        // TODO: serialize bounds
    }
    

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/readClass", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new ReadClassDataTest(file);
            }
        });
    }
    
}
