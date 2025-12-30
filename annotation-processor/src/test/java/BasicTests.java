import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Assert;
import org.junit.Test;
import org.rowland.jpony.annotationprocessor.JPonyProcessor;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BasicTests {

    @Test
    public void testFirstSimple() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("FirstSimple.java"));

        Assert.assertEquals(dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(dumpErrors(compilation.errors()), "at (14,16) incorrect or inconsistent return type", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

    private String dumpErrors(List<Diagnostic<? extends JavaFileObject>> errors) {
        return errors.stream()
                .map(d -> d.getMessage(Locale.getDefault()))
                .collect(Collectors.joining("\n"));
    }
}
