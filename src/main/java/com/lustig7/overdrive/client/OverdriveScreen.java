package com.lustig7.overdrive.client;

import com.lustig7.overdrive.Config;
import com.lustig7.overdrive.block.OverdriveBlock;
import com.lustig7.overdrive.block.OverdriveBlockEntity;
import com.lustig7.overdrive.item.OverclockChipItem;
import com.lustig7.overdrive.logic.CoolingSource;
import com.lustig7.overdrive.logic.HeatStatus;
import com.lustig7.overdrive.logic.OverdriveMath;
import com.lustig7.overdrive.menu.OverdriveMenu;
import com.lustig7.overdrive.item.UpgradeType;
import com.lustig7.overdrive.logic.RedstoneMode;
import com.lustig7.overdrive.network.CycleRedstoneModePayload;
import com.lustig7.overdrive.network.SetMultiplierPayload;
import net.minecraft.world.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Dark tech-themed Overdrive GUI, drawn with plain fills (no texture). */
public class OverdriveScreen extends AbstractContainerScreen<OverdriveMenu> {
    // palette
    private static final int PANEL = 0xFF1F2329, BORDER = 0xFF2C3139, INNER = 0xFF181B20, SLOT = 0xFF15181C;
    private static final int WELL = 0xFF0F1114, BUTTON = 0xFF262B32, BUTTON_BORDER = 0xFF3A404A, BUTTON_HOVER = 0xFF313741;
    private static final int TEXT = 0xFFE6E9EE, MUTED = 0xFF9AA3AD, DISABLED = 0xFF4A5059, ON_ACCENT = 0xFF06161C;
    private static final int ENERGY = 0xFFA78BFA, OK = 0xFF4CC9F0, WARN = 0xFFF59E0B, CRIT = 0xFFFF5A4E;

    // layout (relative to leftPos/topPos)
    private static final int W = 198, H = 260;
    private static final int BAR_Y = 24, BAR_H = 62;
    private static final int FE_X = 8, HEAT_X = 158, COOL_X = 176, BAR_W = 12;
    private static final int STATS_X = 26, STATS_Y = 50, STATS_W = 126, STATS_H = 44;
    private static final int ROW_Y = 100, ROW_H = 14, MINUS_X = 10, STEP_X = 26, STEP_W = 19, STEP_GAP = 2, PLUS_X = 173, PM_W = 14;
    private static final int CHIP_BOX_Y = 117, CHIP_BOX_H = 25;
    private static final int UPG_BOX_Y = 145, UPG_BOX_H = 25, UPG_TEXT_X = 56;
    private static final int RS_X = W - 30, RS_Y = 148, RS_W = 20, RS_H = 19;

    public OverdriveScreen(OverdriveMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
    }

    // ------------------------------------------------------------------ background

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0);

        g.fill(1, 0, W - 1, H, BORDER);
        g.fill(0, 1, W, H - 1, BORDER);
        g.fill(2, 2, W - 2, H - 2, PANEL);

        // vertical bars
        HeatStatus status = menu.status();
        int cap = Math.max(1, menu.capacity());
        drawBar(g, FE_X, (double) menu.energy() / cap, ENERGY);
        drawBar(g, HEAT_X, menu.heat() / 100.0, status == HeatStatus.OFF ? OK : status.color);
        marker(g, HEAT_X, Config.get(Config.THROTTLE_THRESHOLD) / 100.0, WARN);
        marker(g, HEAT_X, Config.get(Config.DAMAGE_THRESHOLD) / 100.0, CRIT);
        drawBar(g, COOL_X, (double) menu.tankAmount() / Math.max(1, menu.tankCapacity()), menu.tankFluid().color);

        // stats box
        box(g, STATS_X, STATS_Y, STATS_W, STATS_H, INNER, BORDER);

        // multiplier row
        int limit = menu.limit();
        int selected = menu.selected();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        button(g, MINUS_X, PM_W, hover(mx, my, MINUS_X, PM_W) ? BUTTON_HOVER : BUTTON, BUTTON_BORDER);
        button(g, PLUS_X, PM_W, hover(mx, my, PLUS_X, PM_W) ? BUTTON_HOVER : BUTTON, BUTTON_BORDER);
        for (int i = 0; i < OverdriveMath.STEPS.length; i++) {
            int step = OverdriveMath.STEPS[i];
            int x = stepX(i);
            boolean on = step == selected;
            boolean enabled = step <= limit;
            int bg = on ? OK : (enabled && hover(mx, my, x, STEP_W) ? BUTTON_HOVER : BUTTON);
            button(g, x, STEP_W, bg, on ? OK : BUTTON_BORDER);
        }

        // chip row
        box(g, 8, CHIP_BOX_Y, W - 16, CHIP_BOX_H, INNER, BORDER);

        // upgrade row + redstone button
        box(g, 8, UPG_BOX_Y, W - 16, UPG_BOX_H, INNER, BORDER);
        boolean rsHover = inside(mx, my, RS_X, RS_Y, RS_W, RS_H);
        box(g, RS_X, RS_Y, RS_W, RS_H, rsHover ? BUTTON_HOVER : BUTTON, menu.redstoneBlocked() ? CRIT : BUTTON_BORDER);
        g.renderItem(redstoneIcon(menu.redstoneMode()), RS_X + 2, RS_Y + 1);

        // slots
        for (Slot slot : menu.slots) {
            box(g, slot.x - 1, slot.y - 1, 18, 18, SLOT, slot.index < OverdriveMenu.PLAYER_START ? BUTTON_BORDER : BORDER);
        }
        g.pose().popPose();
    }

    private static ItemStack redstoneIcon(RedstoneMode mode) {
        return new ItemStack(switch (mode) {
            case IGNORE -> Items.GUNPOWDER;
            case ACTIVE_HIGH -> Items.REDSTONE_TORCH;
            case ACTIVE_LOW -> Items.LEVER;
        });
    }

    private static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void button(GuiGraphics g, int x, int w, int fill, int border) {
        box(g, x, ROW_Y, w, ROW_H, fill, border);
    }

    private static void drawBar(GuiGraphics g, int x, double fraction, int color) {
        box(g, x, BAR_Y, BAR_W, BAR_H, WELL, BORDER);
        int inner = BAR_H - 2;
        int h = (int) Math.round(inner * Math.max(0.0, Math.min(1.0, fraction)));
        if (h > 0) g.fill(x + 1, BAR_Y + 1 + inner - h, x + BAR_W - 1, BAR_Y + BAR_H - 1, color);
    }

    private static void marker(GuiGraphics g, int x, double fraction, int color) {
        int inner = BAR_H - 2;
        int y = BAR_Y + 1 + inner - (int) Math.round(inner * fraction);
        g.fill(x + 1, y, x + BAR_W - 1, y + 1, color);
    }

    private static int stepX(int i) {
        return STEP_X + i * (STEP_W + STEP_GAP);
    }

    private static boolean hover(int mx, int my, int x, int w) {
        return mx >= x && mx < x + w && my >= ROW_Y && my < ROW_Y + ROW_H;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ foreground

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        HeatStatus status = menu.status();

        // title + status pill
        g.drawString(font, title, 8, 8, TEXT, false);
        boolean held = menu.redstoneBlocked() && status == HeatStatus.OFF;
        Component pill = Component.translatable(held ? "gui.overdrive.status.redstone" : status.translationKey());
        int pw = font.width(pill) + 8;
        int px = W - 8 - pw;
        boolean off = status == HeatStatus.OFF;
        g.fill(px + 1, 5, px + pw - 1, 18, off ? BORDER : status.color);
        g.fill(px, 6, px + pw, 17, off ? BORDER : status.color);
        g.drawString(font, pill, px + 4, 7, off ? MUTED : ON_ACCENT, false);

        // bar labels
        smallCentered(g, "FE", FE_X + BAR_W / 2, BAR_Y + BAR_H + 2);
        smallCentered(g, "HEAT", HEAT_X + BAR_W / 2, BAR_Y + BAR_H + 2);
        smallCentered(g, "COOL", COOL_X + BAR_W / 2, BAR_Y + BAR_H + 2);

        // big multiplier readout
        String big = menu.selected() + "×";
        g.pose().pushPose();
        g.pose().translate(28, 25, 0);
        g.pose().scale(2.5F, 2.5F, 1F);
        g.drawString(font, big, 0, 0, 0xFFF2F4F7, false);
        g.pose().popPose();

        // target info next to the readout
        int tx = 28 + (int) Math.ceil(font.width(big) * 2.5F) + 6;
        int maxW = STATS_X + STATS_W - tx - 2;
        g.drawString(font, Component.translatable("gui.overdrive.target"), tx, 27, MUTED, false);
        Component target = targetLine();
        int targetColor = switch (menu.targetStatus()) {
            case OverdriveBlockEntity.TARGET_OK -> TEXT;
            case OverdriveBlockEntity.TARGET_BLOCKED -> CRIT;
            case OverdriveBlockEntity.TARGET_NOT_TICKING -> WARN;
            default -> MUTED;
        };
        g.drawString(font, font.plainSubstrByWidth(target.getString(), maxW), tx, 37, targetColor, false);

        // stats
        int effective = menu.effective();
        int hot = status.ordinal() >= HeatStatus.THROTTLING.ordinal() ? status.color : TEXT;
        statLine(g, 0, "gui.overdrive.usage", String.format("%,d FE/t", menu.cost()), TEXT);
        statLine(g, 1, "gui.overdrive.effective", effective + "×", effective < menu.selected() ? hot == TEXT ? WARN : hot : TEXT);
        statLine(g, 2, "gui.overdrive.runtime", runtime(menu.energy(), menu.cost()), TEXT);
        statLine(g, 3, "gui.overdrive.heat", String.format("%.1f %%", menu.heat()), hot);

        // multiplier buttons
        int limit = menu.limit();
        centered(g, "−", MINUS_X + PM_W / 2, ROW_Y + 3, TEXT);
        centered(g, "+", PLUS_X + PM_W / 2, ROW_Y + 3, TEXT);
        for (int i = 0; i < OverdriveMath.STEPS.length; i++) {
            int step = OverdriveMath.STEPS[i];
            boolean on = step == menu.selected();
            int color = on ? ON_ACCENT : step <= limit ? 0xFFD5DAE0 : DISABLED;
            centered(g, step + "×", stepX(i) + STEP_W / 2, ROW_Y + 3, color);
        }

        // chip info
        ItemStack chip = menu.chip();
        int cx = 34;
        if (chip.getItem() instanceof OverclockChipItem c) {
            g.drawString(font, Component.translatable("gui.overdrive.chip.tier", c.getTier(), c.getMaxMultiplier()), cx, 121, TEXT, false);
            int max = Math.max(1, chip.getMaxDamage());
            double left = 1.0 - (double) chip.getDamageValue() / max;
            int barW = 76;
            g.fill(cx, 133, cx + barW, 136, WELL);
            int col = left > 0.5 ? OK : left > 0.2 ? WARN : CRIT;
            g.fill(cx, 133, cx + (int) Math.round(barW * left), 136, col);
        } else {
            g.drawString(font, Component.translatable("gui.overdrive.chip.none", limit), cx, 121, MUTED, false);
            g.drawString(font, Component.translatable("gui.overdrive.chip.hint"), cx, 131, DISABLED, false);
        }
        Component coolLabel = Component.translatable("gui.overdrive.cooling");
        CoolingSource source = menu.coolingSource();
        Component coolValue = Component.translatable(source.translationKey(), menu.passiveBlocks());
        g.drawString(font, coolLabel, W - 12 - font.width(coolLabel), 121, MUTED, false);
        g.drawString(font, coolValue, W - 12 - font.width(coolValue), 131, source.isFluid() ? source.color : TEXT, false);

        // upgrade summary
        g.drawString(font, Component.translatable("gui.overdrive.upgrades"), UPG_TEXT_X, UPG_BOX_Y + 4, MUTED, false);
        String summary = upgradeSummary();
        int sumW = RS_X - UPG_TEXT_X - 4;
        if (summary.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("gui.overdrive.upgrades.hint").getString(), sumW),
                    UPG_TEXT_X, UPG_BOX_Y + 14, DISABLED, false);
        } else {
            g.drawString(font, font.plainSubstrByWidth(summary, sumW), UPG_TEXT_X, UPG_BOX_Y + 14, OK, false);
        }

        g.drawString(font, Component.translatable("gui.overdrive.inventory"), OverdriveMenu.INV_X, OverdriveMenu.INV_Y - 10, MUTED, false);
    }

    /** e.g. "COOL ×1.5  FE ×0.75  MAX ×2"; empty without upgrades. */
    private String upgradeSummary() {
        StringBuilder sb = new StringBuilder();
        int sinks = menu.upgrades(UpgradeType.HEAT_SINK);
        int eff = menu.upgrades(UpgradeType.EFFICIENCY);
        int over = menu.upgrades(UpgradeType.OVERCHARGE);
        if (sinks > 0) sb.append("COOL ×").append(num(OverdriveMath.upgradeFactor(Config.get(Config.UPGRADE_HEAT_SINK), sinks))).append("  ");
        if (eff > 0) sb.append("FE ×").append(num(OverdriveMath.upgradeFactor(Config.get(Config.UPGRADE_EFFICIENCY), eff))).append("  ");
        if (over > 0) sb.append("MAX ×").append(1 << over).append(" HEAT ×")
                .append(num(OverdriveMath.upgradeFactor(Config.get(Config.UPGRADE_OVERCHARGE_HEAT), over)));
        return sb.toString().trim();
    }

    private static String num(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Component targetLine() {
        return switch (menu.targetStatus()) {
            case OverdriveBlockEntity.TARGET_BLOCKED -> Component.translatable("gui.overdrive.target.blocked");
            case OverdriveBlockEntity.TARGET_NOT_TICKING -> Component.translatable("gui.overdrive.target.not_ticking");
            case OverdriveBlockEntity.TARGET_OK -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) yield Component.empty();
                BlockState self = mc.level.getBlockState(menu.getPos());
                if (!self.hasProperty(OverdriveBlock.FACING)) yield Component.empty();
                yield mc.level.getBlockState(menu.getPos().relative(self.getValue(OverdriveBlock.FACING))).getBlock().getName();
            }
            default -> Component.translatable("gui.overdrive.target.none");
        };
    }

    private void statLine(GuiGraphics g, int row, String key, String value, int valueColor) {
        int y = STATS_Y + 4 + row * 10;
        g.drawString(font, Component.translatable(key), STATS_X + 4, y, MUTED, false);
        g.drawString(font, value, STATS_X + STATS_W - 4 - font.width(value), y, valueColor, false);
    }

    private void centered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(font, s, cx - font.width(s) / 2, y, color, false);
    }

    private void smallCentered(GuiGraphics g, String s, int cx, int y) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(0.6F, 0.6F, 1F);
        g.drawString(font, s, -font.width(s) / 2, 0, MUTED, false);
        g.pose().popPose();
    }

    static String runtime(int energy, int cost) {
        if (cost <= 0) return "∞";
        double secs = energy / (cost * 20.0);
        if (secs >= 3600) return (int) (secs / 3600) + "h " + (int) (secs % 3600 / 60) + "m";
        if (secs >= 60) return (int) (secs / 60) + "m " + (int) (secs % 60) + "s";
        return String.format("%.1fs", secs);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int mx = mouseX - leftPos, my = mouseY - topPos;
        List<Component> tip = new ArrayList<>();
        if (inside(mx, my, FE_X, BAR_Y, BAR_W, BAR_H)) {
            tip.add(Component.translatable("gui.overdrive.tooltip.energy",
                    String.format("%,d", menu.energy()), String.format("%,d", menu.capacity())));
        } else if (inside(mx, my, HEAT_X, BAR_Y, BAR_W, BAR_H)) {
            tip.add(Component.translatable("gui.overdrive.tooltip.heat", String.format("%.1f", menu.heat())));
            tip.add(Component.translatable("gui.overdrive.tooltip.thresholds",
                    (int) Config.get(Config.THROTTLE_THRESHOLD), (int) Config.get(Config.DAMAGE_THRESHOLD)));
        } else if (inside(mx, my, COOL_X, BAR_Y, BAR_W, BAR_H)) {
            CoolingSource fluid = menu.tankFluid();
            tip.add(fluid.isFluid()
                    ? Component.translatable("gui.overdrive.fluid." + fluid.name().toLowerCase(java.util.Locale.ROOT))
                    : Component.translatable("gui.overdrive.tooltip.tank_empty"));
            tip.add(Component.translatable("gui.overdrive.tooltip.tank",
                    String.format("%,d", menu.tankAmount()), String.format("%,d", menu.tankCapacity())));
            tip.add(Component.translatable("gui.overdrive.tooltip.cooling", String.format("%,d", menu.coolingCapacity())));
            CoolingSource source = menu.coolingSource();
            if (source.isFluid()) {
                double rate = Config.get(source == CoolingSource.CRYO ? Config.CRYO_USAGE : Config.WATER_USAGE);
                tip.add(Component.translatable("gui.overdrive.tooltip.coolant_use",
                        String.format("%.2f", OverdriveMath.coolantPerTick(menu.effective(), rate))));
            } else {
                tip.add(Component.translatable("gui.overdrive.tooltip.coolant_hint"));
            }
        } else if (inside(mx, my, RS_X, RS_Y, RS_W, RS_H)) {
            tip.add(Component.translatable("gui.overdrive.redstone"));
            tip.add(Component.translatable(menu.redstoneMode().translationKey()).withStyle(net.minecraft.ChatFormatting.AQUA));
            if (menu.redstoneBlocked()) tip.add(Component.translatable("gui.overdrive.redstone.blocked").withStyle(net.minecraft.ChatFormatting.RED));
            tip.add(Component.translatable("gui.overdrive.redstone.comparator").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX - leftPos, my = (int) mouseY - topPos;
            int limit = menu.limit();
            int idx = indexOf(Math.min(menu.selected(), limit));
            if (inside(mx, my, RS_X, RS_Y, RS_W, RS_H)) {
                PacketDistributor.sendToServer(new CycleRedstoneModePayload(menu.containerId));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            int pick = -1;
            if (hover(mx, my, MINUS_X, PM_W)) pick = OverdriveMath.STEPS[Math.max(0, idx - 1)];
            else if (hover(mx, my, PLUS_X, PM_W)) pick = Math.min(limit, OverdriveMath.STEPS[Math.min(OverdriveMath.STEPS.length - 1, idx + 1)]);
            else {
                for (int i = 0; i < OverdriveMath.STEPS.length; i++) {
                    if (hover(mx, my, stepX(i), STEP_W) && OverdriveMath.STEPS[i] <= limit) pick = OverdriveMath.STEPS[i];
                }
            }
            if (pick > 0) {
                PacketDistributor.sendToServer(new SetMultiplierPayload(menu.containerId, pick));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int indexOf(int step) {
        for (int i = 0; i < OverdriveMath.STEPS.length; i++) if (OverdriveMath.STEPS[i] == step) return i;
        return 0;
    }
}
