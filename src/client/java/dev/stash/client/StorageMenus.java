package dev.stash.client;

import net.minecraft.world.inventory.MenuType;

final class StorageMenus {
    private StorageMenus() {
    }

    static boolean tracked(MenuType<?> type) {
        return type == MenuType.GENERIC_9x1
                || type == MenuType.GENERIC_9x2
                || type == MenuType.GENERIC_9x3
                || type == MenuType.GENERIC_9x4
                || type == MenuType.GENERIC_9x5
                || type == MenuType.GENERIC_9x6
                || type == MenuType.GENERIC_3x3
                || type == MenuType.SHULKER_BOX
                || type == MenuType.HOPPER
                || type == MenuType.FURNACE
                || type == MenuType.BLAST_FURNACE
                || type == MenuType.SMOKER
                || type == MenuType.BREWING_STAND
                || type == MenuType.CRAFTER_3x3;
    }
}
