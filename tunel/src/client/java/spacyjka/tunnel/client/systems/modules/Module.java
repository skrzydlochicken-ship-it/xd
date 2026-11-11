/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.systems.modules;

import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.gui.GuiTheme;
import spacyjka.tunnel.client.gui.widgets.WWidget;
import spacyjka.tunnel.client.settings.Settings;
import spacyjka.tunnel.client.systems.config.Config;
import spacyjka.tunnel.client.utils.Utils;
import spacyjka.tunnel.client.utils.misc.ISerializable;
import spacyjka.tunnel.client.utils.misc.Keybind;
import spacyjka.tunnel.client.utils.render.color.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class Module implements ISerializable<Module>, Comparable<Module> {
    protected final Minecraft mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final String[] aliases;
    public final Color color;

    public final Settings settings = new Settings();

    private boolean active;

    public boolean serialize = true;
    public boolean runInMainMenu = false;
    public boolean autoSubscribe = true;

    public final Keybind keybind = Keybind.none();
    public boolean toggleOnBindRelease = false;
    public boolean chatFeedback = true;
    public boolean favorite = false;

    public Module(Category category, String name, String description, String... aliases) {
        if (name.contains(" ")) TunelClient.LOG.warn("Module '{}' contains invalid characters in its name making it invalid.", name);

        this.mc = Minecraft.getInstance();
        this.category = category;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.aliases = aliases;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);

    }

    public Module(Category category, String name, String desc) {
        this(category, name, desc, new String[0]);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle() {
        if (!active) {
            active = true;
            Modules.get().addActive(this);

            settings.onActivated();

            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) TunelClient.EVENT_BUS.subscribe(this);
                onActivate();
            }
        }
        else {
            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) TunelClient.EVENT_BUS.unsubscribe(this);
                onDeactivate();
            }

            active = false;
            Modules.get().removeActive(this);
        }
    }

    public void enable() {
        if (!isActive()) toggle();
    }

    public void disable() {
        if (isActive()) toggle();
    }

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            chat(ChatFormatting.GRAY, "Toggled " + ChatFormatting.WHITE + title + ChatFormatting.GRAY + " " + (isActive() ? ChatFormatting.GREEN + "on" : ChatFormatting.RED + "off") + ChatFormatting.GRAY + ".");
        }
    }

    public void info(String message, Object... args) {
        chat(ChatFormatting.GRAY, String.format(message, args));
    }

    public void warning(String message, Object... args) {
        chat(ChatFormatting.YELLOW, String.format(message, args));
    }

    public void error(String message, Object... args) {
        chat(ChatFormatting.RED, String.format(message, args));
    }

    private void chat(ChatFormatting color, String message) {
        if (mc.level == null) return;

        Component text = Component.literal("[" + title + "] ").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal(message).withStyle(color));

        mc.execute(() -> mc.gui.getChat().addMessage(text));
    }

    public boolean isActive() {
        return active;
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public CompoundTag toTag() {
        if (!serialize) return null;
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.put("keybind", keybind.toTag());
        tag.putBoolean("toggleOnKeyRelease", toggleOnBindRelease);
        tag.putBoolean("chatFeedback", chatFeedback);
        tag.putBoolean("favorite", favorite);
        tag.put("settings", settings.toTag());
        tag.putBoolean("active", active);

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        // General
        keybind.fromTag(tag.getCompoundOrEmpty("keybind"));
        toggleOnBindRelease = tag.getBooleanOr("toggleOnKeyRelease", false);
        chatFeedback = !tag.contains("chatFeedback") || tag.getBooleanOr("chatFeedback", false);
        favorite = tag.getBooleanOr("favorite", false);

        // Settings
        Tag settingsTag = tag.get("settings");
        if (settingsTag instanceof CompoundTag) settings.fromTag((CompoundTag) settingsTag);

        boolean active = tag.getBooleanOr("active", false);
        if (active != isActive()) toggle();

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        return Objects.equals(name, module.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@NotNull Module o) {
        return name.compareTo(o.name);
    }
}
