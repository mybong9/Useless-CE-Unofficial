package com.sorrowmist.useless.client.gui;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sorrowmist.useless.core.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.joml.Matrix4f;
import java.util.*;

/** Hierarchical native configuration UI with a shared transactional editing session. */
public final class UselessConfigScreen extends ConfigFrame {
    private final Screen parent;
    public UselessConfigScreen(Screen parent) { super(Component.literal("Useless Mod配置")); this.parent = parent; }
    private int sectionStep() { return Math.min(50, Math.max(32, (height - 80) / 3)); }
    private int sectionTop() { return Math.max(height >= 220 ? 40 : 34, (height - (2 * sectionStep() + 72)) / 2); }
    public static Screen forSpec(Screen parent, Component title, ForgeConfigSpec spec, Runnable afterSave) {
        if (!spec.isLoaded() || !Minecraft.getInstance().hasSingleplayerServer()) return new Unavailable(parent, title);
        return new Category(parent, new Session(spec, afterSave), List.of(), List.of(title.getString()));
    }
    @Override protected void init() {
        int y = sectionTop(), step = sectionStep();
        type("通用设置…", "Useless Mod通用配置", "useless_mod-common.toml", ConfigManager.COMMON_SPEC, y, true);
        type("客户端设置…", "Useless Mod客户端配置", "useless_mod-client.toml", ConfigManager.CLIENT_SPEC, y + step, true);
        type("服务端设置…", "Useless Mod服务端配置", "useless_mod-server.toml", ConfigManager.SERVER_SPEC, y + 2 * step, minecraft.hasSingleplayerServer());
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose()).bounds(width / 2 - 100, height - 22, 200, 20).build());
    }
    private void type(String label, String name, String file, ForgeConfigSpec spec, int y, boolean editable) {
        int w = Math.min(310, width - 30);
        Button button = addRenderableWidget(Button.builder(Component.literal(label), b -> minecraft.setScreen(
                new Category(this, new Session(spec, () -> {}), List.of(), List.of(name))))
                .bounds((width - w) / 2, y + 20, w, 20).build());
        button.active = spec.isLoaded() && editable;
        button.setTooltip(Tooltip.create(Component.literal(button.active ? "文件：\"" + file + "\""
                : "进入自己的单人世界后可修改服务端设置。远程服务器由服主管理。")));
    }
    @Override public void render(GuiGraphics g, int x, int y, float delta) {
        background(g, delta);
        int top = sectionTop(), left = (width - Math.min(310, width - 30)) / 2;
        String[] labels = {"通用选项", "客户端选项", "服务端选项"};
        for (int i = 0; i < labels.length; i++) g.drawString(font, Component.literal(labels[i])
                .withStyle(net.minecraft.ChatFormatting.UNDERLINE), left, top + i * sectionStep(), 0xffffff);
        super.render(g, x, y, delta); breadcrumbs(g, List.of(title.getString()));
    }
    @Override public void onClose() { minecraft.setScreen(parent); }

    private static final class Unavailable extends ConfigFrame {
        private final Screen parent;
        Unavailable(Screen parent, Component title) { super(title); this.parent = parent; }
        @Override protected void init() {
            var b = addRenderableWidget(Button.builder(Component.literal("服务端设置…"), ignored -> {})
                    .bounds(width / 2 - 150, height / 2 - 10, 300, 20).build());
            b.active = false;
            b.setTooltip(Tooltip.create(Component.literal("进入自己的单人世界后即可修改；远程服务器由服主管理。")));
            addRenderableWidget(Button.builder(Component.literal("完成"), ignored -> onClose()).bounds(width / 2 - 100, height - 22, 200, 20).build());
        }
        @Override public void render(GuiGraphics g, int x, int y, float delta) {
            background(g, delta); super.render(g, x, y, delta); breadcrumbs(g, List.of(title.getString()));
        }
        @Override public void onClose() { minecraft.setScreen(parent); }
    }

    private static final class Session {
        final ForgeConfigSpec spec;
        final Runnable afterSave;
        final Map<ForgeConfigSpec.ConfigValue<?>, Object> initial = new LinkedHashMap<>(), pending = new LinkedHashMap<>();
        final Map<ForgeConfigSpec.ConfigValue<?>, String> invalid = new HashMap<>(), inputs = new HashMap<>();
        Session(ForgeConfigSpec spec, Runnable afterSave) { this.spec = spec; this.afterSave = afterSave; collect(spec.getValues()); }
        private void collect(UnmodifiableConfig config) {
            for (Object entry : config.valueMap().values()) {
                if (entry instanceof ForgeConfigSpec.ConfigValue<?> value) {
                    initial.put(value, copy(value.get())); pending.put(value, copy(value.get()));
                } else if (entry instanceof UnmodifiableConfig child) collect(child);
            }
        }
        static Object copy(Object value) { return value instanceof List<?> list ? new ArrayList<>(list) : value; }
        boolean in(ForgeConfigSpec.ConfigValue<?> value, List<String> path) {
            return value.getPath().size() >= path.size() && value.getPath().subList(0, path.size()).equals(path);
        }
        Map<ForgeConfigSpec.ConfigValue<?>, Object> snapshot(List<String> path) {
            Map<ForgeConfigSpec.ConfigValue<?>, Object> result = new LinkedHashMap<>();
            pending.forEach((v, object) -> { if (in(v, path)) result.put(v, copy(object)); }); return result;
        }
        void restore(Map<ForgeConfigSpec.ConfigValue<?>, Object> snapshot) {
            snapshot.forEach((v, object) -> { pending.put(v, copy(object)); invalid.remove(v); inputs.remove(v); });
        }
        void reset(List<String> path) {
            pending.keySet().forEach(v -> {
                if (in(v, path)) { pending.put(v, copy(definition(v).getDefault())); invalid.remove(v); inputs.remove(v); }
            });
        }
        ForgeConfigSpec.ValueSpec definition(ForgeConfigSpec.ConfigValue<?> value) { return spec.getSpec().get(value.getPath()); }
        boolean valid(List<String> path) { return invalid.keySet().stream().noneMatch(v -> in(v, path)); }
        boolean changed(Map<ForgeConfigSpec.ConfigValue<?>, Object> snapshot) {
            return snapshot.entrySet().stream().anyMatch(e -> !Objects.equals(e.getValue(), pending.get(e.getKey())) || invalid.containsKey(e.getKey()));
        }
        boolean nonDefault(List<String> path) {
            return pending.entrySet().stream().anyMatch(e -> in(e.getKey(), path)
                    && (!Objects.equals(e.getValue(), definition(e.getKey()).getDefault()) || invalid.containsKey(e.getKey())));
        }
        void number(ForgeConfigSpec.ConfigValue<?> value, String text) {
            inputs.put(value, text);
            try {
                Object original = initial.get(value), parsed;
                if (original instanceof Integer) parsed = Integer.valueOf(text);
                else if (original instanceof Long) parsed = Long.valueOf(text);
                else if (original instanceof Float) {
                    float f = Float.parseFloat(text); if (!Float.isFinite(f)) throw new IllegalArgumentException(); parsed = f;
                } else {
                    double d = Double.parseDouble(text); if (!Double.isFinite(d)) throw new IllegalArgumentException(); parsed = d;
                }
                if (!definition(value).test(parsed)) throw new IllegalArgumentException();
                pending.put(value, parsed); invalid.remove(value);
            } catch (RuntimeException exception) { invalid.put(value, "数值无效或超出允许范围"); }
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        void save() {
            if (!valid(List.of())) throw new IllegalArgumentException("请先修正标红的配置项");
            if (!changed(initial)) return;
            Map<ForgeConfigSpec.ConfigValue<?>, Object> previous = new LinkedHashMap<>();
            pending.forEach((v, object) -> {
                if (!definition(v).test(object)) throw new IllegalArgumentException("配置值无效");
                previous.put(v, copy(v.get()));
            });
            try {
                pending.forEach((v, object) -> ((ForgeConfigSpec.ConfigValue)v).set(copy(object))); spec.save();
            } catch (RuntimeException exception) {
                previous.forEach((v, object) -> ((ForgeConfigSpec.ConfigValue)v).set(object)); throw exception;
            }
            afterSave.run();
        }
        Component name(List<String> path, ForgeConfigSpec.ConfigValue<?> value) {
            String key = value == null ? spec.getLevelTranslationKey(path) : definition(value).getTranslationKey();
            if (key != null && I18n.exists(key)) return Component.translatable(key);
            String leaf = path.get(path.size() - 1);
            for (String namespace : List.of("useless_mod", "useless_stretcher")) {
                String fallback = namespace + ".configuration." + leaf;
                if (I18n.exists(fallback)) return Component.translatable(fallback);
            }
            String comment = value == null ? spec.getLevelComment(path) : definition(value).getComment();
            if (comment != null && !comment.isBlank()) {
                String line = comment.split("\\R", 2)[0];
                if (line.chars().anyMatch(c -> c >= 0x4e00 && c <= 0x9fff)) return Component.literal(line.split(" - |：|: ", 2)[0]);
            }
            return Component.literal(leaf.replace('_', ' '));
        }
        Component help(List<String> path, ForgeConfigSpec.ConfigValue<?> value) {
            Component label = name(path, value);
            String key = value == null ? spec.getLevelTranslationKey(path) : definition(value).getTranslationKey();
            String comment = value == null ? spec.getLevelComment(path) : definition(value).getComment();
            if (key != null) {
                if (I18n.exists(key + ".tooltip")) comment = I18n.get(key + ".tooltip");
                else if (I18n.exists(key + ".comment")) comment = I18n.get(key + ".comment");
            }
            var result = label.copy().withStyle(net.minecraft.ChatFormatting.BOLD);
            if (comment != null && !comment.isBlank()) result.append(Component.literal("\n\n" + comment).withStyle(net.minecraft.ChatFormatting.RESET));
            if (value != null && definition(value).needsWorldRestart()) result.append(Component.literal("\n重新进入世界后生效").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return result;
        }
    }

    private static final class Category extends ConfigFrame {
        private final Screen parent;
        private final Session session;
        private final List<String> path, crumbs;
        private final Map<ForgeConfigSpec.ConfigValue<?>, Object> entered;
        private Rows rows;
        private Button cancel, reset, done;
        private double scroll;
        private boolean refreshPending;
        private String error = "";
        Category(Screen parent, Session session, List<String> path, List<String> crumbs) {
            super(Component.literal(crumbs.get(crumbs.size() - 1)));
            this.parent = parent; this.session = session; this.path = List.copyOf(path); this.crumbs = List.copyOf(crumbs);
            entered = session.snapshot(path);
        }
        @Override protected void init() {
            rows = new Rows();
            UnmodifiableConfig config = path.isEmpty() ? session.spec.getValues() : session.spec.getValues().get(path);
            config.valueMap().forEach((key, object) -> {
                List<String> full = new ArrayList<>(path); full.add(key);
                if (object instanceof UnmodifiableConfig || object instanceof ForgeConfigSpec.ConfigValue<?>) rows.append(new Row(full, object));
            });
            rows.setScrollAmount(scroll); addRenderableWidget(rows);
            if (rows.children().stream().allMatch(row -> row.value == null)) {
                cancel = null; reset = null;
                done = addRenderableWidget(Button.builder(Component.literal("完成"), b -> finish())
                        .bounds(width / 2 - 100, height - 22, 200, 20).build());
                updateButtons();
                return;
            }
            int w = Math.min(120, (width - 38) / 3), gap = 8, start = (width - (3 * w + 2 * gap)) / 2;
            cancel = addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(start, height - 22, w, 20).build());
            reset = addRenderableWidget(Button.builder(Component.literal("重置"), b -> { session.reset(path); refreshPending = true; })
                    .bounds(start + w + gap, height - 22, w, 20).build());
            done = addRenderableWidget(Button.builder(Component.literal("完成"), b -> finish()).bounds(start + 2 * (w + gap), height - 22, w, 20).build());
            updateButtons();
        }
        private void updateButtons() {
            if (cancel != null) cancel.active = session.changed(entered);
            if (reset != null) reset.active = session.nonDefault(path);
            done.active = session.valid(path);
        }
        private void finish() {
            if (!session.valid(path)) return;
            try { if (path.isEmpty()) session.save(); minecraft.setScreen(parent); }
            catch (RuntimeException failure) { error = "保存失败，请检查配置文件权限或数值。"; }
        }
        @Override public void onClose() { session.restore(entered); minecraft.setScreen(parent); }
        @Override public void removed() { if (rows != null) scroll = rows.getScrollAmount(); super.removed(); }
        @Override public void tick() {
            if (refreshPending) { refreshPending = false; scroll = rows.getScrollAmount(); rebuildWidgets(); }
            rows.children().forEach(Row::tick); updateButtons();
        }
        @Override public void render(GuiGraphics g, int x, int y, float delta) {
            background(g, delta); super.render(g, x, y, delta); breadcrumbs(g, crumbs);
            if (!error.isEmpty()) g.drawCenteredString(font, error, width / 2, height - 37, 0xff6666);
        }
        private final class Rows extends ContainerObjectSelectionList<Row> {
            Rows() {
                super(Category.this.minecraft, Category.this.width, Category.this.height, 33, Category.this.height - 30, 25);
                centerListVertically = false;
                setRenderBackground(false); setRenderTopAndBottom(false); setRenderSelection(false);
            }
            void append(Row row) { addEntry(row); }
            @Override public int getRowWidth() { return Math.min(310, Category.this.width - 48); }
            @Override protected int getScrollbarPosition() { return (Category.this.width + getRowWidth()) / 2 + 10; }
        }
        private final class Row extends ContainerObjectSelectionList.Entry<Row> {
            final List<String> full;
            final ForgeConfigSpec.ConfigValue<?> value;
            final Component label, help;
            final AbstractWidget control;
            Row(List<String> full, Object object) {
                this.full = List.copyOf(full); value = object instanceof ForgeConfigSpec.ConfigValue<?> v ? v : null;
                label = session.name(full, value); help = session.help(full, value);
                Object staged = value == null ? null : session.pending.get(value);
                if (value == null) {
                    control = Button.builder(Component.literal("编辑…"), b -> {
                        List<String> next = new ArrayList<>(crumbs); next.add(label.getString());
                        minecraft.setScreen(new Category(Category.this, session, full, next));
                    }).bounds(0, 0, 150, 20).build();
                } else if (staged instanceof Boolean) {
                    control = Button.builder(booleanName((Boolean)staged), b -> {
                        boolean enabled = !(Boolean)session.pending.get(value); session.pending.put(value, enabled); b.setMessage(booleanName(enabled));
                    }).bounds(0, 0, 150, 20).build();
                } else if (staged instanceof Number) {
                    EditBox input = new EditBox(font, 0, 0, 150, 20, label);
                    input.setMaxLength(64); input.setValue(session.inputs.getOrDefault(value, staged.toString()));
                    input.setResponder(text -> session.number(value, text)); control = input;
                } else if (staged instanceof Enum<?>) {
                    control = Button.builder(Component.literal(staged.toString()), b -> {
                        Enum<?> previous = (Enum<?>)session.pending.get(value); Object[] values = previous.getDeclaringClass().getEnumConstants();
                        for (int i = 1; i <= values.length; i++) {
                            Object next = values[(previous.ordinal() + i) % values.length];
                            if (session.definition(value).test(next)) { session.pending.put(value, next); b.setMessage(Component.literal(next.toString())); break; }
                        }
                    }).bounds(0, 0, 150, 20).build();
                } else {
                    control = Button.builder(Component.literal("编辑…"), b -> {
                        List<String> next = new ArrayList<>(crumbs); next.add(label.getString());
                        minecraft.setScreen(new TextEditor(Category.this, session, value, next));
                    }).bounds(0, 0, 150, 20).build();
                }
                control.setTooltip(Tooltip.create(help));
            }
            private Component booleanName(boolean enabled) { return Component.translatable(enabled ? "options.on" : "options.off"); }
            void tick() { if (control instanceof EditBox box) box.tick(); }
            @Override public List<? extends GuiEventListener> children() { return List.of(control); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(control); }
            @Override public void render(GuiGraphics g, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                int half = rowWidth / 2;
                String text = label.getString() + (value == null ? "…" : ""), display = font.plainSubstrByWidth(text, half - 12);
                if (!display.equals(text)) display = font.plainSubstrByWidth(text, half - 22) + "…";
                g.drawString(font, display, x, y + 7, 0xffffff, true);
                control.setX(x + half + 6); control.setY(y + 1); control.setWidth(half - 6);
                if (control instanceof EditBox box) box.setTextColor(session.invalid.containsKey(value) ? 0xff5555 : 0xffffff);
                control.render(g, mouseX, mouseY, delta);
                if (mouseX >= x && mouseX < x + half && mouseY >= y && mouseY < y + rowHeight && mouseY >= rows.getTop() && mouseY < rows.getBottom())
                    Category.this.setTooltipForNextRenderPass(font.split(help, Math.min(300, width - 24)));
            }
        }
    }

    private static final class TextEditor extends ConfigFrame {
        private final Screen parent;
        private final Session session;
        private final ForgeConfigSpec.ConfigValue<?> value;
        private final List<String> crumbs;
        private final boolean list;
        private MultiLineEditBox input;
        private String text, error = "";
        TextEditor(Screen parent, Session session, ForgeConfigSpec.ConfigValue<?> value, List<String> crumbs) {
            super(session.name(value.getPath(), value)); this.parent = parent; this.session = session; this.value = value; this.crumbs = List.copyOf(crumbs);
            Object object = session.pending.get(value); list = object instanceof List<?>; text = format(object);
        }
        private String format(Object object) { return object instanceof List<?> values ? String.join("\n", values.stream().map(String::valueOf).toList()) : String.valueOf(object); }
        @Override protected void init() {
            int w = Math.min(340, width - 30);
            input = new MultiLineEditBox(font, (width - w) / 2, 67, w, Math.max(28, height - 115), Component.literal(list ? "每行一项" : "输入内容"), title);
            input.setCharacterLimit(32767); input.setValue(text); input.setValueListener(s -> text = s); addRenderableWidget(input); setInitialFocus(input);
            int bw = Math.min(120, (width - 38) / 3), gap = 8, left = (width - 3 * bw - 2 * gap) / 2;
            addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(left, height - 22, bw, 20).build());
            addRenderableWidget(Button.builder(Component.literal("重置"), b -> input.setValue(format(session.definition(value).getDefault())))
                    .bounds(left + bw + gap, height - 22, bw, 20).build());
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> finish()).bounds(left + 2 * (bw + gap), height - 22, bw, 20).build());
        }
        private void finish() {
            Object parsed = list ? (text.isEmpty() ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(text.split("\\R")))) : text;
            if (!session.definition(value).test(parsed)) { error = "内容不符合此配置项的要求，请检查后重试。"; return; }
            session.pending.put(value, parsed); session.invalid.remove(value); minecraft.setScreen(parent);
        }
        @Override public void tick() { input.tick(); }
        @Override public void render(GuiGraphics g, int x, int y, float delta) {
            background(g, delta); super.render(g, x, y, delta); breadcrumbs(g, crumbs);
            g.drawCenteredString(font, list ? "每行一项，完成后返回配置列表" : title.getString(), width / 2, 46, 0xffffff);
            if (!error.isEmpty()) g.drawCenteredString(font, error, width / 2, height - 40, 0xff6666);
        }
        @Override public void onClose() { minecraft.setScreen(parent); }
    }
}

/** Shared frame with an isolated blur chain; unavailable post-processing falls back to shading. */
abstract class ConfigFrame extends Screen {
    private static final PanoramaRenderer PANORAMA = new PanoramaRenderer(new CubeMap(new ResourceLocation("textures/gui/title/background/panorama")));
    private PostChain blur;
    private boolean blurUnavailable;
    private int blurWidth, blurHeight;
    protected ConfigFrame(Component title) { super(title); }
    protected void background(GuiGraphics g, float delta) {
        g.flush();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            if (minecraft.level == null) PANORAMA.render(delta, 1.0F);
            if (!blurUnavailable) {
                if (blur == null) blur = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), new ResourceLocation("shaders/post/blur.json"));
                int w = minecraft.getWindow().getWidth(), h = minecraft.getWindow().getHeight();
                if (w != blurWidth || h != blurHeight) { blur.resize(w, h); blurWidth = w; blurHeight = h; }
                blur.process(delta);
            }
        } catch (Exception exception) {
            blurUnavailable = true;
            if (blur != null) { blur.close(); blur = null; }
        } finally {
            // PostPass writes a full-screen quad into the main target's depth attachment.
            // GUI render types enable depth testing themselves, so merely disabling it here
            // still leaves every subsequent widget behind that quad.
            minecraft.getMainRenderTarget().bindWrite(true);
            RenderSystem.depthMask(true);
            RenderSystem.clear(256, Minecraft.ON_OSX);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(515);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        g.fill(0, 0, width, height, 0x66000000);
        g.fill(0, 0, width, 32, 0x55000000); g.fill(0, height - 28, width, height, 0x55000000);
        g.fill(0, 31, width, 32, 0x88777777); g.fill(0, height - 29, width, height - 28, 0x88777777);
    }
    protected void breadcrumbs(GuiGraphics g, List<String> path) {
        List<String> parts = new ArrayList<>(path);
        while (parts.size() > 1 && font.width(String.join(" > ", parts)) > width - 20) {
            if (parts.get(0).equals("…")) parts.remove(1); else { parts.remove(0); parts.add(0, "…"); }
            if (parts.size() == 2 && parts.get(0).equals("…")) break;
        }
        String full = String.join(" > ", parts);
        if (font.width(full) > width - 20) { g.drawCenteredString(font, font.plainSubstrByWidth(full, width - 20), width / 2, 12, 0xffffff); return; }
        int x = (width - font.width(full)) / 2;
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) { g.drawString(font, " > ", x, 12, 0xffcc33, true); x += font.width(" > "); }
            g.drawString(font, parts.get(i), x, 12, 0xffffff, true); x += font.width(parts.get(i));
        }
    }
    @Override public void removed() {
        if (blur != null) { blur.close(); blur = null; }
        blurWidth = 0; blurHeight = 0;
    }
    @Override public boolean isPauseScreen() { return true; }
}
