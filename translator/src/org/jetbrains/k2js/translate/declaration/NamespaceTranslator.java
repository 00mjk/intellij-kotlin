package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getAllClassesDefinedInNamespace;

/**
 * @author Pavel.Talanov
 *         <p/>
 *         Genereate code for a single descriptor.
 */
public final class NamespaceTranslator extends AbstractTranslator {

    @NotNull
    private final NamespaceDescriptor descriptor;
    @NotNull
    private final JsName namespaceName;
    @NotNull
    private final ClassDeclarationTranslator classDeclarationTranslator;

    @NotNull
    public static JsStatement translateNamespace(@NotNull NamespaceDescriptor namespace,
                                                 @NotNull TranslationContext context) {
        return (new NamespaceTranslator(namespace, context)).translateNamespace();
    }

    private NamespaceTranslator(@NotNull NamespaceDescriptor descriptor, @NotNull TranslationContext context) {
        super(context.newDeclaration(descriptor));
        this.descriptor = descriptor;
        this.namespaceName = context.getNameForDescriptor(descriptor);
        this.classDeclarationTranslator = new ClassDeclarationTranslator(context(),
                getAllClassesDefinedInNamespace(descriptor));
    }

    @NotNull
    public JsStatement translateNamespace() {
        if (isNamespaceEmpty()) {
            return program().getEmptyStmt();
        }
        classDeclarationTranslator.generateDeclarations();
        return AstUtil.newBlock(classDeclarationsStatement(),
                namespaceOwnDeclarationStatement(),
                namespaceInitializeStatement());
    }

    //TODO: at the moment this check is very ineffective, possible solution is to cash the result of getDFN
    // other solution is to determine it's not affecting performance :D
    private boolean isNamespaceEmpty() {
        return BindingUtils.getDeclarationsForNamespace(context().bindingContext(), descriptor).isEmpty();
    }

    @NotNull
    private JsStatement classDeclarationsStatement() {
        return classDeclarationTranslator.getDeclarationsStatement();
    }

    @NotNull
    private JsStatement namespaceInitializeStatement() {
        JsNameRef initializeMethodReference = Namer.initializeMethodReference();
        AstUtil.setQualifier(initializeMethodReference, namespaceName.makeRef());
        return AstUtil.newInvocation(initializeMethodReference).makeStmt();
    }

    @NotNull
    private JsInvocation namespaceCreateMethodInvocation() {
        return AstUtil.newInvocation(context().namer().namespaceCreationMethodReference());
    }

    @NotNull
    private JsStatement namespaceOwnDeclarationStatement() {
        JsInvocation namespaceDeclaration = namespaceCreateMethodInvocation();
        addMemberDeclarations(namespaceDeclaration);
        addClassesDeclarations(namespaceDeclaration);
        return AstUtil.newVar(namespaceName, namespaceDeclaration);
    }

    private void addClassesDeclarations(@NotNull JsInvocation namespaceDeclaration) {
        namespaceDeclaration.getArguments().add(classDeclarationTranslator.classDeclarationsForNamespace(descriptor));
    }

    private void addMemberDeclarations(@NotNull JsInvocation jsNamespace) {
        JsObjectLiteral jsClassDescription = translateNamespaceMemberDeclarations();
        jsNamespace.getArguments().add(jsClassDescription);
    }

    @NotNull
    private JsObjectLiteral translateNamespaceMemberDeclarations() {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        propertyList.add(Translation.generateNamespaceInitializerMethod(descriptor, context()));
        propertyList.addAll(new DeclarationBodyVisitor().traverseNamespace(descriptor, context()));
        return new JsObjectLiteral(propertyList);
    }
}
