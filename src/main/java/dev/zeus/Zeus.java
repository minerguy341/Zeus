package dev.zeus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*? if fabric {*/
import net.fabricmc.api.ModInitializer;
/*?}*/

/*? if neoforge {*/
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
*/
/*?}*/

/**
 * Zeus lets resource packs remount Athena CTM onto another backend (Fusion first)
 * by suppressing Athena when packs already specify that backend's model loader.
 * Client-only; safe no-op on dedicated servers.
 */
/*? if neoforge {*/
/*@Mod(value = Zeus.MOD_ID, dist = Dist.CLIENT)
public class Zeus {
*/
/*?}*/

/*? if fabric {*/
public class Zeus implements ModInitializer {
/*?}*/

    public static final String MOD_ID = "zeus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /*? if neoforge {*/
    /*public Zeus(IEventBus modEventBus, ModContainer modContainer) {
        init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            dev.zeus.client.ZeusClient.initClient();
        }
    }
    */
    /*?}*/

    /*? if fabric {*/
    @Override
    public void onInitialize() {
        init();
    }
    /*?}*/

    private static void init() {
        ZeusConfig.load();
        LOGGER.info("Zeus ready (backend={}, namespaces={})", ZeusConfig.ctmBackend, ZeusConfig.remountNamespaces);
    }
}
