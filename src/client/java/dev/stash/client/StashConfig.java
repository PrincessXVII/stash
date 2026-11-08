package dev.stash.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.stash.domain.StashLimits;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class StashConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private StashConfig() {
    }

    static StashLimits load(Path path, Logger logger) {
        if (Files.notExists(path)) {
            writeDefaults(path, logger);
            return StashLimits.defaults();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FileModel model = GSON.fromJson(reader, FileModel.class);
            if (model == null) {
                logger.warn("stash.json пустой, беру обычные настройки");
                return StashLimits.defaults();
            }
            return model.toLimits();
        } catch (JsonSyntaxException exception) {
            logger.warn("stash.json битый, беру обычные настройки", exception);
            return StashLimits.defaults();
        } catch (IOException exception) {
            logger.warn("stash.json не открылся, беру обычные настройки", exception);
            return StashLimits.defaults();
        }
    }

    private static void writeDefaults(Path path, Logger logger) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, GSON.toJson(FileModel.from(StashLimits.defaults())), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            logger.warn("stash.json не записался", exception);
        }
    }

    private static final class FileModel {
        int maxContainers = StashLimits.defaults().maxContainers();
        int maxSlotsPerContainer = StashLimits.defaults().maxSlotsPerContainer();
        int maxSearchHits = StashLimits.defaults().maxSearchHits();
        int queryMaxLength = StashLimits.defaults().queryMaxLength();

        static FileModel from(StashLimits limits) {
            FileModel model = new FileModel();
            model.maxContainers = limits.maxContainers();
            model.maxSlotsPerContainer = limits.maxSlotsPerContainer();
            model.maxSearchHits = limits.maxSearchHits();
            model.queryMaxLength = limits.queryMaxLength();
            return model;
        }

        StashLimits toLimits() {
            return new StashLimits(maxContainers, maxSlotsPerContainer, maxSearchHits, queryMaxLength);
        }
    }
}
