package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.network.ImprintPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.mixin.client.SlotPositionAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 刻印台：深空星图上的黑曜紫晶祭坛，参悟与刻印共用台面，背包按需展开。
 */
public class ImprintDeskScreen extends AbstractContainerScreen<ImprintDeskMenu> {
    private static final ResourceLocation ALTAR_GLOW = gui("altar_hub_glow_strip.png");
    private static final ResourceLocation ALTAR_WIDGETS = gui("altar_widgets.png");
    private static final ResourceLocation CONFIRM_BUTTONS = gui("confirm_button_states.png");
    private static final ResourceLocation CORNER_BUTTONS = gui("corner_button_states.png");
    private static final ResourceLocation INVENTORY_DRAWER = gui("inventory_drawer.png");
    private static final ResourceLocation SCREEN_BORDER = gui("screen_border_9slice.png");

    private static final int STAR_BG = 0xFF070A12;
    private static final int NEBULA = 0xFF171530;
    private static final int TITLE_IDLE = 0xFFEDE0FF;
    private static final int TITLE_READY = 0xFFFFF6FF;
    private static final int TITLE_DISABLED = 0xFF828094;
    private static final int CORNER_TEXT = 0xFFE4D8F4;
    private static final double ZOOM_LOWER_BOUND = ImprintTreeGeometry.MIN_ZOOM;
    private static final double ZOOM_UPPER_BOUND = ImprintTreeGeometry.MAX_ZOOM;
    private static final int ALTAR_WIDTH = 184;
    private static final int ALTAR_HEIGHT = 192;
    static final int FRAME_INSET = 10;
    private static final int DRAWER_OPEN_ALTAR_SHIFT_X = -32;
    private static final int DRAWER_WIDTH = 178;
    private static final int DRAWER_HEIGHT = 104;

    private boolean inventoryOpen;
    private final ImprintTreeView tree = new ImprintTreeView(this);
    String selectedTechniqueId;
    double zoom = 1.0;
    private double panX;
    private double panY;
    private boolean panning;
    private double lastDragX;
    private double lastDragY;
    private final List<Star> stars = new ArrayList<>();
    private int starWidth = -1;
    private int starHeight = -1;
    private boolean hubReady;
    private ItemStack previewStack = ItemStack.EMPTY;
    private String previewSignature = "";
    private int slipOwned;

    double hubCenterX;
    double hubCenterY;
    double viewHubX;
    double viewHubY;
    int altarX;
    int altarY;
    int drawerX;
    int drawerY;
    int contentLeft;
    int contentTop;
    int contentRight;
    int contentBottom;
    private boolean confirmReady;
    private boolean studying;
    int hoveredNode = -1;
    private boolean slot0Active;
    private boolean slot1Active;
    private boolean learnedSelected;
    private boolean unlockedSelected;
    private boolean studyableSelected;

    public ImprintDeskScreen(ImprintDeskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = ImprintDeskMenu.IMAGE_WIDTH;
        this.imageHeight = ImprintDeskMenu.IMAGE_HEIGHT;
    }

    ImprintDeskMenu deskMenu() {
        return menu;
    }

    Font textFont() {
        return font;
    }

    boolean inventoryOpen() {
        return inventoryOpen;
    }

    @Override
    protected void init() {
        super.init();
        refreshLayout();
    }

    private static ResourceLocation gui(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/gui/imprint_desk/" + fileName);
    }

    private void refreshLayout() {
        double shift = inventoryOpen && width < 600 ? DRAWER_OPEN_ALTAR_SHIFT_X : 0;
        double nextX = width / 2.0 + shift;
        double nextY = height / 2.0;
        if (hubReady) {
            panX += hubCenterX - nextX;
            panY += hubCenterY - nextY;
        }
        hubReady = true;
        hubCenterX = nextX;
        hubCenterY = nextY;
        drawerX = width - 184;
        drawerY = 28;
        contentLeft = FRAME_INSET;
        contentTop = FRAME_INSET;
        contentRight = Math.max(contentLeft + 1, width - FRAME_INSET);
        contentBottom = Math.max(contentTop + 1, height - FRAME_INSET);
        ensureStars();
        updateView();
    }

    private void updateView() {
        viewHubX = hubCenterX + panX * zoom;
        viewHubY = hubCenterY + panY * zoom;
        altarX = (int) Math.round(viewHubX) - 92;
        altarY = (int) Math.round(viewHubY) - 96;
        repositionSlots();
    }

    private void repositionSlots() {
        slot0Active = placeExistingSlot(0, altarX + 64, altarY + 74);
        slot1Active = placeExistingSlot(1, altarX + 104, altarY + 74);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int screenX = inventoryOpen ? drawerX + 9 + col * 18 : -1000;
                int screenY = inventoryOpen ? drawerY + 15 + row * 18 : -1000;
                placeExistingSlot(2 + row * 9 + col, screenX, screenY);
            }
        }
        for (int col = 0; col < 9; col++) {
            int screenX = inventoryOpen ? drawerX + 9 + col * 18 : -1000;
            int screenY = inventoryOpen ? drawerY + 73 : -1000;
            placeExistingSlot(29 + col, screenX, screenY);
        }
    }

    private boolean placeExistingSlot(int menuIndex, int screenX, int screenY) {
        boolean active = menuIndex >= 2 ? inventoryOpen : slotOnScreen(screenX, screenY) && !overlapsDrawer(screenX, screenY, 16, 16);
        Slot slot = menu.slots.get(menuIndex);
        int slotX = screenX - leftPos;
        int slotY = screenY - topPos;
        if (slot.x != slotX || slot.y != slotY) {
            SlotPositionAccess access = (SlotPositionAccess) (Object) slot;
            access.xuexi$setX(slotX);
            access.xuexi$setY(slotY);
        }
        menu.placeClientSlot(menuIndex, slotX, slotY, active);
        return active;
    }

    @Override
    protected void containerTick() {
        syncSelection();
    }

    private void syncSelection() {
        tree.ensureStructure(font);
        tree.refreshLearning();
        learnedSelected = selectedTechniqueId != null && ShuSyncPacket.ClientShuCache.hasLearned(selectedTechniqueId);
        Technique selected = tree.technique(selectedTechniqueId);
        unlockedSelected = selected != null && ShuSyncPacket.ClientShuCache.isUnlocked(selected);
        studyableSelected = selected != null && !learnedSelected && containsTechnique(menu.getStudyTechniques(), selectedTechniqueId);
        studying = selectedTechniqueId != null && !learnedSelected;
        confirmReady = studying ? studyableSelected && studyMaterialsReady() : learnedSelected && unlockedSelected && imprintReady();
        refreshPreview();
    }

    private boolean imprintReady() {
        return !menu.getEmbryoStack().isEmpty()
                && menu.getEmbryoStack().getItem() instanceof ShuEmbryoItem
                && menu.getOutputStack().isEmpty();
    }

    private boolean studyMaterialsReady() {
        Technique technique = selectedTechnique();
        if (technique == null || !embryoMatches(technique)) {
            return false;
        }
        LearningGraph.Stage stage = LearningGraph.stage(technique.getId());
        return stage != null && slipCount(stage) > 0;
    }

    private Technique selectedTechnique() {
        if (selectedTechniqueId == null) {
            return null;
        }
        for (Technique technique : menu.getLearnedTechniques()) {
            if (selectedTechniqueId.equals(technique.getId())) {
                return technique;
            }
        }
        for (Technique technique : menu.getStudyTechniques()) {
            if (selectedTechniqueId.equals(technique.getId())) {
                return technique;
            }
        }
        return tree.technique(selectedTechniqueId);
    }

    private boolean embryoMatches(Technique technique) {
        ItemStack embryo = menu.getEmbryoStack();
        return !embryo.isEmpty()
                && embryo.getItem() instanceof ShuEmbryoItem
                && ShuEmbryoItem.getAttribute(embryo) == LearningGraph.embryo(technique);
    }

    private int slipCount(LearningGraph.Stage stage) {
        return countSlip(stage);
    }

    private int countSlip(LearningGraph.Stage stage) {
        if (stage == null) {
            return 0;
        }
        int count = 0;
        Inventory inventory = menu.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(LearningGraph.slip(stage))) {
                count += stack.getCount();
            }
        }
        slipOwned = count;
        return count;
    }

    private void refreshPreview() {
        Technique technique = selectedTechnique();
        ItemStack embryo = menu.getEmbryoStack();
        String signature = (technique == null ? "" : technique.getId()) + "|" + studying + "|"
                + embryo.getItem() + "|" + embryo.getTag() + "|" + menu.getOutputStack().isEmpty() + "|" + slipOwned;
        if (signature.equals(previewSignature)) {
            return;
        }
        previewSignature = signature;
        if (technique == null || studying) {
            previewStack = ItemStack.EMPTY;
            return;
        }
        ShuAttribute material = embryo.getItem() instanceof ShuEmbryoItem
                ? ShuEmbryoItem.getAttribute(embryo) : technique.getAttribute();
        previewStack = SkillStoneItem.create(SkillStoneData.imprint(technique, material));
    }

    private static boolean containsTechnique(java.util.List<Technique> techniques, String techniqueId) {
        for (Technique technique : techniques) {
            if (technique.getId().equals(techniqueId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        refreshLayout();
        clipContent(guiGraphics);
        try {
            drawStars(guiGraphics);
            drawNebula(guiGraphics);
            tree.draw(guiGraphics, font, mouseX, mouseY);
            drawAltar(guiGraphics);
            drawAltarOverlay(guiGraphics, mouseX, mouseY);
        } finally {
            guiGraphics.disableScissor();
        }
        // The fixed drawer is outside the tree scissor, but behind its item contents.
        if (inventoryOpen) {
            blit(guiGraphics, INVENTORY_DRAWER, drawerX, drawerY, DRAWER_WIDTH, DRAWER_HEIGHT,
                    0, 0, DRAWER_WIDTH, DRAWER_HEIGHT);
        }
        clipContent(guiGraphics);
        try {
            // Slots are already container-local (screen position minus leftPos/topPos).
            // Vanilla adds that origin for items, highlights and carried stacks, and
            // subtracts it for hit testing. Keep the incoming pose in screen space.
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            drawSlotMarks(guiGraphics);
        } finally {
            guiGraphics.disableScissor();
        }
        blitNine(guiGraphics, SCREEN_BORDER, 0, 0, width, height, 24, 24, 8);
        drawCornerButtons(guiGraphics, mouseX, mouseY);
        if (hoveredNode >= 0 && hoveredSlot == null
                && !isOverDrawer(mouseX, mouseY) && !isOverCornerButtons(mouseX, mouseY)) {
            tree.drawTooltip(guiGraphics, font, hoveredNode);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, STAR_BG);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    private void clipContent(GuiGraphics graphics) {
        graphics.flush();
        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom);
    }

    private void drawNebula(GuiGraphics guiGraphics) {
        int centerX = (int) Math.round(viewHubX);
        int centerY = (int) Math.round(viewHubY);
        int red = (NEBULA >> 16) & 0xFF;
        int green = (NEBULA >> 8) & 0xFF;
        int blue = NEBULA & 0xFF;
        for (int ring = 5; ring >= 1; ring--) {
            int radiusX = 78 + ring * 28;
            int radiusY = 52 + ring * 18;
            int alpha = 18 + (6 - ring) * 10;
            guiGraphics.fill(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY,
                    (alpha << 24) | (red << 16) | (green << 8) | blue);
        }
    }

    private void ensureStars() {
        if (starWidth == width && starHeight == height) {
            return;
        }
        starWidth = Math.max(1, width);
        starHeight = Math.max(1, height);
        stars.clear();
        java.util.Random random = new java.util.Random(20261113L);
        int[] colors = {0xFF3A4866, 0xFF586A91, 0xFF869CC6, 0xFFC4D4F5, 0xFF8A62C4};
        for (int index = 0; index < 210; index++) {
            stars.add(new Star(random.nextInt(starWidth), random.nextInt(starHeight),
                    (index % 5) + 1, colors[random.nextInt(colors.length)]));
        }
    }

    private void drawStars(GuiGraphics graphics) {
        ensureStars();
        graphics.flush();
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = graphics.pose().last().pose();
        double parallax = panX * 0.08 + panY * 0.04;
        for (Star star : stars) {
            int x = Math.floorMod((int) Math.round(star.x - parallax * star.layer), starWidth);
            int y = Math.floorMod((int) Math.round(star.y + parallax * (star.layer % 3 + 1) * 0.4), starHeight);
            float alpha = ((star.color >>> 24) & 255) / 255.0F;
            float red = ((star.color >> 16) & 255) / 255.0F;
            float green = ((star.color >> 8) & 255) / 255.0F;
            float blue = (star.color & 255) / 255.0F;
            buffer.vertex(pose, x, y, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(pose, x, y + 1, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(pose, x + 1, y + 1, 0).color(red, green, blue, alpha).endVertex();
            buffer.vertex(pose, x + 1, y, 0).color(red, green, blue, alpha).endVertex();
        }
        graphics.flush();
    }

    private void drawAltar(GuiGraphics guiGraphics) {
        int frameIndex = (int) ((net.minecraft.Util.getMillis() / 120L) % 8L);
        blit(guiGraphics, ALTAR_GLOW, altarX, altarY, ALTAR_WIDTH, ALTAR_HEIGHT,
                0, frameIndex * ALTAR_HEIGHT, ALTAR_WIDTH, ALTAR_HEIGHT, ALTAR_WIDTH, ALTAR_HEIGHT * 8);
    }

    private void drawAltarOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String plaque = studying ? "\u53c2\u609f \u00b7 " + stageName(LearningGraph.stage(selectedTechniqueId)) : "\u539f\u80da\u523b\u5370";
        guiGraphics.drawString(font, plaque, altarX + 92 - font.width(plaque) / 2, altarY + 61, TITLE_IDLE, false);
        if (studying) {
            blit(guiGraphics, ALTAR_WIDGETS, altarX + 85, altarY + 75, 14, 14,
                    confirmReady ? 48 : 32, 0, 14, 14, 64, 32);
        } else {
            blit(guiGraphics, ALTAR_WIDGETS, altarX + 84, altarY + 76, 16, 12,
                    confirmReady ? 16 : 0, 0, 16, 12, 64, 32);
        }
        drawConfirmButton(guiGraphics, mouseX, mouseY);
    }

    private void drawSlotMarks(GuiGraphics guiGraphics) {
        Technique technique = selectedTechnique();
        ItemStack embryo = menu.getEmbryoStack();
        if (slot0Active) {
            int embryoBadge = embryoBadge(technique, embryo);
            if (embryoBadge >= 0) {
                blit(guiGraphics, ALTAR_WIDGETS, altarX + 74, altarY + 84, 7, 7, embryoBadge, 16, 7, 7, 64, 32);
            }
        }
        if (!slot1Active) {
            return;
        }
        if (!studying) {
            if (menu.getOutputStack().isEmpty() && !previewStack.isEmpty()) {
                blit(guiGraphics, ALTAR_WIDGETS, altarX + 104, altarY + 74, 16, 16, 32, 16, 16, 16, 64, 32);
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.58F);
                guiGraphics.renderItem(previewStack, altarX + 104, altarY + 74);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            return;
        }
        if (technique == null || !menu.getOutputStack().isEmpty()) {
            return;
        }
        LearningGraph.Stage stage = LearningGraph.stage(technique.getId());
        if (stage == null) {
            return;
        }
        guiGraphics.renderItem(new ItemStack(LearningGraph.slip(stage)), altarX + 104, altarY + 74);
        int owned = countSlip(stage);
        String count = owned + "/1";
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        guiGraphics.drawString(font, count, altarX + 120 - font.width(count), altarY + 83,
                owned > 0 ? 0xFFE8C6FF : 0xFFFF8A8A, true);
        blit(guiGraphics, ALTAR_WIDGETS, altarX + 114, altarY + 84, 7, 7, owned > 0 ? 0 : 16, 16, 7, 7, 64, 32);
        guiGraphics.pose().popPose();
    }

    private int embryoBadge(Technique technique, ItemStack embryo) {
        if (technique == null) {
            return -1;
        }
        if (embryo.isEmpty() || !(embryo.getItem() instanceof ShuEmbryoItem)) {
            return 16;
        }
        if (studying) {
            return embryoMatches(technique) ? 0 : 8;
        }
        ElementReaction reaction = ElementReaction.of(ShuEmbryoItem.getAttribute(embryo), technique.getAttribute());
        if (reaction.kind() == ElementReaction.Kind.GENERATE || reaction.kind() == ElementReaction.Kind.SAME) {
            return 0;
        }
        return 8;
    }

    private void drawConfirmButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonX = altarX + 62;
        int buttonY = altarY + 96;
        boolean hover = confirmReady && isOverConfirm(mouseX, mouseY);
        int textureV = confirmReady ? (hover ? 20 : 0) : 40;
        blit(guiGraphics, CONFIRM_BUTTONS, buttonX, buttonY, 60, 20, 0, textureV, 60, 20, 60, 60);
        String label = studying ? "\u53c2\u609f" : "\u523b\u5370";
        int color = confirmReady ? (hover ? TITLE_READY : TITLE_IDLE) : TITLE_DISABLED;
        guiGraphics.drawString(font, label, buttonX + 30 - font.width(label) / 2, buttonY + 6, color, false);
    }

    private void drawCornerButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawCornerButton(guiGraphics, width - 118, 8, "\u5f52\u4f4d",
                contains(mouseX, mouseY, width - 118, 8, 52, 18) ? 18 : 0);
        int inventoryV = inventoryOpen ? 36 : (contains(mouseX, mouseY, width - 60, 8, 52, 18) ? 18 : 0);
        drawCornerButton(guiGraphics, width - 60, 8, inventoryOpen ? "\u6536\u8d77\u80cc\u5305" : "\u80cc\u5305", inventoryV);
    }

    private void drawCornerButton(GuiGraphics guiGraphics, int x, int y, String label, int textureV) {
        blit(guiGraphics, CORNER_BUTTONS, x, y, 52, 18, 0, textureV, 52, 18, 52, 54);
        guiGraphics.pose().pushPose();
        float scale = label.length() > 2 ? 0.72F : 1.0F;
        float drawX = x + 26 - font.width(label) * scale / 2.0F;
        float drawY = y + (18 - 8 * scale) / 2.0F;
        guiGraphics.pose().translate(drawX, drawY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, label, 0, 0, CORNER_TEXT, false);
        guiGraphics.pose().popPose();
    }

    private static String stageName(LearningGraph.Stage stage) {
        if (stage == LearningGraph.Stage.MYSTIC) {
            return "\u901a\u7384";
        }
        if (stage == LearningGraph.Stage.TRANSCENDENT) {
            return "\u5316\u5883";
        }
        return "\u51e1\u80ce";
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private void confirmSelected() {
        if (!confirmReady || selectedTechniqueId == null) {
            return;
        }
        ImprintPacket.Action action = studying ? ImprintPacket.Action.STUDY : ImprintPacket.Action.IMPRINT;
        ModNetwork.CHANNEL.sendToServer(new ImprintPacket(selectedTechniqueId, action));
    }

    private boolean isOverDrawer(double mouseX, double mouseY) {
        return inventoryOpen && contains(mouseX, mouseY, drawerX, drawerY, DRAWER_WIDTH, DRAWER_HEIGHT);
    }

    private boolean overlapsDrawer(int x, int y, int boxWidth, int boxHeight) {
        return inventoryOpen
                && x < drawerX + DRAWER_WIDTH && x + boxWidth > drawerX
                && y < drawerY + DRAWER_HEIGHT && y + boxHeight > drawerY;
    }

    private boolean isOverCornerButtons(double mouseX, double mouseY) {
        return contains(mouseX, mouseY, width - 118, 8, 52, 18)
                || contains(mouseX, mouseY, width - 60, 8, 52, 18);
    }

    private boolean isOverConfirm(double mouseX, double mouseY) {
        int buttonX = altarX + 62;
        int buttonY = altarY + 96;
        if (buttonX >= contentRight || buttonY >= contentBottom || buttonX + 60 <= contentLeft || buttonY + 20 <= contentTop) {
            return false;
        }
        return contains(mouseX, mouseY, buttonX, buttonY, 60, 20)
                && !isOverDrawer(mouseX, mouseY)
                && !isOverCornerButtons(mouseX, mouseY);
    }

    private boolean slotOnScreen(int x, int y) {
        return x < contentRight && y < contentBottom && x + 16 > contentLeft && y + 16 > contentTop;
    }

    boolean coversControls(double mouseX, double mouseY) {
        return isOverDrawer(mouseX, mouseY) || isOverCornerButtons(mouseX, mouseY) || isOverConfirm(mouseX, mouseY);
    }

    private boolean isOverInteractive(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int slotScreenX = leftPos + slot.x;
            int slotScreenY = topPos + slot.y;
            if (mouseX >= slotScreenX - 1 && mouseX < slotScreenX + 17
                    && mouseY >= slotScreenY - 1 && mouseY < slotScreenY + 17) {
                return true;
            }
        }
        return coversControls(mouseX, mouseY);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int boxWidth, int boxHeight) {
        return mouseX >= x && mouseX < x + boxWidth && mouseY >= y && mouseY < y + boxHeight;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < 0 || mouseY < 0 || mouseX >= this.width || mouseY >= this.height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverCornerButtons(mouseX, mouseY)) {
            if (contains(mouseX, mouseY, width - 118, 8, 52, 18)) {
                panX = 0;
                panY = 0;
                zoom = 1.0;
            } else {
                inventoryOpen = !inventoryOpen;
            }
            refreshLayout();
            return true;
        }
        if (isOverDrawer(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0 && isOverConfirm(mouseX, mouseY)) {
            confirmSelected();
            return true;
        }
        if (button == 0 && !isOverInteractive(mouseX, mouseY) && menu.getCarried().isEmpty()) {
            syncSelection();
            int hit = tree.hit(mouseX, mouseY);
            if (hit >= 0) {
                selectedTechniqueId = tree.techniqueId(hit);
                return true;
            }
            panning = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning && menu.getCarried().isEmpty()) {
            panX += mouseX - lastDragX;
            panY += mouseY - lastDragY;
            lastDragX = mouseX;
            lastDragY = mouseY;
            updateView();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panning) {
            panning = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isOverDrawer(mouseX, mouseY) || isOverCornerButtons(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (delta == 0) {
            return true;
        }
        double scale = delta > 0 ? 1.1 : 1.0 / 1.1;
        double oldZoom = zoom;
        double newZoom = Math.max(ZOOM_LOWER_BOUND, Math.min(ZOOM_UPPER_BOUND, oldZoom * scale));
        if (newZoom == zoom) {
            return true;
        }
        zoom = newZoom;
        panX = mouseX - (mouseX - viewHubX) * newZoom / oldZoom - hubCenterX;
        panY = mouseY - (mouseY - viewHubY) * newZoom / oldZoom - hubCenterY;
        updateView();
        return true;
    }

    private record Star(int x, int y, int layer, int color) {
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture,
                             int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight) {
        blit(graphics, texture, x, y, width, height, u, v, regionWidth, regionHeight, regionWidth, regionHeight);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture,
                             int x, int y, int width, int height,
                             int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    private static void blitNine(GuiGraphics graphics, ResourceLocation texture,
                                 int x, int y, int width, int height, int sourceWidth, int sourceHeight, int corner) {
        blit(graphics, texture, x, y, corner, corner, 0, 0, corner, corner, sourceWidth, sourceHeight);
        blit(graphics, texture, x + width - corner, y, corner, corner,
                sourceWidth - corner, 0, corner, corner, sourceWidth, sourceHeight);
        blit(graphics, texture, x, y + height - corner, corner, corner,
                0, sourceHeight - corner, corner, corner, sourceWidth, sourceHeight);
        blit(graphics, texture, x + width - corner, y + height - corner, corner, corner,
                sourceWidth - corner, sourceHeight - corner, corner, corner, sourceWidth, sourceHeight);
        if (width > corner * 2) {
            blit(graphics, texture, x + corner, y, width - corner * 2, corner,
                    corner, 0, sourceWidth - corner * 2, corner, sourceWidth, sourceHeight);
            blit(graphics, texture, x + corner, y + height - corner, width - corner * 2, corner,
                    corner, sourceHeight - corner, sourceWidth - corner * 2, corner, sourceWidth, sourceHeight);
        }
        if (height > corner * 2) {
            blit(graphics, texture, x, y + corner, corner, height - corner * 2,
                    0, corner, corner, sourceHeight - corner * 2, sourceWidth, sourceHeight);
            blit(graphics, texture, x + width - corner, y + corner, corner, height - corner * 2,
                    sourceWidth - corner, corner, corner, sourceHeight - corner * 2, sourceWidth, sourceHeight);
        }
    }
}
