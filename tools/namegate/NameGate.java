// 可读性门禁：拒绝短名字和含糊缩写。
// 检查局部变量、参数和字段。不检查类名和方法名。
// 不给循环索引、坐标、颜色单独开豁免。

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class NameGate {
    private static final Set<String> FORBIDDEN_NAME_PARTS = Set.of(
            "arg", "args", "arr", "avg", "cb", "cfg", "cnt", "cond", "conds", "ctx",
            "dir", "doc", "dto", "env", "err", "fn", "idx", "len", "loc", "max",
            "min", "msg", "num", "obj", "opts", "params", "pos", "proj", "req",
            "res", "resp", "rot", "str", "svc", "tx", "val", "vel");

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        Path projectRoot = args.length > 0 ? Path.of(args[0]) : Path.of("").toAbsolutePath();
        List<Path> sourcePaths = sourcePaths(projectRoot.resolve("src"));
        if (sourcePaths.isEmpty()) {
            System.err.println("源码命名可读性检查失败: src 下没有 Java 文件");
            System.exit(1);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("源码命名可读性检查失败: 当前 java 没有编译器，请用 JDK 运行");
            System.exit(1);
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> sourceObjects = fileManager.getJavaFileObjectsFromPaths(sourcePaths);
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("--release", "17", "-proc:none"),
                    null,
                    sourceObjects);
            Iterable<? extends CompilationUnitTree> units = task.parse();
            Trees trees = Trees.instance(task);
            List<String> violations = new ArrayList<>();
            for (CompilationUnitTree unit : units) {
                new BindingScanner(unit, trees.getSourcePositions()).scan(unit, null);
                violations.addAll(BindingScanner.collected);
                BindingScanner.collected.clear();
            }
            boolean parseFailed = printParseErrors(diagnostics);
            if (!violations.isEmpty()) {
                violations.sort(Comparator.naturalOrder());
                System.err.println("源码命名可读性检查失败:");
                for (String violation : violations) {
                    System.err.println(violation);
                }
                System.err.println("共 " + violations.size() + " 处");
                System.exit(1);
            }
            if (parseFailed) {
                System.exit(1);
            }
        }
        System.out.println("源码命名可读性检查通过：未发现单字母变量或禁止缩写");
    }

    private static List<Path> sourcePaths(Path sourceRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            return walk.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean printParseErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        boolean failed = false;
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            failed = true;
            System.err.println(diagnostic.getSource() + ":"
                    + diagnostic.getLineNumber() + ":"
                    + diagnostic.getColumnNumber() + " 无法解析: "
                    + diagnostic.getMessage(Locale.ROOT));
        }
        return failed;
    }

    static String namingProblem(String name) {
        if (name.length() <= 2) {
            return "变量名不得少于 3 个字符";
        }
        for (String namePart : nameParts(name)) {
            if (FORBIDDEN_NAME_PARTS.contains(namePart)) {
                return "禁止含糊缩写 \"" + namePart + "\"";
            }
        }
        return null;
    }

    private static List<String> nameParts(String name) {
        String spaced = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        String[] pieces = spaced.split("[_\\s]+");
        List<String> parts = new ArrayList<>();
        for (String piece : pieces) {
            if (!piece.isEmpty()) {
                parts.add(piece.toLowerCase(Locale.ROOT));
            }
        }
        return parts;
    }

    private static final class BindingScanner extends TreePathScanner<Void, Void> {
        private static final List<String> collected = new ArrayList<>();
        private final CompilationUnitTree unit;
        private final SourcePositions positions;

        private BindingScanner(CompilationUnitTree unit, SourcePositions positions) {
            this.unit = unit;
            this.positions = positions;
        }

        @Override
        public Void visitVariable(VariableTree node, Void ignored) {
            String name = node.getName().toString();
            String problem = namingProblem(name);
            if (problem != null) {
                long position = namePosition(node, name);
                long line = position < 0 ? 1 : unit.getLineMap().getLineNumber(position);
                long column = position < 0 ? 1 : unit.getLineMap().getColumnNumber(position);
                collected.add(unit.getSourceFile().getName() + ":" + line + ":" + column + " " + name + ": " + problem);
            }
            return super.visitVariable(node, ignored);
        }

        private long namePosition(VariableTree node, String name) {
            CharSequence content = sourceContent();
            long typeEnd = node.getType() == null ? -1 : positions.getEndPosition(unit, node.getType());
            long fromType = findName(content, typeEnd, name);
            if (fromType >= 0) {
                return fromType;
            }
            return findName(content, positions.getStartPosition(unit, node), name);
        }

        private CharSequence sourceContent() {
            try {
                return unit.getSourceFile().getCharContent(true);
            } catch (IOException exception) {
                return "";
            }
        }

        private static long findName(CharSequence content, long start, String name) {
            if (start < 0 || content.length() == 0) {
                return -1;
            }
            int index = (int) Math.min(start, content.length());
            int limit = Math.min(content.length(), index + 160);
            while (index < limit) {
                int found = indexOf(content, name, index, limit);
                if (found < 0) {
                    return -1;
                }
                boolean leftBound = found == 0 || !Character.isJavaIdentifierPart(content.charAt(found - 1));
                int after = found + name.length();
                boolean rightBound = after >= content.length() || !Character.isJavaIdentifierPart(content.charAt(after));
                if (leftBound && rightBound) {
                    return found;
                }
                index = found + 1;
            }
            return -1;
        }

        private static int indexOf(CharSequence content, String name, int from, int limit) {
            for (int index = from; index + name.length() <= limit; index++) {
                boolean matched = true;
                for (int offset = 0; offset < name.length(); offset++) {
                    if (content.charAt(index + offset) != name.charAt(offset)) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    return index;
                }
            }
            return -1;
        }
    }
}
