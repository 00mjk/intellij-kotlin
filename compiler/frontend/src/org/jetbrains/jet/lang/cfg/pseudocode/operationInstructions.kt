/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import kotlin.properties.Delegates

public abstract class OperationInstruction protected(
        element: JetElement,
        lexicalScope: LexicalScope,
        public override val inputValues: List<PseudoValue>
) : InstructionWithNext(element, lexicalScope) {
    protected var resultValue: PseudoValue? = null

    override val outputValue: PseudoValue?
        get() = resultValue

    protected fun renderInstruction(name: String, desc: String): String =
            "$name($desc" +
            (if (inputValues.notEmpty) "|${inputValues.makeString(", ")})" else ")") +
            (if (resultValue != null) " -> $resultValue" else "")

    protected fun setResult(value: PseudoValue?): OperationInstruction {
        this.resultValue = value
        return this
    }

    protected fun setResult(factory: PseudoValueFactory?, valueElement: JetElement? = element): OperationInstruction {
        return setResult(factory?.newValue(valueElement, this))
    }
}

public class CallInstruction private(
        element: JetElement,
        lexicalScope: LexicalScope,
        val resolvedCall: ResolvedCall<*>,
        inputValues: List<PseudoValue>
) : OperationInstruction(element, lexicalScope, inputValues) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitCallInstruction(this)
    }

    override fun createCopy() =
            CallInstruction(element, lexicalScope, resolvedCall, inputValues).setResult(resultValue)

    override fun toString() =
            renderInstruction("call", "${render(element)}, ${resolvedCall.getResultingDescriptor()!!.getName()}")

    class object {
        fun create (
                element: JetElement,
                lexicalScope: LexicalScope,
                resolvedCall: ResolvedCall<*>,
                inputValues: List<PseudoValue>,
                factory: PseudoValueFactory?
        ): CallInstruction = CallInstruction(element, lexicalScope, resolvedCall, inputValues).setResult(factory) as CallInstruction
    }
}

// Introduces black-box operation
// Used to:
//      consume input values (so that they aren't considered unused)
//      denote value transformation which can't be expressed by other instructions (such as call or read)
//      pass more than one value to instruction which formally requires only one (e.g. jump)
// "Synthetic" means that the instruction does not correspond to some operation explicitly expressed by PSI element
//      Examples: merging branches of 'if', 'when' and 'try' expressions, providing initial values for parameters, etc.
public class MagicInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val synthetic: Boolean,
        inputValues: List<PseudoValue>
) : OperationInstruction(element, lexicalScope, inputValues) {
    override val outputValue: PseudoValue
        get() = resultValue!!

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitMagic(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitMagic(this)
    }

    override fun createCopy() =
            MagicInstruction(element, lexicalScope, synthetic, inputValues).setResult(resultValue)

    override fun toString() = renderInstruction("magic", render(element))

    class object {
        fun create(
                element: JetElement,
                valueElement: JetElement?,
                lexicalScope: LexicalScope,
                synthetic: Boolean,
                inputValues: List<PseudoValue>,
                factory: PseudoValueFactory
        ): MagicInstruction = MagicInstruction(element, lexicalScope, synthetic, inputValues).setResult(factory, valueElement) as MagicInstruction
    }
}