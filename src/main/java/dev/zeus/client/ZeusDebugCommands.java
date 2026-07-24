package dev.zeus.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.zeus.Zeus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Client-side debug commands for Zeus.
 */
public final class ZeusDebugCommands {
    private ZeusDebugCommands() {
    }

    /** NeoForge / shared CommandSourceStack dispatcher. */
    public static void registerServerStyle(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zeus")
                .then(Commands.literal("loader")
                        .executes(ctx -> {
                            BlockLoaderProbe.reportLookedAtBlock();
                            return 1;
                        }))
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Zeus debug: /zeus loader — probe looked-at block model loaders"),
                                    false
                            );
                            return 1;
                        }));
        dispatcher.register(root);
        Zeus.LOGGER.info("Registered client command /zeus loader");
    }

    /*? if fabric {*/
    public static void registerFabric(
            CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher
    ) {
        var root = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("zeus")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("loader")
                        .executes(ctx -> {
                            BlockLoaderProbe.reportLookedAtBlock();
                            return 1;
                        }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal(
                                    "Zeus debug: /zeus loader — probe looked-at block model loaders"
                            ));
                            return 1;
                        }));
        dispatcher.register(root);
        Zeus.LOGGER.info("Registered Fabric client command /zeus loader");
    }
    /*?}*/

    static void log(String msg) {
        Zeus.LOGGER.info("[probe] {}", msg);
    }
}