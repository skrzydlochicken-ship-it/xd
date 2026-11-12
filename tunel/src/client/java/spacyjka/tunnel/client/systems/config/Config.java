/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.systems.config;

import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.renderer.Fonts;
import spacyjka.tunnel.client.renderer.text.FontFace;
import spacyjka.tunnel.client.settings.*;
import spacyjka.tunnel.client.systems.System;
import spacyjka.tunnel.client.systems.Systems;
import spacyjka.tunnel.client.systems.modules.Module;
import spacyjka.tunnel.client.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

import static spacyjka.tunnel.client.TunelClient.mc;

public class Config extends System<Config> {
    public final Settings settings = new Settings();

    private final SettingGroup sgVisual = settings.createGroup("Visual");
    private final SettingGroup sgModules = settings.createGroup("Modules");
    private final SettingGroup sgChat = settings.createGroup("Chat");

    // Visual

    public final Setting<Boolean> customFont = sgVisual.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Use a custom font.")
        .defaultValue(true)
        .build()
    );

    public final Setting<FontFace> font = sgVisual.add(new FontFaceSetting.Builder()
        .name("font")
        .description("Custom font to use.")
        .visible(customFont::get)
        .onChanged(Fonts::load)
        .build()
    );

    public final Setting<Double> rainbowSpeed = sgVisual.add(new DoubleSetting.Builder()
        .name("rainbow-speed")
        .description("The global rainbow speed.")
        .defaultValue(0.5)
        .range(0, 10)
        .sliderMax(5)
        .build()
    );

    // Modules

    public final Setting<Integer> moduleSearchCount = sgModules.add(new IntSetting.Builder()
        .name("module-search-count")
        .description("Amount of modules and settings to be shown in the module search bar.")
        .defaultValue(8)
        .min(1).sliderMax(12)
        .build()
    );

    public final Setting<Boolean> moduleAliases = sgModules.add(new BoolSetting.Builder()
        .name("search-module-aliases")
        .description("Whether or not module aliases will be used in the module search bar.")
        .defaultValue(true)
        .build()
    );

    // Chat

    public final Setting<Boolean> chatFeedback = sgChat.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Sends chat feedback when the mod performs certain actions.")
        .defaultValue(true)
        .build()
    );

    // Misc

    public List<String> dontShowAgainPrompts = new ArrayList<>();

    public Config() {
        super("config");
    }

    public static Config get() {
        return Systems.get(Config.class);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.put("settings", settings.toTag());
        tag.put("dontShowAgainPrompts", listToTag(dontShowAgainPrompts));

        return tag;
    }

    @Override
    public Config fromTag(CompoundTag tag) {
        if (tag.contains("settings")) settings.fromTag(tag.getCompoundOrEmpty("settings"));
        if (tag.contains("dontShowAgainPrompts")) dontShowAgainPrompts = listFromTag(tag, "dontShowAgainPrompts");

        return this;
    }

    private ListTag listToTag(List<String> list) {
        ListTag nbt = new ListTag();
        for (String item : list) nbt.add(StringTag.valueOf(item));
        return nbt;
    }

    private List<String> listFromTag(CompoundTag tag, String key) {
        List<String> list = new ArrayList<>();
        for (Tag item : tag.getListOrEmpty(key)) list.add(item.asString().orElse(""));
        return list;
    }

}
