package dev.stash.client;

import dev.stash.domain.StashLimits;
import dev.stash.domain.StashService;
import dev.stash.domain.StashStorageException;
import dev.stash.storage.SqliteStashRepository;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class StashClient implements ClientModInitializer {
    static final Logger LOGGER = LoggerFactory.getLogger("Stash");
    static final String MOD_ID = "stash";

    private static StashService service;
    private static StashLimits limits = StashLimits.defaults();

    @Override
    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        limits = StashConfig.load(configDir.resolve("stash.json"), LOGGER);
        openDatabase(configDir.resolve("stash").resolve("stash.sqlite"));
        ContainerWatcher.register();

        KeyMapping open = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "stash.key",
                GLFW.GLFW_KEY_H,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) {
                if (client.player != null && StashScreens.canOpenOver(client.screen)) {
                    StashScreens.open(client);
                }
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("stash").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null) {
                        StashScreens.open(client);
                    }
                    return 1;
                }))
        );
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> closeDatabase());
    }

    static StashService service() {
        return service;
    }

    static StashLimits limits() {
        return limits;
    }

    private static void openDatabase(Path file) {
        try {
            service = new StashService(new SqliteStashRepository(file), limits);
        } catch (StashStorageException exception) {
            LOGGER.warn("stash.sqlite не открылся, поиск будет после перезапуска", exception);
            service = null;
        }
    }

    private static void closeDatabase() {
        if (service == null) {
            return;
        }
        try {
            service.close();
        } catch (StashStorageException exception) {
            LOGGER.warn("stash.sqlite не закрылся", exception);
        } finally {
            service = null;
        }
    }
}
