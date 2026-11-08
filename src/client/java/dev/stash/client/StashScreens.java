package dev.stash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

final class StashScreens {
    private StashScreens() {
    }

    static boolean canOpenOver(Screen screen) {
        return screen == null
                || screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen
                || screen instanceof StashScreen;
    }

    static void open(Minecraft client) {
        if (client.player == null) {
            return;
        }
        client.setScreen(new StashScreen());
    }
}
