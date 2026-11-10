package com.lustig7.overdrive.compat;

import com.lustig7.overdrive.CommonConfig;
import com.lustig7.overdrive.Overdrive;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Ships an FTB Quests chapter without a compile-time dependency: FTB Quests reads every
 * {@code .snbt} in {@code config/ftbquests/quests/chapters/}, so we copy ours there.
 * <p>
 * The marker file remembers which chapter version was installed. A newer version replaces the
 * file (unless {@code integration.ftbquests.updateChapter} is off); a deleted chapter is never
 * re-added. FTB Quests rewrites the file and moves its text into {@code quests/lang/}, so we
 * can't detect edits by content, hence the explicit switch for packs that customise it.
 */
public final class FtbQuestsCompat {
    public static final String MODID = "ftbquests";
    /** Bump whenever overdrive.snbt changes (tools/gen_data.py: CHAPTER_VERSION). */
    public static final int CHAPTER_VERSION = 2;
    private static final String RESOURCE = "/overdrive/ftbquests/overdrive.snbt";

    private FtbQuestsCompat() {}

    public static void installChapter() {
        if (!ModList.get().isLoaded(MODID) || !CommonConfig.INSTALL_FTB_QUESTS_CHAPTER.get()) return;
        Path config = FMLPaths.CONFIGDIR.get();
        Path marker = config.resolve("overdrive/ftbquests_chapter.installed");
        Path target = config.resolve("ftbquests/quests/chapters/overdrive.snbt");
        try {
            boolean hasMarker = Files.exists(marker);
            if (!Files.exists(target)) {
                if (hasMarker) return; // the pack removed it on purpose
            } else {
                if (!hasMarker) return; // someone else's overdrive.snbt
                int installed = readVersion(marker);
                if (installed >= CHAPTER_VERSION || !CommonConfig.UPDATE_FTB_QUESTS_CHAPTER.get()) return;
            }
            try (InputStream in = FtbQuestsCompat.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    Overdrive.LOGGER.error("Overdrive quest chapter {} missing from the jar", RESOURCE);
                    return;
                }
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "version=" + CHAPTER_VERSION + "\n");
            Overdrive.LOGGER.info("Installed Overdrive FTB Quests chapter v{} at {}", CHAPTER_VERSION, target);
        } catch (IOException e) {
            Overdrive.LOGGER.error("Could not install the Overdrive FTB Quests chapter", e);
        }
    }

    /** Version from the marker; markers from 1.0.0 (no version line) count as version 1. */
    static int readVersion(Path marker) throws IOException {
        for (String line : Files.readAllLines(marker)) {
            if (line.startsWith("version=")) {
                try {
                    return Integer.parseInt(line.substring(8).trim());
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 1;
    }
}
