package com.bslx.XueXiMod.gui;
import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


final class ImprintTreeView {
    private static final ResourceLocation NODE_SHEET = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/gui/imprint_desk/tree_nodes_sheet.png");
    private static final ResourceLocation TOOLTIP_FRAME = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/gui/imprint_desk/tooltip_frame_9slice.png");
    static final double MIN_ZOOM = ImprintTreeGeometry.MIN_ZOOM;
    static final ShuAttribute[] ELEMENT_ORDER = {
            ShuAttribute.WOOD, ShuAttribute.FIRE, ShuAttribute.WATER,
            ShuAttribute.EARTH, ShuAttribute.METAL, ShuAttribute.NONE
    };


    private static final int[] LINE_COLORS = {
            0xFF5CC64C, 0xFFF07E26, 0xFF3E9EF2, 0xFFB0733A, 0xFFDEB24C, 0xFF9474CE, 0xFFCC964A
    };
    private static final int LOCKED_LINE = 0xDC42465A;
    private static final int NAME_LEARNED = 0xFFECEEF6;
    private static final int NAME_STUDYABLE = 0xFFF0DCFF;
    private static final int NAME_LOCKED = 0xFF7A7E92;
    private static final int NAME_SELECTED = 0xFFFFE484;

    private final ImprintDeskScreen screen;
    private final List<TreeNode> nodes = new ArrayList<>();
    private final List<ImprintTreeGeometry.Route> routes = new ArrayList<>();
    private final Map<String, Integer> indexById = new HashMap<>();
    private long structureEpoch = Long.MIN_VALUE;
    private int fontStamp = Integer.MIN_VALUE;
    private int learningStamp = Integer.MIN_VALUE;
    private Set<String> learnedIds = Set.of();
    private int swordLevel = Integer.MIN_VALUE;
    private int magicLevel = Integer.MIN_VALUE;
    private boolean structureReady;
    private String tooltipSignature = "";
    private final List<String> tooltipLines = new ArrayList<>();
    private final List<Integer> tooltipColors = new ArrayList<>();

    ImprintTreeView(ImprintDeskScreen owner) {
        this.screen = owner;
    }


    String techniqueId(int index) {
        return nodes.get(index).technique.getId();
    }

    Technique technique(String techniqueId) {
        Integer index = indexById.get(techniqueId);
        return index == null ? null : nodes.get(index).technique;
    }

    void ensureStructure(Font font) {
        long epoch = ChainStructureEpoch.current();
        int stamp = font.lineHeight * 100000 + font.width("刻印台术式名称宽度");
        if (structureReady && epoch == structureEpoch && stamp == fontStamp) {
            return;
        }
        rebuildStructure(font);
        structureEpoch = epoch;
        fontStamp = stamp;
        structureReady = true;
        learningStamp = Integer.MIN_VALUE;
    }

    private void rebuildStructure(Font font) {
        nodes.clear();
        routes.clear();
        indexById.clear();
        List<ImprintTreeGeometry.Spec> specs = new ArrayList<>();
        List<Technique> techniques = new ArrayList<>();
        for (Technique technique : TechniqueManager.playerCatalog()) {
            if (!TechniqueAccess.allowsPlayerUse(technique)) {
                continue;
            }
            LearningGraph.Node graph = LearningGraph.node(technique.getId());
            specs.add(new ImprintTreeGeometry.Spec(technique.getId(), layoutGroup(technique),
                    font.width(technique.getName()), font.lineHeight, graph == null ? List.of() : graph.parentGroups()));
            techniques.add(technique);
        }
        ImprintTreeGeometry.Layout layout = ImprintTreeGeometry.layout(specs);
        for (int index = 0; index < techniques.size(); index++) {
            Technique technique = techniques.get(index);
            TreeNode node = new TreeNode(technique, specs.get(index).group);
            node.x = layout.x[index];
            node.y = layout.y[index];
            node.name = technique.getName();
            node.nameWidth = font.width(node.name);
            node.lineHeight = font.lineHeight;
            node.icon = icon(technique);
            nodes.add(node);
            indexById.put(technique.getId(), index);
        }
        routes.addAll(layout.routes);
    }


    void refreshLearning() {
        Set<String> live = ShuSyncPacket.ClientShuCache.getLearnedIds();
        int sword = ShuSyncPacket.ClientShuCache.getSwordLevel();
        int magic = ShuSyncPacket.ClientShuCache.getMagicLevel();
        int stamp = live.hashCode() ^ (sword * 31) ^ (magic * 131);
        if (stamp == learningStamp && swordLevel == sword && magicLevel == magic && learnedIds.equals(live)) {
            return;
        }
        learnedIds = Set.copyOf(live);
        swordLevel = sword;
        magicLevel = magic;
        learningStamp = stamp;
        PlayerShuData data = new PlayerShuData();
        for (String id : learnedIds) {
            data.learnTechnique(id);
        }
        for (TreeNode node : nodes) {
            boolean learned = learnedIds.contains(node.technique.getId());
            boolean unlocked = unlocked(node.technique);
            boolean parents = LearningGraph.parentsSatisfied(data, node.technique.getId());
            boolean graph = LearningGraph.node(node.technique.getId()) != null;
            if (learned) {
                node.state = unlocked ? NodeState.LEARNED : NodeState.LEARNED_LOCKED;
            } else if (graph && parents && unlocked) {
                node.state = NodeState.STUDYABLE;
            } else {
                node.state = NodeState.LOCKED;
            }
        }
    }

    private boolean unlocked(Technique technique) {
        if (technique.getType() == TechniqueType.UNIVERSAL || technique.getType() == TechniqueType.MOVEMENT) {
            return true;
        }
        int level = technique.getType() == TechniqueType.SWORD ? swordLevel : magicLevel;
        return level >= technique.getUnlockLevel();
    }


    void draw(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        ensureStructure(font);
        refreshLearning();
        graphics.flush();
        drawEdges(graphics, font);
        graphics.flush();
        int hovered = -1;
        for (int index = nodes.size() - 1; index >= 0; index--) {
            if (!nodeVisible(index)) {
                continue;
            }
            if (hits(index, mouseX, mouseY)) {
                hovered = index;
            }
            drawNode(graphics, font, index, hovered == index);
        }
        screen.hoveredNode = hovered;
    }

    private void drawEdges(GuiGraphics graphics, Font font) {
        graphics.flush();
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = graphics.pose().last().pose();
        for (ImprintTreeGeometry.Route route : routes) {
            drawRoute(buffer, pose, font, route, colorOf(route.child));
        }
        graphics.flush();
        // Shared turn stones are texture draws, after the single GUI line batch has flushed.
        Set<Long> stones = new java.util.HashSet<>();
        for (ImprintTreeGeometry.Route route : routes) {
            for (int p = 1; p + 1 < route.x.length; p++) {
                int x = (int) Math.round(screen.viewHubX + route.x[p] * screen.zoom);
                int y = (int) Math.round(screen.viewHubY + route.y[p] * screen.zoom);
                long key = ((long) x << 32) ^ (y & 0xffffffffL);
                if (stones.add(key) && !outsideContent(x, y) && !overNodeBounds(x, y))
                    blit(graphics, NODE_SHEET, x - 4, y - 4, 8, 8, 114, 26, 8, 8, 192, 56);
            }
        }
    }


    private boolean overNodeBounds(double x, double y) {
        for (int i = 0; i < nodes.size(); i++) {
            TreeNode node = nodes.get(i);
            double halfWidth = Math.max(17, node.nameWidth / 2.0 + 4);
            if (x + 4 >= screenX(i) - halfWidth && x - 4 <= screenX(i) + halfWidth
                    && y + 4 >= screenY(i) - 18 && y - 4 <= screenY(i) + 16 + node.lineHeight) return true;
        }
        return false;
    }

    private void drawRoute(VertexConsumer buffer, Matrix4f pose, Font font, ImprintTreeGeometry.Route route, int color) {
        for (int point = 1; point < route.x.length; point++) {
            if (point == 1 && route.parent == -1) {
                int[] anchor = ImprintTreeGeometry.ANCHORS[route.group];
                append(buffer, pose, font, screen.viewHubX + anchor[0], screen.viewHubY + anchor[1],
                        screen.viewHubX + route.x[point] * screen.zoom,
                        screen.viewHubY + route.y[point] * screen.zoom, color, -1, route.child);
                continue;
            }
            append(buffer, pose, font, screen.viewHubX + route.x[point - 1] * screen.zoom, screen.viewHubY + route.y[point - 1] * screen.zoom,
                    screen.viewHubX + route.x[point] * screen.zoom, screen.viewHubY + route.y[point] * screen.zoom,
                    color, point == 1 ? route.parent : -1, point == route.x.length - 1 ? route.child : -1);
        }
    }

    private void append(VertexConsumer buffer, Matrix4f pose, Font font,
                       double x1, double y1, double x2, double y2, int color, int parent, int child) {
        double[] visible = clip(x1, y1, x2, y2,
                screen.contentLeft, screen.contentTop, screen.contentRight, screen.contentBottom);
        if (visible == null) {
            return;
        }
        List<double[]> pieces = new ArrayList<>();
        pieces.add(visible);
        pieces = subtract(pieces, screen.altarX + 4, screen.altarY + 4, 176, 184);
        if (parent >= 0) {
            pieces = subtractNode(pieces, font, parent);
        }
        if (child >= 0) {
            pieces = subtractNode(pieces, font, child);
        }
        for (double[] piece : pieces) {
            segment(buffer, pose, piece[0], piece[1] + 1, piece[2], piece[3] + 1, 0xDC080910);
            segment(buffer, pose, piece[0], piece[1], piece[2], piece[3], color);
        }
    }


    private List<double[]> subtractNode(List<double[]> pieces, Font font, int index) {
        int x = (int) Math.round(screenX(index));
        int y = (int) Math.round(screenY(index));
        TreeNode node = nodes.get(index);
        int left = Math.min(x - 16, x - node.nameWidth / 2 - 3);
        int right = Math.max(x + 16, x + (node.nameWidth + 1) / 2 + 3);
        int top = y - 18;
        int bottom = y + 14 + font.lineHeight + 3;
        return subtract(pieces, left, top, right - left, bottom - top);
    }

    private List<double[]> subtract(List<double[]> pieces, int left, int top, int width, int height) {
        List<double[]> result = new ArrayList<>();
        for (double[] piece : pieces) {
            trim(piece[0], piece[1], piece[2], piece[3], left, top, left + width, top + height, result);
        }
        return result;
    }

    private void trim(double x1, double y1, double x2, double y2,
                      double left, double top, double right, double bottom, List<double[]> result) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx * dx + dy * dy < 1.0) {
            return;
        }
        double enter = 0.0;
        double leave = 1.0;
        double[] starts = {x1, x1, y1, y1};
        double[] limits = {left, right, top, bottom};
        double[] deltas = {dx, -dx, dy, -dy};
        double[] rates = {dx, dx, dy, dy};
        for (int edge = 0; edge < 4; edge++) {
            if (Math.abs(rates[edge]) < 1.0e-7) {
                if ((edge % 2 == 0 && starts[edge] < limits[edge]) || (edge % 2 == 1 && starts[edge] > limits[edge])) {
                    result.add(new double[]{x1, y1, x2, y2});
                    return;
                }
                continue;
            }
            double hit = (limits[edge] - starts[edge]) / rates[edge];
            if (deltas[edge] > 0.0) {
                enter = Math.max(enter, hit);
            } else {
                leave = Math.min(leave, hit);
            }
        }
        if (enter >= leave) {
            result.add(new double[]{x1, y1, x2, y2});
            return;
        }
        if (enter > 0.01) {
            result.add(new double[]{x1, y1, x1 + dx * enter, y1 + dy * enter});
        }
        if (leave < 0.99) {
            result.add(new double[]{x1 + dx * leave, y1 + dy * leave, x2, y2});
        }
    }


    private void segment(VertexConsumer buffer, Matrix4f pose,
                            double x1, double y1, double x2, double y2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 0.75) {
            return;
        }
        double px = -dy / length * 0.55;
        double py = dx / length * 0.55;
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        buffer.vertex(pose, (float) (x1 + px), (float) (y1 + py), 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(pose, (float) (x2 + px), (float) (y2 + py), 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(pose, (float) (x2 - px), (float) (y2 - py), 0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(pose, (float) (x1 - px), (float) (y1 - py), 0).color(red, green, blue, alpha).endVertex();
    }

    private int colorOf(int index) {
        NodeState state = nodes.get(index).state;
        if (state == NodeState.LOCKED || state == NodeState.LEARNED_LOCKED) {
            return LOCKED_LINE;
        }
        return LINE_COLORS[nodes.get(index).group];
    }


    private void drawNode(GuiGraphics graphics, Font font, int index, boolean hover) {
        TreeNode node = nodes.get(index);
        int x = (int) Math.round(screenX(index));
        int y = (int) Math.round(screenY(index));
        boolean selected = node.technique.getId().equals(screen.selectedTechniqueId);
        boolean locked = node.state == NodeState.LOCKED || node.state == NodeState.LEARNED_LOCKED;
        int sheet = locked ? 7 : node.group;
        blit(graphics, NODE_SHEET, x - 11, y - 11, 22, 22, sheet * 24, 0, 22, 22, 192, 56);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, node.state == NodeState.LOCKED ? 0.35F : 1.0F);
        graphics.blit(node.icon, x - 8, y - 9, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (locked) {
            blit(graphics, NODE_SHEET, x + 3, y + 1, 8, 9, 102, 26, 8, 9, 192, 56);
        }
        if (node.state == NodeState.STUDYABLE) {
            blit(graphics, NODE_SHEET, x - 13, y - 13, 26, 26, 28, 26, 26, 26, 192, 56);
            blit(graphics, NODE_SHEET, x + 5, y - 15, 10, 10, 88, 26, 10, 10, 192, 56);
        }
        if (selected) {
            blit(graphics, NODE_SHEET, x - 13, y - 13, 26, 26, 0, 26, 26, 26, 192, 56);
        } else if (hover) {
            blit(graphics, NODE_SHEET, x - 13, y - 13, 26, 26, 56, 26, 26, 26, 192, 56);
        }
        int color = selected ? NAME_SELECTED : node.state == NodeState.STUDYABLE ? NAME_STUDYABLE
                : node.state == NodeState.LOCKED ? NAME_LOCKED : NAME_LEARNED;
        graphics.drawString(font, node.name, x - node.nameWidth / 2, y + 13, color, false);
    }


    void drawTooltip(GuiGraphics graphics, Font font, int index) {
        TreeNode node = nodes.get(index);
        fillTooltip(node);
        int width = 24;
        for (String line : tooltipLines) {
            width = Math.max(width, font.width(line) + 16);
        }
        int height = tooltipLines.size() * 11 + 16;
        int x = (int) Math.round(screenX(index)) + 16;
        int y = (int) Math.round(screenY(index)) - 8;
        if (screen.inventoryOpen() && x + width > screen.drawerX - 6) {
            x = (int) Math.round(screenX(index)) - width - 16;
        }
        x = Math.max(4, Math.min(x, screen.width - width - 4));
        y = Math.max(4, Math.min(y, screen.height - height - 4));
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xF60E0B1A);
        blitNine(graphics, x, y, width, height);
        for (int line = 0; line < tooltipLines.size(); line++) {
            graphics.drawString(font, tooltipLines.get(line), x + 8, y + 8 + line * 11, tooltipColors.get(line), false);
        }
    }

    private void fillTooltip(TreeNode node) {
        Technique technique = node.technique;
        ItemStack embryo = screen.deskMenu().getEmbryoStack();
        String signature = technique.getId() + node.state + embryo.getCount() + embryo.getTag()
                + BalanceDisplay.damage(technique.getId(), technique.getBaseDamage())
                + BalanceDisplay.cooldown(technique.getId(), technique.getBaseCooldown());
        if (signature.equals(tooltipSignature)) {
            return;
        }
        tooltipSignature = signature;
        tooltipLines.clear();
        tooltipColors.clear();
        tip(technique.getName(), 0xFFFFE284);
        tip(label(node.state) + "  " + attributeName(technique.getAttribute()) + " · "
                + typeName(technique.getType()) + " · " + roleName(technique), 0xFFECEEF6);
        tip(parentsOf(technique), 0xFFD8D4E6);
        tip(node.state == NodeState.LEARNED || node.state == NodeState.LEARNED_LOCKED
                ? reactionOf(technique) : costOf(technique),
                node.state == NodeState.STUDYABLE || node.state == NodeState.LOCKED ? 0xFFE8C6FF : 0xFF8AFFA6);
        tip("伤害 " + number(BalanceDisplay.damage(technique.getId(), technique.getBaseDamage()))
                + "  冷却 " + BalanceDisplay.cooldown(technique.getId(), technique.getBaseCooldown()) + "t", 0xFFD8D4E6);
    }

    private void tip(String text, int color) {
        tooltipLines.add(text);
        tooltipColors.add(color);
    }


    private String parentsOf(Technique technique) {
        LearningGraph.Node graph = LearningGraph.node(technique.getId());
        if (graph == null || !graph.hasParents()) {
            return "前置：无";
        }
        StringBuilder text = new StringBuilder("前置：");
        for (int group = 0; group < graph.parentGroups().size(); group++) {
            if (group > 0) {
                text.append(" + ");
            }
            boolean met = false;
            List<String> parents = graph.parentGroups().get(group);
            for (int parent = 0; parent < parents.size(); parent++) {
                if (parent > 0) {
                    text.append("/");
                }
                Technique value = TechniqueManager.getById(parents.get(parent));
                text.append(value == null ? parents.get(parent) : value.getName());
                met |= learnedIds.contains(parents.get(parent));
            }
            text.append(met ? "✓" : "×");
        }
        return text.toString();
    }

    private String costOf(Technique technique) {
        LearningGraph.Stage stage = LearningGraph.stage(technique.getId());
        ShuAttribute attribute = LearningGraph.embryo(technique);
        ItemStack embryo = screen.deskMenu().getEmbryoStack();
        boolean ready = !embryo.isEmpty() && embryo.getItem() instanceof ShuEmbryoItem
                && ShuEmbryoItem.getAttribute(embryo) == attribute;
        return "参悟：" + attributeName(attribute) + "原胚" + (ready ? "✓" : "×") + " + " + stageName(stage);
    }

    private String reactionOf(Technique technique) {
        ItemStack embryo = screen.deskMenu().getEmbryoStack();
        if (embryo.isEmpty() || !(embryo.getItem() instanceof ShuEmbryoItem)) {
            return "刻印：放入原胚后显示五行相生";
        }
        ElementReaction reaction = ElementReaction.of(ShuEmbryoItem.getAttribute(embryo), technique.getAttribute());
        return reaction.kind() == ElementReaction.Kind.GENERATE ? "五行相生 +10%  " + reaction.describe() : reaction.describe();
    }


    int hit(double mouseX, double mouseY) {
        for (int index = nodes.size() - 1; index >= 0; index--) {
            if (hits(index, mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private boolean hits(int index, double mouseX, double mouseY) {
        if (!nodeVisible(index) || outsideContent(mouseX, mouseY)
                || screen.inventoryOpen() && overDrawer(index) || screen.coversControls(mouseX, mouseY)) {
            return false;
        }
        double x = screenX(index);
        double y = screenY(index);
        return mouseX >= x - 12.0 && mouseX <= x + 12.0 && mouseY >= y - 12.0 && mouseY <= y + 12.0;
    }

    private boolean overDrawer(int index) {
        double x = screenX(index);
        double y = screenY(index);
        return x >= screen.drawerX - 14 && x <= screen.drawerX + 192 && y >= screen.drawerY - 6 && y <= screen.drawerY + 122;
    }

    private boolean nodeVisible(int index) {
        TreeNode node = nodes.get(index);
        int x = (int) Math.round(screenX(index));
        int y = (int) Math.round(screenY(index));
        int left = Math.min(x - 18, x - node.nameWidth / 2);
        int right = Math.max(x + 18, x + node.nameWidth / 2);
        return right > screen.contentLeft && left < screen.contentRight
                && y + 14 + node.lineHeight > screen.contentTop && y - 20 < screen.contentBottom;
    }

    private boolean outsideContent(double x, double y) {
        return x < screen.contentLeft || y < screen.contentTop || x >= screen.contentRight || y >= screen.contentBottom;
    }

    private double screenX(int index) {
        return screen.viewHubX + nodes.get(index).x * screen.zoom;
    }

    private double screenY(int index) {
        return screen.viewHubY + nodes.get(index).y * screen.zoom;
    }


    private static double[] clip(double x1, double y1, double x2, double y2,
                                 double left, double top, double right, double bottom) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double start = 0.0;
        double end = 1.0;
        double[] points = {-dx, dx, -dy, dy};
        double[] limits = {x1 - left, right - x1, y1 - top, bottom - y1};
        for (int edge = 0; edge < 4; edge++) {
            if (Math.abs(points[edge]) < 1.0e-8) {
                if (limits[edge] < 0.0) {
                    return null;
                }
            } else {
                double hit = limits[edge] / points[edge];
                if (points[edge] < 0.0) {
                    start = Math.max(start, hit);
                } else {
                    end = Math.min(end, hit);
                }
                if (start > end) {
                    return null;
                }
            }
        }
        return new double[]{x1 + dx * start, y1 + dy * start, x1 + dx * end, y1 + dy * end};
    }


    private static ResourceLocation icon(Technique technique) {
        String kind = technique.getType() == TechniqueType.SWORD ? "attack" : "other";
        ShuAttribute attribute = technique.getAttribute();
        return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID,
                "textures/gui/technique/" + kind + "_" + (attribute == null ? "none" : attribute.getId()) + ".png");
    }

    private static String label(NodeState state) {
        return switch (state) {
            case LEARNED, LEARNED_LOCKED -> "已掌握";
            case STUDYABLE -> "可参悟";
            case LOCKED -> "未解锁";
        };
    }

    private static String stageName(LearningGraph.Stage stage) {
        if (stage == LearningGraph.Stage.MYSTIC) return "通玄残简";
        if (stage == LearningGraph.Stage.TRANSCENDENT) return "化境孤本";
        return "凡胎研习简";
    }

    private static String number(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
    }

    static String typeName(TechniqueType type) {
        return switch (type) {
            case SWORD -> "剑术";
            case MOVEMENT -> "动作";
            case MAGIC -> "术法";
            default -> "通用";
        };
    }

    static String attributeName(ShuAttribute attribute) {
        return switch (attribute) {
            case WOOD -> "木";
            case FIRE -> "火";
            case WATER -> "水";
            case EARTH -> "土";
            case METAL -> "金";
            default -> "无";
        };
    }


    static String roleName(Technique technique) {
        return switch (ruleKind(technique)) {
            case 0 -> "索敌";
            case 1 -> "弹道";
            case 2 -> "规则";
            case 3 -> "动作";
            default -> "攻击";
        };
    }

    static int ruleKind(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition != null && Float.isFinite(definition.chainEnergyScale())
                && Math.abs(definition.chainEnergyScale() - 1.0F) > 0.001F) {
            return 2;
        }
        ChainRole role = definition == null ? null : definition.role();
        if (role == null) {
            return technique.getType() == TechniqueType.MOVEMENT ? 3 : -1;
        }
        return switch (role) {
            case SEEK_SPACE, SEEK_ENTITY, SEEK_SURFACE -> 0;
            case TRAJECTORY_TRACKING, TRAJECTORY_DROP, TRAJECTORY_SCATTER,
                    TRAJECTORY_UPWELL, TRAJECTORY_PARALLEL, TRAJECTORY_MANIPULATION -> 1;
            case RULE_TRIPLE, RULE_DUAL, RULE_STAGGER, RULE_HIT_CONTINUE, RULE_DOMAIN_BIND, RULE_COOPERATIVE -> 2;
            case ACTION -> 3;
            default -> -1;
        };
    }

    static int layoutGroup(Technique technique) {
        // Element identity precedes release role: fire actions remain fire, water forms remain water.
        for (int i = 0; i < 5; i++) if (ELEMENT_ORDER[i] == technique.getAttribute()) return i;
        // Weapon manipulation, movement and cooperative clones form one nearby martial family.
        if (technique.getType() == TechniqueType.SWORD || technique.getType() == TechniqueType.MOVEMENT
                || technique.getId().equals("clone_body") || technique.getId().equals("rule_cooperative")) return 5;
        int kind = ruleKind(technique);
        return kind >= 0 && kind <= 2 || technique.getId().equals("test_sustained") ? 6 : 5;
    }


    private static void blit(GuiGraphics graphics, ResourceLocation texture,
                             int x, int y, int width, int height,
                             int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    private static void blitNine(GuiGraphics graphics, int x, int y, int width, int height) {
        int corner = 8;
        blit(graphics, TOOLTIP_FRAME, x, y, corner, corner, 0, 0, corner, corner, 24, 24);
        blit(graphics, TOOLTIP_FRAME, x + width - corner, y, corner, corner, 16, 0, corner, corner, 24, 24);
        blit(graphics, TOOLTIP_FRAME, x, y + height - corner, corner, corner, 0, 16, corner, corner, 24, 24);
        blit(graphics, TOOLTIP_FRAME, x + width - corner, y + height - corner, corner, corner, 16, 16, corner, corner, 24, 24);
        if (width > corner * 2) {
            blit(graphics, TOOLTIP_FRAME, x + corner, y, width - corner * 2, corner, 8, 0, 8, corner, 24, 24);
            blit(graphics, TOOLTIP_FRAME, x + corner, y + height - corner, width - corner * 2, corner, 8, 16, 8, corner, 24, 24);
        }
        if (height > corner * 2) {
            blit(graphics, TOOLTIP_FRAME, x, y + corner, corner, height - corner * 2, 0, 8, corner, 8, 24, 24);
            blit(graphics, TOOLTIP_FRAME, x + width - corner, y + corner, corner, height - corner * 2, 16, 8, corner, 8, 24, 24);
        }
    }

    private enum NodeState { LEARNED, LEARNED_LOCKED, STUDYABLE, LOCKED }

    private static final class TreeNode {
        private final Technique technique;
        private final int group;
        private double x;
        private double y;
        private String name = "";
        private int nameWidth;
        private int lineHeight = 9;
        private ResourceLocation icon;
        private NodeState state = NodeState.LOCKED;
        private TreeNode(Technique technique, int group) {
            this.technique = technique;
            this.group = group;
        }
    }

}


