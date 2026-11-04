package com.sorrowmist.useless.client.gui;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;

/** Local staff appearance and G-window dimensions; gameplay and server settings are untouched. */
final class StaffUiPreferences {
    static final java.util.Map<String,int[]> groups=new java.util.HashMap<>();
    static boolean aeStyle;
    static int width = -1, height = -1;
    private static boolean loaded;
    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve("useless_mod-staff-ui.json"); }
    static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.isRegularFile(path())) return;
            var data = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            if(data.has("groups"))for(var entry:data.getAsJsonObject("groups").entrySet()){var a=entry.getValue().getAsJsonArray();groups.put(entry.getKey(),new int[]{Math.max(100,Math.min(800,a.get(0).getAsInt())),Math.max(40,Math.min(1600,a.get(1).getAsInt()))});}
            if (data.has("aeStyle")) aeStyle = data.get("aeStyle").getAsBoolean();
            if (data.has("width")) width = Math.max(-1, Math.min(8192, data.get("width").getAsInt()));
            if (data.has("height")) height = Math.max(-1, Math.min(8192, data.get("height").getAsInt()));
        } catch (Exception error) { LogUtils.getLogger().warn("Cannot read staff UI preferences", error); }
    }
    static void save() {
        try {
            var data = new JsonObject(); var sizes=new JsonObject();groups.forEach((key,size)->{var a=new JsonArray();a.add(size[0]);a.add(size[1]);sizes.add(key,a);});data.add("groups",sizes); data.addProperty("aeStyle", aeStyle);
            data.addProperty("width", width); data.addProperty("height", height);
            Files.createDirectories(path().getParent());
            var temp = path().resolveSibling(path().getFileName() + ".tmp");
            Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(data));
            Files.move(temp, path(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception error) { LogUtils.getLogger().warn("Cannot save staff UI preferences", error); }
    }
}
