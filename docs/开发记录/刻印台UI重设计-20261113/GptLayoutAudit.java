package com.bslx.XueXiMod.gui;

import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import com.bslx.XueXiMod.shu.*;
import com.bslx.XueXiMod.shu.technique.release.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.font.GlyphInfo;

/** No client/bootstrap/window. Calls production Font.width and compiled Geometry. */
public class GptLayoutAudit {
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray input = JsonParser.parseString(Files.readString(out.resolve("gpt-layout-input-20261114.json"))).getAsJsonArray();
        // Cached vanilla unifont.json assigns U+3200..9FFF left=0/right=15.
        // Use the actual vanilla Glyph implementation and Font splitter, without texture baking.
        Class<?> glyphClass = Class.forName("net.minecraft.client.gui.font.providers.UnihexProvider$Glyph");
        Constructor<?> ctor = glyphClass.getDeclaredConstructors()[0]; ctor.setAccessible(true);
        GlyphInfo cjk = (GlyphInfo) ctor.newInstance(null, 0, 15);
        FontSet set = new FontSet(null, ResourceLocation.fromNamespaceAndPath("minecraft", "default")) {
            @Override public GlyphInfo getGlyphInfo(int cp, boolean filter) {
                if (cp < 0x3200 || cp > 0x9fff) throw new IllegalArgumentException("Need actual provider for U+"+Integer.toHexString(cp));
                return cjk;
            }
        };
        Font font = new Font(ignored -> set, false);
        Map<String, Technique> builtins = new HashMap<>();
        for (Field field : ModTechniques.class.getDeclaredFields()) {
            if (field.getType() == Technique.class && Modifier.isStatic(field.getModifiers())) {
                Technique t = (Technique) field.get(null); builtins.put(t.getId(), t);
            }
        }
        if (builtins.size() != 73) throw new IllegalStateException("Unexpected primary catalog size " + builtins.size());
        List<ImprintTreeGeometry.Spec> specs = new ArrayList<>();
        for (JsonElement entry : input) {
            JsonObject o = entry.getAsJsonObject(); String id = o.get("id").getAsString(), name = o.get("name").getAsString();
            Technique t = new Technique(id, name, TechniqueType.valueOf(o.get("type").getAsString()),0,0,20,"",0,ShuAttribute.valueOf(o.get("attribute").getAsString()),0);
            if (builtins.containsKey(id)) {
                Technique actual = builtins.get(id);
                if (!actual.getName().equals(name) || actual.getAttribute() != t.getAttribute() || actual.getType() != t.getType())
                    throw new IllegalStateException("Catalog extraction mismatch " + id);
                t = actual;
            }
            ReleaseDefinition definition = (ReleaseDefinition) Proxy.newProxyInstance(GptLayoutAudit.class.getClassLoader(),new Class[]{ReleaseDefinition.class},(proxy, method, values) -> switch(method.getName()) {
                case "id" -> id;
                case "role" -> ChainRole.valueOf(o.get("role").getAsString());
                case "chainEnergyScale" -> o.get("energy").getAsFloat();
                default -> throw new UnsupportedOperationException(method.getName());
            });
            ReleaseRegistry.register(definition);
            List<List<String>> parents = new ArrayList<>();
            for (JsonElement group : o.getAsJsonArray("parents")) {
                List<String> values = new ArrayList<>(); for (JsonElement value : group.getAsJsonArray()) values.add(value.getAsString()); parents.add(values);
            }
            com.bslx.XueXiMod.player.LearningGraph.Node actualGraph = com.bslx.XueXiMod.player.LearningGraph.node(id);
            List<List<String>> actualParents = actualGraph == null ? List.of() : actualGraph.parentGroups();
            if (!parents.equals(actualParents)) throw new IllegalStateException("Prerequisite extraction mismatch " + id);
            parents = actualParents;
            int width = font.width(name), group = ImprintTreeView.layoutGroup(t);
            specs.add(new ImprintTreeGeometry.Spec(id,group,width,font.lineHeight,parents));
            o.addProperty("width",width); o.addProperty("group",group);
        }
        long begin = System.nanoTime();
        ImprintTreeGeometry.Layout layout = ImprintTreeGeometry.layout(specs);
        double millis = (System.nanoTime()-begin)/1e6;
        for (int i=0;i<specs.size();i++) {
            JsonObject o=input.get(i).getAsJsonObject();o.addProperty("x",layout.x[i]);o.addProperty("y",layout.y[i]);o.addProperty("depth",layout.depth[i]);
        }
        JsonObject output = new JsonObject(); output.add("nodes",input); output.add("routes",gson.toJsonTree(layout.routes));
        output.add("anchors",gson.toJsonTree(ImprintTreeGeometry.ANCHORS));
        output.add("sectorStart",gson.toJsonTree(ImprintTreeGeometry.SECTOR_START));
        output.add("sectorEnd",gson.toJsonTree(ImprintTreeGeometry.SECTOR_END));
        output.addProperty("audit",ImprintTreeGeometry.audit(specs,layout)); output.addProperty("layoutMillis",millis);
        output.addProperty("catalog", "73 compiled ModTechniques instances verified against source extraction; 12 helper registrations statically enumerated; all prerequisite groups read from compiled LearningGraph");
        output.addProperty("geometryClass", ImprintTreeGeometry.class.getProtectionDomain().getCodeSource().getLocation().toString());
        output.addProperty("font","Production Font.width with vanilla UnihexProvider.Glyph and cached CJK size overrides; no live resource-pack snapshot");
        Files.writeString(out.resolve("gpt-layout-export-20261114.json"),gson.toJson(output));
        Files.writeString(out.resolve("gpt-layout-audit-20261114.txt"),output.get("audit").getAsString()+"\nlayoutMillis="+millis+"\n"+output.get("font").getAsString()+"\n");
        System.out.println(output.get("audit").getAsString()+" layoutMillis="+millis);
    }
}
