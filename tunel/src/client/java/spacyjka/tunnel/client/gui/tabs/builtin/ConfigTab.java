/*
 * This file is part of the Meteor Client distribution (https://github.com/TunelDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.tabs.builtin;

import spacyjka.tunnel.client.gui.GuiTheme;
import spacyjka.tunnel.client.gui.tabs.Tab;
import spacyjka.tunnel.client.gui.tabs.TabScreen;
import spacyjka.tunnel.client.gui.tabs.WindowTabScreen;
import spacyjka.tunnel.client.settings.Settings;
import spacyjka.tunnel.client.systems.config.Config;
import spacyjka.tunnel.client.utils.misc.NbtUtils;
import net.minecraft.client.gui.screens.Screen;

public class ConfigTab extends Tab {
    public ConfigTab() {
        super("Config");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new ConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigScreen;
    }

    public static class ConfigScreen extends WindowTabScreen {
        private final Settings settings;

        public ConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            settings = Config.get().settings;
            settings.onActivated();

        }

        @Override
        public void initWidgets() {
            add(theme.settings(settings)).expandX();
        }

        @Override
        public void tick() {
            super.tick();

            settings.tick(window, theme);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Config.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Config.get());
        }
    }
}
