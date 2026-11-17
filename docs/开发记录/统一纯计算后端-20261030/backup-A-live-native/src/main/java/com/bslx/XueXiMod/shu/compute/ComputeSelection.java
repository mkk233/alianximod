package com.bslx.XueXiMod.shu.compute;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 只在第一次纯计算入口读取启动参数并尝试加载本地库。失败后保持 Java，不按实体重复加载。
 */
final class ComputeSelection {
    private static final Object GATE = new Object();
    private static volatile boolean resolved;
    private static volatile boolean nativeReady;
    private static volatile int capabilityMask;
    private static volatile String recordedLine = "纯计算后端：尚未选择";
    private static volatile String recordedReason = "unresolved";

    private ComputeSelection() {
    }

    static void ensureResolved() {
        if (resolved) {
            return;
        }
        synchronized (GATE) {
            if (resolved) {
                return;
            }
            resolve();
            resolved = true;
        }
    }

    static boolean useNative(int capability, int count) {
        ensureResolved();
        return nativeReady && count >= ComputeCodes.NATIVE_BATCH_FLOOR && hasCapability(capability);
    }

    static boolean missingNativeCapability(int capability, int count) {
        ensureResolved();
        return nativeReady && count >= ComputeCodes.NATIVE_BATCH_FLOOR && !hasCapability(capability);
    }

    static String recordedLine() {
        ensureResolved();
        return recordedLine;
    }

    static String recordedReason() {
        ensureResolved();
        return recordedReason;
    }

    static void disableNative(String reason) {
        nativeReady = false;
        recordedReason = reason;
    }

    private static boolean hasCapability(int capability) {
        return (capabilityMask & capability) == capability;
    }

    private static void resolve() {
        String backendText = System.getProperty(ComputeCodes.PROPERTY_BACKEND);
        String backendToken = backendText == null ? "" : backendText.trim();
        if (backendToken.isEmpty() || ComputeCodes.BACKEND_TOKEN_JAVA.equals(backendToken)) {
            String libraryText = System.getProperty(ComputeCodes.PROPERTY_LIBRARY);
            if (libraryText != null && !libraryText.trim().isEmpty()) {
                acceptJava("library-ignored",
                        "纯计算后端：Java 参考实现。backend 不是 native，忽略 library，不加载动态库。");
                return;
            }
            acceptJava("java-default",
                    "纯计算后端：Java 参考实现。未请求 native，不加载动态库。");
            return;
        }
        if (!ComputeCodes.BACKEND_TOKEN_NATIVE.equals(backendToken)) {
            acceptJava("unrecognized-backend",
                    "纯计算后端：回退 Java 参考实现。原因=unrecognized-backend，值=" + backendToken);
            return;
        }
        String libraryText = System.getProperty(ComputeCodes.PROPERTY_LIBRARY);
        if (libraryText == null || libraryText.trim().isEmpty()) {
            acceptJava("library-missing",
                    "纯计算后端：回退 Java 参考实现。原因=library-missing，native 需要绝对路径。");
            return;
        }
        Path libraryPath = Path.of(libraryText.trim());
        if (!libraryPath.isAbsolute()) {
            acceptJava("library-not-absolute",
                    "纯计算后端：回退 Java 参考实现。原因=library-not-absolute。不搜索其它目录。");
            return;
        }
        Path fileName = libraryPath.getFileName();
        String expectedName = expectedLibraryFile();
        if (expectedName == null) {
            acceptJava("unsupported-os",
                    "纯计算后端：回退 Java 参考实现。原因=unsupported-os。");
            return;
        }
        if (fileName == null || !expectedName.equals(fileName.toString())) {
            acceptJava("library-name",
                    "纯计算后端：回退 Java 参考实现。原因=library-name，要求文件名=" + expectedName);
            return;
        }
        if (!supportedArchitecture()) {
            acceptJava("unsupported-arch",
                    "纯计算后端：回退 Java 参考实现。原因=unsupported-arch。");
            return;
        }
        if (!Files.isRegularFile(libraryPath)) {
            acceptJava("library-not-file",
                    "纯计算后端：回退 Java 参考实现。原因=library-not-file。");
            return;
        }
        try {
            System.load(libraryPath.toString());
        } catch (UnsatisfiedLinkError linkError) {
            acceptJava("load-failed",
                    "纯计算后端：回退 Java 参考实现。原因=load-failed，" + safeMessage(linkError));
            return;
        } catch (SecurityException securityFailure) {
            acceptJava("load-failed",
                    "纯计算后端：回退 Java 参考实现。原因=load-failed，" + safeMessage(securityFailure));
            return;
        }
        int[] sink = new int[2];
        int status;
        try {
            status = XuexiComputeNative.handshake(sink);
        } catch (UnsatisfiedLinkError linkError) {
            acceptJava("handshake-failed",
                    "纯计算后端：回退 Java 参考实现。原因=handshake-failed，" + safeMessage(linkError)
                            + "。库已留在进程内但不再调用。");
            return;
        }
        if (status != ComputeCodes.STATUS_OK) {
            acceptJava("handshake-failed",
                    "纯计算后端：回退 Java 参考实现。原因=handshake-failed，状态=" + status
                            + "。库已留在进程内但不再调用。");
            return;
        }
        if (sink[0] != ComputeCodes.ABI_VERSION) {
            acceptJava("abi-mismatch",
                    "纯计算后端：回退 Java 参考实现。原因=abi-mismatch，库版本=" + sink[0]
                            + "，要求=" + ComputeCodes.ABI_VERSION + "。库已留在进程内但不再调用。");
            return;
        }
        nativeReady = true;
        capabilityMask = sink[1];
        recordedReason = "native";
        recordedLine = "纯计算后端：native 已握手。abi=" + sink[0]
                + "，capabilities=" + sink[1]
                + "，library=" + libraryPath;
    }

    private static void acceptJava(String reason, String line) {
        nativeReady = false;
        capabilityMask = 0;
        recordedReason = reason;
        recordedLine = line;
    }

    private static String expectedLibraryFile() {
        String operatingSystem = System.getProperty("os.name");
        if (operatingSystem == null) {
            return null;
        }
        String folded = operatingSystem.toLowerCase(Locale.ROOT);
        if (folded.startsWith("windows")) {
            return ComputeCodes.WINDOWS_LIBRARY_FILE;
        }
        if (folded.startsWith("linux")) {
            return ComputeCodes.LINUX_LIBRARY_FILE;
        }
        if (folded.startsWith("mac")) {
            return ComputeCodes.MACOS_LIBRARY_FILE;
        }
        return null;
    }

    private static boolean supportedArchitecture() {
        String operatingSystem = System.getProperty("os.name");
        String architecture = System.getProperty("os.arch");
        if (operatingSystem == null || architecture == null) {
            return false;
        }
        String folded = operatingSystem.toLowerCase(Locale.ROOT);
        boolean x64 = "amd64".equals(architecture) || "x86_64".equals(architecture);
        if (folded.startsWith("windows")) {
            return x64;
        }
        if (folded.startsWith("linux")) {
            return x64;
        }
        if (folded.startsWith("mac")) {
            return x64 || "aarch64".equals(architecture) || "arm64".equals(architecture);
        }
        return false;
    }

    private static String safeMessage(Throwable failure) {
        String text = failure.getMessage();
        if (text == null || text.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return text;
    }
}
