package dev.stash.client;

import dev.stash.domain.InvalidSnapshotException;
import dev.stash.domain.StashService;
import dev.stash.domain.StashStorageException;
import dev.stash.domain.StoredSlot;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

final class ContainerWatcher {
    private ContainerWatcher() {
    }

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }
            AbstractContainerMenu menu = containerScreen.getMenu();
            if (!StorageMenus.tracked(menu.getType())) {
                return;
            }
            // Прицел берём сразу: к закрытию игрок уже смотрит в сторону, а меню на клиенте пустое.
            PlaceResolver.Place place = PlaceResolver.capture(
                    client,
                    menu.getType(),
                    PlaceResolver.titleOf(containerScreen.getTitle())
            );
            if (place == null) {
                return;
            }
            SlotCapture capture = new SlotCapture();
            capture.read(client, menu);
            ScreenEvents.beforeTick(screen).register(ignored -> capture.read(client, menu));
            ScreenEvents.remove(screen).register(ignored -> save(place, capture.slots));
        });
    }

    private static void save(PlaceResolver.Place place, List<StoredSlot> slots) {
        StashService service = StashClient.service();
        if (service == null) {
            return;
        }
        try {
            service.remember(place.snapshot(slots, System.currentTimeMillis()));
        } catch (StashStorageException | InvalidSnapshotException exception) {
            StashClient.LOGGER.warn("сундук не запомнился: {}", exception.getMessage());
        }
    }

    private static final class SlotCapture {
        private List<StoredSlot> slots = List.of();

        private void read(Minecraft client, AbstractContainerMenu menu) {
            if (client.player == null) {
                return;
            }
            slots = SlotReader.read(menu, client.player.getInventory());
        }
    }
}
