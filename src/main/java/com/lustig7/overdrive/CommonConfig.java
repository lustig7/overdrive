package com.lustig7.overdrive;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Instance-wide settings (config/overdrive-common.toml), loaded before common setup. */
public final class CommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue INSTALL_FTB_QUESTS_CHAPTER = BUILDER
            .comment("If FTB Quests is installed, copy the Overdrive quest chapter into config/ftbquests/quests/chapters/.",
                     "A deleted chapter is never re-added (delete config/overdrive/ftbquests_chapter.installed to install it again).")
            .define("integration.ftbquests.installChapter", true);

    public static final ModConfigSpec.BooleanValue UPDATE_FTB_QUESTS_CHAPTER = BUILDER
            .comment("Replace the installed chapter when a newer Overdrive version ships a newer one.",
                     "Turn off if your pack edits the Overdrive chapter.")
            .define("integration.ftbquests.updateChapter", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CommonConfig() {}
}
