/*
* Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.DescriptorWithParentObject
import com.intellij.debugger.ui.tree.render.OnDemandPresentationProvider
import com.intellij.debugger.ui.tree.render.OnDemandRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiExpression
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.sun.jdi.Method
import com.sun.jdi.ObjectReference

class GetterDescriptor(
    private val parentObject: ObjectReference,
    private val getter: Method,
    project: Project
) : ValueDescriptorImpl(project), DescriptorWithParentObject {
    companion object {
        val GETTER_PREFIXES = arrayOf("get", "is")

        private val defaultGetterOnDemandPresentationProvider = OnDemandPresentationProvider { node ->
            node.setFullValueEvaluator(OnDemandRenderer.createFullValueEvaluator(KotlinDebuggerCoreBundle.message("message.variables.property.get")))
            node.setPresentation(AllIcons.Nodes.Property, XRegularValuePresentation("", null, ""), false)
        }
    }

    private val name = getter.name().removeGetterPrefix().decapitalize()

    init {
        OnDemandRenderer.ON_DEMAND_CALCULATED.set(this, false)
        setOnDemandPresentationProvider(defaultGetterOnDemandPresentationProvider)
    }

    private fun String.removeGetterPrefix(): String {
        if (startsWith("get")) {
            return drop(3)
        }
        // For properties starting with 'is' leave the name unmodified
        return this
    }

    override fun getObject() = parentObject

    override fun getDescriptorEvaluation(context: DebuggerContext): PsiExpression = throw EvaluateException("Getter evaluation is not supported")

    override fun getName() = name

    override fun calcValue(evaluationContext: EvaluationContextImpl?) =
        evaluationContext?.debugProcess?.invokeMethod(evaluationContext, parentObject, getter, emptyList())
}
