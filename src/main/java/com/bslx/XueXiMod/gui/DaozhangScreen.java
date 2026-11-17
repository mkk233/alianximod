package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.network.DaozhangActionPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.npc.daozhang.DaozhangCourse;
import com.bslx.XueXiMod.npc.daozhang.DaozhangRelation;
import com.bslx.XueXiMod.player.LearningGraph;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** 原版界面底图上的拜师和课程选择，不是商人界面。 */
public class DaozhangScreen extends AbstractContainerScreen<DaozhangMenu> {
    private static final int ROWS = 6;
    private int seenRevision = -1;
    private DaozhangCourse.Kind kind = DaozhangCourse.Kind.SWORD;
    private int scroll;
    private String feedback = "";

    public DaozhangScreen(DaozhangMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 248;
        this.imageHeight = 196;
    }

    @Override
    protected void init() {
        super.init();
        this.seenRevision = this.menu.revision();
        this.rebuild();
        this.send(new DaozhangActionPacket(DaozhangActionPacket.Action.REFRESH,
                this.menu.daozhangId(), this.menu.sessionNonce(),
                this.menu.containerId, this.nextSequence(), ""));
    }

    @Override
    protected void containerTick() {
        if (this.menu.revision() != this.seenRevision) {
            this.seenRevision = this.menu.revision();
            this.rebuild();
        }
    }

    private void rebuild() {
        this.clearWidgets();
        int max = Math.max(0, this.filtered().size() - ROWS);
        this.scroll = Math.max(0, Math.min(max, this.scroll));
        int x = this.leftPos;
        int y = this.topPos;
        if (this.menu.relation() == DaozhangRelation.NEUTRAL) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.xueximod.daozhang.apprentice"),
                    button -> this.send(new DaozhangActionPacket(DaozhangActionPacket.Action.APPRENTICE,
                            this.menu.daozhangId(), this.menu.sessionNonce(),
                            this.menu.containerId, this.nextSequence(), "")))
                    .bounds(x + 74, y + 86, 100, 20).build());
            return;
        }
        if (this.menu.relation() == DaozhangRelation.HOSTILE) {
            return;
        }
        this.addRenderableWidget(Button.builder(Component.translatable("gui.xueximod.daozhang.sword"),
                button -> this.select(DaozhangCourse.Kind.SWORD)).bounds(x + 8, y + 22, 72, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.xueximod.daozhang.spell"),
                button -> this.select(DaozhangCourse.Kind.SPELL)).bounds(x + 84, y + 22, 72, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), button -> this.move(-1))
                .bounds(x + 220, y + 22, 12, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.move(1))
                .bounds(x + 234, y + 22, 12, 16).build());
        List<DaozhangCourse> visible = this.visible();
        for (int row = 0; row < Math.min(ROWS, visible.size()); row++) {
            DaozhangCourse course = visible.get(row);
            int rowY = y + 44 + row * 22;
            this.addRenderableWidget(Button.builder(Component.translatable("gui.xueximod.daozhang.study"),
                    button -> this.study(course)).bounds(x + 188, rowY, 52, 16).build())
                    .active = course.canStudy();
        }
    }

    private void select(DaozhangCourse.Kind next) {
        this.kind = next;
        this.scroll = 0;
        this.rebuild();
    }

    private void move(int delta) {
        int max = Math.max(0, this.filtered().size() - ROWS);
        this.scroll = Math.max(0, Math.min(max, this.scroll + delta));
        this.rebuild();
    }

    private void study(DaozhangCourse course) {
        this.feedback = course.techniqueId();
        this.send(new DaozhangActionPacket(DaozhangActionPacket.Action.STUDY,
                this.menu.daozhangId(), this.menu.sessionNonce(),
                this.menu.containerId, this.nextSequence(), course.techniqueId()));
    }

    private void send(DaozhangActionPacket packet) {
        ModNetwork.CHANNEL.sendToServer(packet);
    }

    private int nextSequence() {
        return this.menu.claimSequence();
    }

    private List<DaozhangCourse> filtered() {
        List<DaozhangCourse> result = new ArrayList<>();
        for (DaozhangCourse course : this.menu.courses()) {
            if (course.kind() == this.kind) {
                result.add(course);
            }
        }
        return result;
    }

    private List<DaozhangCourse> visible() {
        List<DaozhangCourse> filtered = this.filtered();
        int end = Math.min(filtered.size(), this.scroll + ROWS);
        return this.scroll >= end ? List.of() : filtered.subList(this.scroll, end);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight, 0xF0101010);
        graphics.fill(this.leftPos + 2, this.topPos + 2, this.leftPos + this.imageWidth - 2,
                this.topPos + this.imageHeight - 2, 0xFF2B2418);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xF2E2B8, false);
        Component relation = Component.translatable("gui.xueximod.daozhang.relation." + this.menu.relation().name().toLowerCase());
        graphics.drawString(this.font, relation, 150, 6, 0xFFFFFF, false);
        if (this.menu.relation() == DaozhangRelation.NEUTRAL) {
            graphics.drawWordWrap(this.font, Component.translatable("gui.xueximod.daozhang.neutral"), 12, 48, 224, 0xE8D7AE);
        } else if (this.menu.relation() == DaozhangRelation.HOSTILE) {
            graphics.drawWordWrap(this.font, Component.translatable("gui.xueximod.daozhang.hostile"), 12, 48, 224, 0xE8D7AE);
        } else {
            List<DaozhangCourse> visible = this.visible();
            for (int row = 0; row < visible.size(); row++) {
                this.drawCourse(graphics, visible.get(row), 44 + row * 22);
            }
        }
        if (!this.feedback.isBlank()) {
            graphics.drawString(this.font, Component.translatable("gui.xueximod.daozhang.sent", this.feedback),
                    8, this.imageHeight - 12, 0xC8C8C8, false);
        }
    }

    private void drawCourse(GuiGraphics graphics, DaozhangCourse course, int y) {
        graphics.drawString(this.font, course.name(), 8, y, course.canStudy() ? 0xFFFFFF : 0xA0A0A0, false);
        String parents = course.parents().isBlank() ? "-" : course.parents();
        String state = (course.learned() ? "已学" : "未学") + " / " + (course.unlocked() ? "可施放" : "等级未开")
                + " / " + (course.parentsSatisfied() ? "前置已满足" : "前置未满足");
        graphics.drawString(this.font, Component.translatable(stageKey(course.stage()))
                .append(" " + parents + " " + state), 8, y + 9, 0xD0C090, false);
    }

    private static String stageKey(LearningGraph.Stage stage) {
        return switch (stage) {
            case MORTAL -> "gui.xueximod.daozhang.stage.mortal";
            case MYSTIC -> "gui.xueximod.daozhang.stage.mystic";
            case TRANSCENDENT -> "gui.xueximod.daozhang.stage.transcendent";
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
