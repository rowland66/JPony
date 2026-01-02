import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Assert;
import org.junit.Test;
import org.rowland.jpony.annotationprocessor.JPonyProcessor;

import java.util.Locale;

public class CapSafetyTests {

    @Test
    public void testNoWriteToVal() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("capsafety/NoWriteToVal.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (21,9) left hand side variable is not writable", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

    @Test
    public void testNoWriteToBox() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("capsafety/NoWriteToBox.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (17,9) left hand side variable is not writable", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

    @Test
    public void testWriteToRef() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("capsafety/WriteToRef.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 0, compilation.errors().size());
    }

    @Test
    public void testNoCallRefOnVal() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("capsafety/NoCallRefOnVal.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (19,9) receiver is not a subtype of target type", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

}
