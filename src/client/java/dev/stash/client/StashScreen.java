package dev.stash.client;

import dev.stash.domain.SearchHit;
import dev.stash.domain.SearchOrigin;
import dev.stash.domain.SearchOutcome;
import dev.stash.domain.StashService;
import dev.stash.domain.StashStorageException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

final class StashScreen extends Screen {
    private static final int PADDING = 16;
    private static final int TITLE_Y = 10;
    private static final int QUERY_Y = 26;
    private static final int QUERY_HEIGHT = 18;
    private static final int HINT_Y = 48;
    private static final int LIST_TOP = 64;
    private static final int BOTTOM_BAR = 22;
    private static final int ROW_HEIGHT = 14;
    private static final int SCROLL_STEP = 12;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int STATUS_COLOR = 0xFF7DDF7D;
    private static final int HINT_COLOR = 0xFFA0A0A0;

    private EditBox query;
    private final List<Row> rows = new ArrayList<>();
    private Component info;
    private Component status;
    private int scroll;

    StashScreen() {
        super(Component.translatable("stash.title"));
    }

    @Override
    protected void init() {
        int fieldWidth = Math.max(120, this.width - PADDING * 2);
        query = new EditBox(this.font, PADDING, QUERY_Y, fieldWidth, QUERY_HEIGHT, Component.translatable("stash.query"));
        query.setMaxLength(StashClient.limits().queryMaxLength());
        query.setHint(Component.translatable("stash.query.hint"));
        query.setResponder(text -> {
            status = null;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(query);
        query.setFocused(true);
        refresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, this.width / 2, TITLE_Y, TITLE_COLOR);
        graphics.drawString(font, Component.translatable("stash.hint"), PADDING, HINT_Y, HINT_COLOR);
        int viewTop = LIST_TOP;
        int viewBottom = this.height - BOTTOM_BAR;
        graphics.enableScissor(0, viewTop, this.width, viewBottom);
        int y = viewTop - scroll;
        if (info != null) {
            for (FormattedCharSequence line : font.split(info, this.width - PADDING * 2)) {
                graphics.drawString(font, line, PADDING, y, TEXT_COLOR);
                y += font.lineHeight + 2;
            }
        }
        for (Row row : rows) {
            int rowY = y;
            row.top = rowY;
            graphics.drawString(font, Component.literal(row.text), PADDING, rowY, TEXT_COLOR);
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        if (status != null) {
            graphics.drawString(font, status, PADDING, this.height - 14, STATUS_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        int viewTop = LIST_TOP;
        int viewBottom = this.height - BOTTOM_BAR;
        if (event.y() < viewTop || event.y() > viewBottom) {
            return false;
        }
        for (Row row : rows) {
            if (event.y() >= row.top && event.y() < row.top + ROW_HEIGHT) {
                activate(row.hit, event.hasShiftDown());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int content = rows.size() * ROW_HEIGHT + font.lineHeight;
        int view = this.height - BOTTOM_BAR - LIST_TOP;
        int maxScroll = Math.max(0, content - view);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY) * SCROLL_STEP));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refresh() {
        rows.clear();
        info = null;
        StashService service = StashClient.service();
        if (service == null) {
            info = Component.translatable("stash.unavailable");
            return;
        }
        try {
            SearchOutcome outcome = service.search(query.getValue(), origin());
            switch (outcome) {
                case SearchOutcome.NeedQuery ignored -> info = Component.translatable("stash.intro");
                case SearchOutcome.None ignored -> info = Component.translatable("stash.empty");
                case SearchOutcome.Hits hits -> {
                    for (SearchHit hit : hits.hits()) {
                        rows.add(new Row(hit, HitText.line(hit)));
                    }
                }
            }
        } catch (StashStorageException exception) {
            StashClient.LOGGER.warn("поиск упал: {}", exception.getMessage());
            info = Component.translatable("stash.search_failed");
        }
    }

    private void activate(SearchHit hit, boolean forget) {
        StashService service = StashClient.service();
        if (service == null) {
            return;
        }
        if (forget) {
            try {
                service.forget(hit.locationKey());
            } catch (StashStorageException exception) {
                StashClient.LOGGER.warn("не получилось забыть запись: {}", exception.getMessage());
                status = Component.translatable("stash.search_failed");
                return;
            }
            status = Component.translatable("stash.forgot");
            refresh();
            return;
        }
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(HitText.coordinates(hit));
        }
        status = Component.translatable("stash.copied", hit.x(), hit.y(), hit.z());
    }

    private SearchOrigin origin() {
        Player player = minecraft == null ? null : minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        return new SearchOrigin(
                minecraft.level.dimension().identifier().toString(),
                player.blockPosition().getX(),
                player.blockPosition().getY(),
                player.blockPosition().getZ()
        );
    }

    private static final class Row {
        private final SearchHit hit;
        private final String text;
        private int top;

        private Row(SearchHit hit, String text) {
            this.hit = hit;
            this.text = text;
        }
    }
}
