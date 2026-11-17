package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class ImprintTreeView {
    static final int LINE_COLOR = 0xFF3E4C66;
    static final int NODE_SELECT = 0xFFFFE36A;
    static final int NODE_NAME = 0xFFD8D8D8;
    static final int NODE_SIZE = 12;
    static final double INNER_RADIUS = 78.0;
    static final double NODE_GAP = 36.0;
    static final ShuAttribute[] ELEMENT_ORDER = {
            ShuAttribute.WOOD, ShuAttribute.FIRE, ShuAttribute.WATER,
            ShuAttribute.EARTH, ShuAttribute.METAL, ShuAttribute.NONE
    };

    private final ImprintDeskScreen screen;
    private final List<Technique> nodeTechniques = new ArrayList<>();
    private final List<Double> nodeAngles = new ArrayList<>();
    private final List<Double> nodeRadii = new ArrayList<>();
    private final List<Integer> nodeColors = new ArrayList<>();

    ImprintTreeView(ImprintDeskScreen screen) {
        this.screen = screen;
    }

    String techniqueId(int index) {
        return nodeTechniques.get(index).getId();
    }

    void refreshNodes() {
        nodeTechniques.clear();
        nodeAngles.clear();
        nodeRadii.clear();
        nodeColors.clear();

        List<Technique> learned = screen.deskMenu().getLearnedTechniques();
        int ruleGroup = ELEMENT_ORDER.length;
        int[] groupSizes = new int[ruleGroup + 1];
        for (Technique technique : learned) {
            groupSizes[layoutGroup(technique)]++;
        }
        int groupCount = 0;
        for (int size : groupSizes) {
            if (size > 0) groupCount++;
        }
        if (groupCount == 0) return;

        int sector = 0;
        for (int group = 0; group < groupSizes.length; group++) {
            if (groupSizes[group] == 0) continue;
            double centerAngle = (sector + 0.5) * 2.0 * Math.PI / groupCount;
            int within = 0;
            int groupSize = groupSizes[group];
            if (group == ruleGroup) {
                for (int kind = 0; kind < 4; kind++) {
                    for (Technique technique : learned) {
                        if (ruleKind(technique) != kind) continue;
                        addNode(technique, centerAngle, within, groupSize, groupCount);
                        within++;
                    }
                }
            } else {
                for (Technique technique : learned) {
                    if (layoutGroup(technique) != group) continue;
                    addNode(technique, centerAngle, within, groupSize, groupCount);
                    within++;
                }
            }
            sector++;
        }
    }

    void addNode(Technique technique, double centerAngle, int within, int groupSize, int groupCount) {
        int columns = Math.min(3, Math.max(1, groupSize));
        int ring = within / columns;
        int columnIndex = within % columns;
        int onRing = Math.min(columns, groupSize - ring * columns);
        double sector = 2.0 * Math.PI / Math.max(1, groupCount);
        double span = sector * 0.62;
        double step = onRing <= 1 ? 0.0 : span / (onRing - 1);
        double offset = onRing <= 1 ? 0.0 : (columnIndex - (onRing - 1) / 2.0) * step;
        double radiusForStep = step <= 1.0e-4 ? INNER_RADIUS : NODE_GAP / step;
        double radiusForNeighbors = NODE_GAP / (sector * 0.38);
        double base = Math.max(INNER_RADIUS, Math.max(radiusForStep, radiusForNeighbors));
        nodeTechniques.add(technique);
        nodeAngles.add(centerAngle + offset);
        nodeRadii.add(base + ring * NODE_GAP);
        nodeColors.add(attributeColor(technique.getAttribute()));
    }

    void drawNodeLines(GuiGraphics guiGraphics) {
        for (int nodeIndex = 0; nodeIndex < nodeTechniques.size(); nodeIndex++) {
            double endX = nodeScreenX(nodeIndex);
            double endY = nodeScreenY(nodeIndex);
            if (isGroupStart(nodeIndex)) {
                if (!nodeInsidePanel(endX, endY)) {
                    double[] edge = panelEdgeToward(endX, endY);
                    drawLine(guiGraphics, edge[0], edge[1], endX, endY, LINE_COLOR);
                }
            } else {
                drawLine(guiGraphics, nodeScreenX(nodeIndex - 1), nodeScreenY(nodeIndex - 1), endX, endY, LINE_COLOR);
            }
        }
    }

    boolean isGroupStart(int nodeIndex) {
        if (nodeIndex == 0) return true;
        return layoutGroup(nodeTechniques.get(nodeIndex)) != layoutGroup(nodeTechniques.get(nodeIndex - 1));
    }

    boolean nodeInsidePanel(double screenX, double screenY) {
        return screenX >= screen.panelLeft && screenX <= screen.panelRight && screenY >= screen.panelTop && screenY <= screen.panelBottom;
    }

    double[] panelEdgeToward(double screenX, double screenY) {
        double deltaX = screenX - screen.viewHubX;
        double deltaY = screenY - screen.viewHubY;
        double lowerBound = Double.MAX_VALUE;
        if (deltaX > 1.0e-6) {
            lowerBound = Math.min(lowerBound, (screen.panelRight - screen.viewHubX) / deltaX);
        } else if (deltaX < -1.0e-6) {
            lowerBound = Math.min(lowerBound, (screen.panelLeft - screen.viewHubX) / deltaX);
        }
        if (deltaY > 1.0e-6) {
            lowerBound = Math.min(lowerBound, (screen.panelBottom - screen.viewHubY) / deltaY);
        } else if (deltaY < -1.0e-6) {
            lowerBound = Math.min(lowerBound, (screen.panelTop - screen.viewHubY) / deltaY);
        }
        if (lowerBound == Double.MAX_VALUE || lowerBound < 0) lowerBound = 0;
        return new double[]{screen.viewHubX + deltaX * lowerBound, screen.viewHubY + deltaY * lowerBound};
    }

    void drawLine(GuiGraphics guiGraphics, double startX, double startY, double endX, double endY, int color) {
        double deltaX = endX - startX;
        double deltaY = endY - startY;
        int steps = (int) Math.ceil(Math.max(Math.abs(deltaX), Math.abs(deltaY)));
        if (steps <= 0) {
            if (!nodeInsidePanel(startX, startY)) {
                int pixelX = (int) Math.round(startX);
                int pixelY = (int) Math.round(startY);
                guiGraphics.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color);
            }
            return;
        }
        for (int stepIndex = 0; stepIndex <= steps; stepIndex++) {
            double stepFraction = (double) stepIndex / steps;
            double pointX = startX + deltaX * stepFraction;
            double pointY = startY + deltaY * stepFraction;
            if (nodeInsidePanel(pointX, pointY)) continue;
            int pixelX = (int) Math.round(pointX);
            int pixelY = (int) Math.round(pointY);
            guiGraphics.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color);
        }
    }

    int drawNodes(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int hovered = -1;
        for (int nodeIndex = 0; nodeIndex < nodeTechniques.size(); nodeIndex++) {
            double screenX = nodeScreenX(nodeIndex);
            double screenY = nodeScreenY(nodeIndex);
            int screenPixelX = (int) Math.round(screenX);
            int screenPixelY = (int) Math.round(screenY);
            boolean isHover = isOverNode(screenX, screenY, mouseX, mouseY);
            if (isHover) hovered = nodeIndex;

            int color = nodeColors.get(nodeIndex);
            if (isHover) color = brighten(color, 0x30);
            boolean selected = nodeTechniques.get(nodeIndex).getId().equals(screen.selectedTechniqueId);
            if (selected) {
                guiGraphics.fill(screenPixelX - NODE_SIZE / 2 - 1, screenPixelY - NODE_SIZE / 2 - 1,
                        screenPixelX + NODE_SIZE / 2 + 1, screenPixelY + NODE_SIZE / 2 + 1, NODE_SELECT);
            }
            guiGraphics.fill(screenPixelX - NODE_SIZE / 2, screenPixelY - NODE_SIZE / 2, screenPixelX + NODE_SIZE / 2, screenPixelY + NODE_SIZE / 2, color);

            if (screen.zoom >= 0.7 && isGroupStart(nodeIndex) && isRule(nodeTechniques.get(nodeIndex))) {
                double labelRadius = Math.max(52.0, nodeRadii.get(nodeIndex) - NODE_GAP);
                int labelX = (int) Math.round(screen.viewHubX + Math.cos(nodeAngles.get(nodeIndex)) * labelRadius * screen.zoom);
                int labelY = (int) Math.round(screen.viewHubY + Math.sin(nodeAngles.get(nodeIndex)) * labelRadius * screen.zoom);
                guiGraphics.drawString(screen.textFont(), "规则", labelX - screen.textFont().width("规则") / 2, labelY - 4, 0xFFE7D7A1, false);
            }
            boolean showName = isHover || selected || screen.zoom * NODE_GAP >= 56.0;
            if (showName) {
                guiGraphics.drawString(screen.textFont(), nodeTechniques.get(nodeIndex).getName(), screenPixelX + NODE_SIZE / 2 + 4, screenPixelY - 4,
                        selected ? NODE_SELECT : NODE_NAME, false);
            }
        }
        return hovered;
    }

    void drawNodeTooltip(GuiGraphics guiGraphics, int index) {
        Technique technique = nodeTechniques.get(index);
        ItemStack embryo = screen.deskMenu().getEmbryoStack();

        List<String> lines = new ArrayList<>();
        lines.add("类型：" + typeName(technique.getType()) + "  属性：" + attributeName(technique.getAttribute()));
        lines.add("描述：" + technique.getDescription());
        if (!embryo.isEmpty()) {
            ShuAttribute material = ShuEmbryoItem.getAttribute(embryo);
            SkillStoneData preview = SkillStoneData.imprint(technique, material);
            String reaction = ElementReaction.of(material, technique.getAttribute()).describe();
            lines.add("伤害 " + format(preview.getDamage()) + "  冷却 " + preview.getCooldown() + "t  " + reaction);
        } else {
            lines.add("伤害 " + format(technique.getBaseDamage()) + "  冷却 " + technique.getBaseCooldown() + "t");
        }

        List<String> wrapped = new ArrayList<>();
        for (String line : lines) {
            wrapped.addAll(wrapText(line, 190));
        }

        int lineHeight = 10;
        int padding = 5;
        int boxWidth = padding * 2;
        for (String line : wrapped) {
            boxWidth = Math.max(boxWidth, padding * 2 + screen.textFont().width(line));
        }
        int boxHeight = padding * 2 + wrapped.size() * lineHeight;

        int tooltipX = (int) Math.round(nodeScreenX(index)) + 10;
        int tooltipY = (int) Math.round(nodeScreenY(index)) + 8;
        tooltipX = Math.max(2, Math.min(tooltipX, screen.width - boxWidth - 2));
        tooltipY = Math.max(2, Math.min(tooltipY, screen.height - boxHeight - 2));

        guiGraphics.fill(tooltipX, tooltipY, tooltipX + boxWidth, tooltipY + boxHeight, 0xE6000000);
        for (int lineIndex = 0; lineIndex < wrapped.size(); lineIndex++) {
            guiGraphics.drawString(screen.textFont(), wrapped.get(lineIndex), tooltipX + padding, tooltipY + padding + lineIndex * lineHeight, 0xFFFFFFFF, false);
        }
    }

    List<String> wrapText(String text, int upperBound) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int charIndex = 0; charIndex < text.length(); ) {
            int codePoint = text.codePointAt(charIndex);
            int charCount = Character.charCount(codePoint);
            String character = text.substring(charIndex, charIndex + charCount);
            if (current.length() > 0 && screen.textFont().width(current.toString() + character) > upperBound) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(character);
            charIndex += charCount;
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    boolean isOverNode(double screenX, double screenY, double mouseX, double mouseY) {
        return mouseX >= screenX - NODE_SIZE / 2.0 - 1 && mouseX <= screenX + NODE_SIZE / 2.0 + 1
                && mouseY >= screenY - NODE_SIZE / 2.0 - 1 && mouseY <= screenY + NODE_SIZE / 2.0 + 1;
    }

    int hitNode(double mouseX, double mouseY) {
        refreshNodes();
        for (int nodeIndex = 0; nodeIndex < nodeTechniques.size(); nodeIndex++) {
            if (isOverNode(nodeScreenX(nodeIndex), nodeScreenY(nodeIndex), mouseX, mouseY)) {
                return nodeIndex;
            }
        }
        return -1;
    }

    double nodeScreenX(int nodeIndex) {
        double nodeX = Math.cos(nodeAngles.get(nodeIndex)) * nodeRadii.get(nodeIndex);
        return screen.viewHubX + nodeX * screen.zoom;
    }

    double nodeScreenY(int nodeIndex) {
        double nodeY = Math.sin(nodeAngles.get(nodeIndex)) * nodeRadii.get(nodeIndex);
        return screen.viewHubY + nodeY * screen.zoom;
    }
    static String format(float value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.1f", value);
    }

    static String typeName(TechniqueType type) {
        switch (type) {
            case SWORD: return "剑术";
            case MOVEMENT: return "动作";
            case MAGIC: return "法术";
            default: return "通用";
        }
    }

    static String attributeName(ShuAttribute attribute) {
        switch (attribute) {
            case NONE: return "无";
            case WOOD: return "木";
            case FIRE: return "火";
            case WATER: return "水";
            case EARTH: return "土";
            default: return "金";
        }
    }

    static int ruleKind(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition != null) {
            float supplied = definition.chainEnergyScale();
            if (Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f) {
                return 2;
            }
        }
        ChainRole role = definition == null ? null : definition.role();
        if (role == null) {
            return technique.getType() == TechniqueType.MOVEMENT ? 3 : -1;
        }
        switch (role) {
            case SEEK_SPACE:
            case SEEK_ENTITY:
            case SEEK_SURFACE:
                return 0;
            case TRAJECTORY_TRACKING:
            case TRAJECTORY_DROP:
            case TRAJECTORY_SCATTER:
            case TRAJECTORY_UPWELL:
                return 1;
            case RULE_TRIPLE:
            case RULE_DUAL:
            case RULE_STAGGER:
            case RULE_HIT_CONTINUE:
                return 2;
            case ACTION:
                return 3;
            default:
                return -1;
        }
    }

    static boolean isRule(Technique technique) {
        return ruleKind(technique) >= 0;
    }

    static int layoutGroup(Technique technique) {
        if (isRule(technique)) return ELEMENT_ORDER.length;
        for (int elementIndex = 0; elementIndex < ELEMENT_ORDER.length; elementIndex++) {
            if (ELEMENT_ORDER[elementIndex] == technique.getAttribute()) return elementIndex;
        }
        return ELEMENT_ORDER.length - 1;
    }

    static int attributeColor(ShuAttribute attribute) {
        switch (attribute) {
            case WOOD: return 0xFF3C8C40;
            case FIRE: return 0xFFD25A1C;
            case WATER: return 0xFF2E6FD6;
            case EARTH: return 0xFF8A5A2A;
            case METAL: return 0xFFD4B24A;
            default: return 0xFF8A8A8A;
        }
    }

    static int brighten(int color, int amount) {
        int alpha = (color >> 24) & 0xFF;
        int red = Math.min(0xFF, ((color >> 16) & 0xFF) + amount);
        int green = Math.min(0xFF, ((color >> 8) & 0xFF) + amount);
        int blue = Math.min(0xFF, (color & 0xFF) + amount);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
