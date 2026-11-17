import com.bslx.XueXiMod.shu.technique.release.statusquery.ResourceStatuses;
import com.bslx.XueXiMod.status.wood.client.WoodResourceClientSetup;
import com.bslx.XueXiMod.status.wood.resource.SuihuaStatus;

/**
 * 直接调用客户端登记入口和 ResourceStatuses。不启动 Forge，也不启动本地服务端。
 * FMLClientSetupEvent 是否真的派发，要在客户端环境里看，这次命令覆盖不到。
 */
public final class SuihuaClientRegisterCheck {
    private SuihuaClientRegisterCheck() {
    }

    public static void main(String[] arguments) {
        if (ResourceStatuses.find(SuihuaStatus.IDENTIFIER).isPresent()) {
            System.err.println("suihua was already registered before the client entry");
            System.exit(1);
        }
        WoodResourceClientSetup.registerSuihua();
        if (!SuihuaStatus.IDENTIFIER.equals(ResourceStatuses.find(SuihuaStatus.IDENTIFIER)
                .map(provider -> provider.identifier())
                .orElse(null))) {
            System.err.println("suihua missing after client register");
            System.exit(1);
        }
        WoodResourceClientSetup.registerSuihua();
        System.out.println("suihua client register check passed");
    }
}
