package dev.zeus.client;

import dev.zeus.Zeus;
import dev.zeus.ZeusConfig;

/*? if fabric {*/
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
/*?}*/

/*? if neoforge {*/
/*import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
*/
/*?}*/

/**
 * Client entry: config + debug commands. Resource packs supply CTM models; Zeus suppresses Athena
 * when those packs use the configured backend loader.
 */
/*? if fabric {*/
public final class ZeusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        initClient();
    }
/*?} else {*/
/*public final class ZeusClient {
*/
/*?}*/

    public static void initClient() {
        ZeusConfig.maybeReload();
        registerDebugCommands();
        Zeus.LOGGER.info(
                "Zeus client ready (backend={}, namespaces={}) — Athena suppressed only when packs use the configured backend loader",
                ZeusConfig.ctmBackend,
                ZeusConfig.remountNamespaces
        );
    }

    private static void registerDebugCommands() {
        /*? if fabric {*/
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                ZeusDebugCommands.registerFabric(dispatcher)
        );
        /*?} else {*/
        /*NeoForge.EVENT_BUS.addListener(ZeusClient::onRegisterClientCommands);*/
        /*?}*/
    }

    /*? if neoforge {*/
    /*private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ZeusDebugCommands.registerServerStyle(event.getDispatcher());
    }*/
    /*?}*/
}