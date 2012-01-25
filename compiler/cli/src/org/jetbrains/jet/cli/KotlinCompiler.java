package org.jetbrains.jet.cli;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;

import java.io.PrintStream;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class KotlinCompiler {
    private KotlinCompiler() {
    }

    public static class Arguments {
        @Argument(value = "output", description = "output directory")
        public String outputDir;

        @Argument(value = "jar", description = "jar file name")
        public String jar;

        @Argument(value = "src", description = "source file or directory")
        public String src;

        @Argument(value = "module", description = "module to compile")
        public String module;

        @Argument(value = "includeRuntime", description = "include Kotlin runtime in to resulting jar")
        public boolean includeRuntime;

        @Argument(value = "stdlib", description = "Path to the stdlib.jar")
        public String stdlib;

        @Argument(value = "help", alias = "h", description = "show help")
        public boolean help;
    }

    private static void usage(PrintStream target) {
        target.println("Usage: KotlinCompiler [-output <outputDir>|-jar <jarFileName>] [-stdlib <path to runtime.jar>] [-src <filename or dirname>|-module <module file>] [-includeRuntime]");
    }

    public static void main(String... args) {
        try {
            int rc = exec(args);
            if (rc != 0) {
                System.err.println("exec() finished with " + rc + " return code");
                System.exit(rc);
            }
        } catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public static int exec(String... args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        }
        catch (IllegalArgumentException e) {
            usage(System.err);
            return 1;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return 1;
        }

        if (arguments.help) {
            usage(System.out);
            return 0;
        }

        CompileEnvironment environment = new CompileEnvironment();

        if (arguments.stdlib != null) {
            environment.setStdlib(arguments.stdlib);
        }

        if (arguments.module != null) {
            environment.compileModuleScript(arguments.module, arguments.jar, arguments.includeRuntime);
            return 0;
        }
        else {
            if (!environment.compileBunchOfSources(arguments.src, arguments.jar, arguments.outputDir, arguments.includeRuntime)) {
                return 1;
            }
        }

        return 0;
    }
}
