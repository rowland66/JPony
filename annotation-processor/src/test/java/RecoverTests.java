import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Assert;
import org.junit.Test;
import org.rowland.jpony.annotationprocessor.JPonyProcessor;

import java.util.Locale;

public class RecoverTests {

    @Test
    public void testNoWriteToVal() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new JPonyProcessor())
                .compile(JavaFileObjects.forResource("recover/CantReturnTrn_TrnAutoRecovery.java"));

        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), 1, compilation.errors().size());
        Assert.assertEquals(TestUtils.dumpErrors(compilation.errors()), "at (24,57) receiver is not a subtype of target type", compilation.errors().getFirst().getMessage(Locale.getDefault()));
    }
}
