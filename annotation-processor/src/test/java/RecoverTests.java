import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Assert;
import org.junit.Test;
import org.rowland.jpony.annotationprocessor.JPonyProcessor;

import java.util.Locale;

public class RecoverTests {

    @Test
    public void testCanRecover_NewRefToIso() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CanRecover_NewRefToIso.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 0, compilation.errors().size());
    }

    @Test
    public void testCanRecover_NewRefToVal() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CanRecover_NewRefToVal.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 0, compilation.errors().size());
    }

    @Test
    public void testCantRecover_NewValToIso() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CantRecover_NewValToIso.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (17,16) incorrect or inconsistent return type", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

    @Test
    public void testCantRecover_NewTagToVal() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CantRecover_NewTagToVal.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (17,16) incorrect or inconsistent return type", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

    @Test
    public void testCanSee_LetLocalRefAsTag() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CanSee_LetLocalRefAsTag.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (20,45) argument is not a subtype of parameter", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }

    @Test
    public void testNoWriteToVal() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CantReturnTrn_TrnAutoRecovery.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (24,57) receiver is not a subtype of target type", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }
}
