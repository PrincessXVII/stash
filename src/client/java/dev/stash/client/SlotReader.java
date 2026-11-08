package dev.stash.client;

import dev.stash.domain.InvalidSlotException;
import dev.stash.domain.StoredSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SlotReader {
    private SlotReader() {
    }

    static List<StoredSlot> read(AbstractContainerMenu menu, Inventory playerInventory) {
        Map<Integer, StoredSlot> byIndex = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container == playerInventory || slot.container instanceof Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String name = stack.getHoverName().getString();
            try {
                byIndex.put(slot.getContainerSlot(), new StoredSlot(slot.getContainerSlot(), itemId, stack.getCount(), name, ""));
            } catch (InvalidSlotException exception) {
                StashClient.LOGGER.warn("слот {} кривой: {}", slot.getContainerSlot(), exception.getMessage());
            }
        }
        return List.copyOf(byIndex.values());
    }
}
