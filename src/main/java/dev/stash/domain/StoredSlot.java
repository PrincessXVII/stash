package dev.stash.domain;

public record StoredSlot(int slotIndex, String itemId, int count, String displayName, String searchName) {
    public StoredSlot {
        if (slotIndex < 0) {
            throw new InvalidSlotException("Номер слота меньше нуля");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new InvalidSlotException("У предмета пустой id");
        }
        if (count <= 0) {
            throw new InvalidSlotException("Количество должно быть больше нуля");
        }
        itemId = Texts.fold(itemId);
        displayName = Texts.oneLine(displayName, Texts.MAX_DISPLAY_NAME_LENGTH);
        if (displayName.isEmpty()) {
            displayName = itemId;
        }
        searchName = Texts.fold(displayName);
    }
}
