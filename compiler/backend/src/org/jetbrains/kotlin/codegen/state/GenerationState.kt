/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.state

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.`when`.MappingsClassesForWhenByEnum
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import java.io.File

public class GenerationState(
        public val project: Project,
        builderFactory: ClassBuilderFactory,
        public val progress: Progress,
        public val module: ModuleDescriptor,
        bindingContext: BindingContext,
        public val files: List<JetFile>,
        private val disableCallAssertions: Boolean,
        private val disableParamAssertions: Boolean,
        public val generateDeclaredClassFilter: GenerationState.GenerateClassFilter,
        private val disableInline: Boolean,
        disableOptimization: Boolean,
        packagesWithObsoleteParts: Collection<FqName>?,
        // for PackageCodegen in incremental compilation mode
        public val moduleId: String?,
        public val diagnostics: DiagnosticSink,
        // TODO: temporary hack, see JetTypeMapperWithOutDirectory state for details
        public val outDirectory: File?
) {
    public interface GenerateClassFilter {
        public fun shouldAnnotateClass(classOrObject: JetClassOrObject): Boolean
        public fun shouldGenerateClass(classOrObject: JetClassOrObject): Boolean
        public fun shouldGeneratePackagePart(jetFile: JetFile): Boolean
        public fun shouldGenerateScript(script: JetScript): Boolean

        companion object {
            public val GENERATE_ALL: GenerateClassFilter = object : GenerateClassFilter {
                override fun shouldAnnotateClass(classOrObject: JetClassOrObject): Boolean = true

                override fun shouldGenerateClass(classOrObject: JetClassOrObject): Boolean = true

                override fun shouldGenerateScript(script: JetScript): Boolean = true

                override fun shouldGeneratePackagePart(jetFile: JetFile): Boolean = true
            }
        }
    }

    private var used = false
    public val classBuilderMode: ClassBuilderMode
    public val bindingContext: BindingContext
    public val factory: ClassFileFactory
    public val intrinsics: IntrinsicMethods
    public val samWrapperClasses: SamWrapperClasses = SamWrapperClasses(this)
    public val inlineCycleReporter: InlineCycleReporter
    public val mappingsClassesForWhenByEnum: MappingsClassesForWhenByEnum = MappingsClassesForWhenByEnum(this)
    public val bindingTrace: BindingTrace
    public val typeMapper: JetTypeMapper
    public var earlierScriptsForReplInterpreter: List<ScriptDescriptor>? = null
    public val reflectionTypes: ReflectionTypes
    public val jvmRuntimeTypes: JvmRuntimeTypes
    public val packagesWithObsoleteParts: Collection<FqName>
    private val interceptedBuilderFactory: ClassBuilderFactory

    public constructor(
            project: Project,
            builderFactory: ClassBuilderFactory,
            module: ModuleDescriptor,
            bindingContext: BindingContext,
            files: List<JetFile>) : this(project, builderFactory, Progress.DEAF, module, bindingContext, files, true, true, GenerateClassFilter.GENERATE_ALL,
                                         false, false, null, null, DiagnosticSink.DO_NOTHING, null) {
    }

    init {
        var builderFactory = builderFactory
        this.packagesWithObsoleteParts = packagesWithObsoleteParts ?: emptySet<FqName>()
        this.classBuilderMode = builderFactory.getClassBuilderMode()

        this.bindingTrace = DelegatingBindingTrace(bindingContext, "trace in GenerationState")
        this.bindingContext = bindingTrace.getBindingContext()
        this.typeMapper = JetTypeMapperWithOutDirectory(this.bindingContext, classBuilderMode, outDirectory)

        this.intrinsics = IntrinsicMethods()

        builderFactory = OptimizationClassBuilderFactory(builderFactory, disableOptimization)

        var interceptedBuilderFactory: ClassBuilderFactory = BuilderFactoryForDuplicateSignatureDiagnostics(
                builderFactory, this.bindingContext, diagnostics)

        val interceptExtensions = ClassBuilderInterceptorExtension.getInstances(project)

        for (extension in interceptExtensions) {
            interceptedBuilderFactory = extension.interceptClassBuilderFactory(interceptedBuilderFactory, bindingContext, diagnostics)
        }

        this.interceptedBuilderFactory = interceptedBuilderFactory
        this.factory = ClassFileFactory(this, interceptedBuilderFactory)

        this.reflectionTypes = ReflectionTypes(module)
        this.jvmRuntimeTypes = JvmRuntimeTypes()

        this.inlineCycleReporter = InlineCycleReporter(diagnostics)
    }

    public fun isCallAssertionsEnabled(): Boolean = !disableCallAssertions

    public fun isParamAssertionsEnabled(): Boolean = !disableParamAssertions

    public fun isInlineEnabled(): Boolean = !disableInline

    public fun beforeCompile() {
        markUsed()

        CodegenBinding.initTrace(this)
    }

    private fun markUsed() {
        if (used) throw IllegalStateException("${GenerationState::class.java} cannot be used more than once")

        used = true
    }

    public fun destroy() {
        interceptedBuilderFactory.close()
    }
}
