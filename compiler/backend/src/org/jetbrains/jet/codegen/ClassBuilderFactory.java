package org.jetbrains.jet.codegen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author max
 */
public interface ClassBuilderFactory {
    ClassBuilder newClassBuilder();
    String asText(ClassBuilder builder);
    byte[] asBytes(ClassBuilder builder);
    
    ClassBuilderFactory TEXT = new ClassBuilderFactory() {
        @Override
        public ClassBuilder newClassBuilder() {
            return new ClassBuilder.Concrete(new TraceClassVisitor(new PrintWriter(new StringWriter())));
        }

        @Override
        public String asText(ClassBuilder builder) {
            TraceClassVisitor visitor = (TraceClassVisitor) builder.getVisitor();
    
            StringWriter writer = new StringWriter();
            visitor.print(new PrintWriter(writer));
    
            return writer.toString();
        }

        @Override
        public byte[] asBytes(ClassBuilder builder) {
            throw new UnsupportedOperationException("TEXT generator asked for bytes");
        }
    };
    
    ClassBuilderFactory BINARIES = new ClassBuilderFactory() {
        @Override
        public ClassBuilder newClassBuilder() {
            return new ClassBuilder.Concrete(new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS){
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    try {
                        return super.getCommonSuperClass(type1, type2);
                    }
                    catch (Throwable t) {
                        // @todo we might need at some point do more sofisticated handling
                        return "java/lang/Object";
                    }
                }
            });
        }

        @Override
        public String asText(ClassBuilder builder) {
            throw new UnsupportedOperationException("BINARIES generator asked for text");
        }

        @Override
        public byte[] asBytes(ClassBuilder builder) {
            ClassWriter visitor = (ClassWriter) builder.getVisitor();
            return visitor.toByteArray();
        }
    };

}
