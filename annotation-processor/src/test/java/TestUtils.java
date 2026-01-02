import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TestUtils {
    static String dumpErrors(List<Diagnostic<? extends JavaFileObject>> errors) {
        return errors.stream()
                .map(d -> d.getMessage(Locale.getDefault()))
                .collect(Collectors.joining("\n"));
    }
}
