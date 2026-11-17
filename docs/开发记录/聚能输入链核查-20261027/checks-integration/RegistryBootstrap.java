public final class RegistryBootstrap {
    private RegistryBootstrap() {
    }

    public static void main(String[] programArguments) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Class<?> target = Class.forName(programArguments[0]);
        target.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
    }
}
