/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.screens.settings.*;
import spacyjka.tunnel.client.gui.themes.tunel.widgets.WTunelLabel;
import spacyjka.tunnel.client.gui.utils.Cell;
import spacyjka.tunnel.client.gui.utils.CharFilter;
import spacyjka.tunnel.client.gui.utils.SettingsWidgetFactory;
import spacyjka.tunnel.client.gui.widgets.*;
import spacyjka.tunnel.client.gui.widgets.containers.*;
import spacyjka.tunnel.client.gui.widgets.input.*;
import spacyjka.tunnel.client.gui.widgets.pressable.WButton;
import spacyjka.tunnel.client.gui.widgets.pressable.WCheckbox;
import spacyjka.tunnel.client.gui.widgets.pressable.WMinus;
import spacyjka.tunnel.client.gui.widgets.pressable.WPlus;
import spacyjka.tunnel.client.renderer.Fonts;
import spacyjka.tunnel.client.settings.*;
import spacyjka.tunnel.client.utils.Utils;
import spacyjka.tunnel.client.utils.render.color.SettingColor;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static spacyjka.tunnel.client.TunelClient.mc;

public class DefaultSettingsWidgetFactory extends SettingsWidgetFactory {
    private static final SettingColor WHITE = new SettingColor();

    public DefaultSettingsWidgetFactory(GuiTheme theme) {
        super(theme);

        factories.put(BoolSetting.class, (table, setting) -> boolW(table, (BoolSetting) setting));
        factories.put(IntSetting.class, (table, setting) -> intW(table, (IntSetting) setting));
        factories.put(DoubleSetting.class, (table, setting) -> doubleW(table, (DoubleSetting) setting));
        factories.put(StringSetting.class, (table, setting) -> stringW(table, (StringSetting) setting));
        factories.put(EnumSetting.class, (table, setting) -> enumW(table, (EnumSetting<? extends Enum<?>>) setting));
        factories.put(ProvidedStringSetting.class, (table, setting) -> providedStringW(table, (ProvidedStringSetting) setting));
        factories.put(ColorSetting.class, (table, setting) -> colorW(table, (ColorSetting) setting));
        factories.put(KeybindSetting.class, (table, setting) -> keybindW(table, (KeybindSetting) setting));
        factories.put(FontFaceSetting.class, (table, setting) -> fontW(table, (FontFaceSetting) setting));
    }

    @Override
    public WWidget create(GuiTheme theme, Settings settings, String filter) {
        WVerticalList list = theme.verticalList();

        List<RemoveInfo> removeInfoList = new ArrayList<>();

        // Add all settings
        for (SettingGroup group : settings.groups) {
            group(list, group, filter, removeInfoList);
        }

        // Calculate width and set it as minimum width
        list.calculateSize();
        list.minWidth = list.width;

        // Remove hidden settings
        for (RemoveInfo removeInfo : removeInfoList) {
            removeInfo.remove(list);
        }

        return list;
    }

    // If a different theme uses has different heights of widgets this can method can be overwritten to account for it in the setting titles
    protected double settingTitleTopMargin() {
        return 6;
    }

    private void group(WVerticalList list, SettingGroup group, String filter, List<RemoveInfo> removeInfoList) {
        WSection section = list.add(theme.section(group.name, group.sectionExpanded)).expandX().widget();
        section.action = () -> group.sectionExpanded = section.isExpanded();

        WTable table = section.add(theme.table()).expandX().widget();

        RemoveInfo removeInfo = null;

        for (Setting<?> setting : group) {
            if (!Strings.CI.contains(setting.title, filter)) continue;

            boolean visible = setting.isVisible();
            setting.lastWasVisible = visible;
            if (!visible) {
                if (removeInfo == null) removeInfo = new RemoveInfo(section, table);
                removeInfo.markRowForRemoval();
            }

            table.add(theme.label(setting.title)).top().marginTop(settingTitleTopMargin()).widget().tooltip = setting.description;

            Factory factory = getFactory(setting.getClass());
            if (factory != null) factory.create(table, setting);

            table.row();
        }

        if (removeInfo != null) removeInfoList.add(removeInfo);
    }

    private static class RemoveInfo {
        private final WSection section;
        private final WTable table;
        private final IntList rowIds = new IntArrayList();

        public RemoveInfo(WSection section, WTable table) {
            this.section = section;
            this.table = table;
        }

        public void markRowForRemoval() {
            rowIds.add(table.rowI());
        }

        public void remove(WVerticalList list) {
            for (int i = 0; i < rowIds.size(); i++) {
                table.removeRow(rowIds.getInt(i) - i);
            }

            if (table.cells.isEmpty()) list.cells.removeIf(cell -> cell.widget() == section);
        }
    }

    // Settings

    private void boolW(WTable table, BoolSetting setting) {
        WCheckbox checkbox = table.add(theme.checkbox(setting.get())).expandCellX().widget();
        checkbox.action = () -> setting.set(checkbox.checked);

        reset(table, setting, () -> checkbox.checked = setting.get());
    }

    private void intW(WTable table, IntSetting setting) {
        WIntEdit edit = table.add(theme.intEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.noSlider)).expandX().widget();

        edit.action = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private void doubleW(WTable table, DoubleSetting setting) {
        WDoubleEdit edit = theme.doubleEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.decimalPlaces, setting.noSlider);
        table.add(edit).expandX();

        Runnable action = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        if (setting.onSliderRelease) edit.actionOnRelease = action;
        else edit.action = action;

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private void stringW(WTable table, StringSetting setting) {
        CharFilter filter = setting.filter == null ? (text, c) -> true : setting.filter;
        Cell<WTextBox> cell = table.add(theme.textBox(setting.get(), setting.placeholder, filter, setting.renderer));
        if (setting.wide) cell.minWidth(Utils.getWindowWidth() - Utils.getWindowWidth() / 4.0);

        WTextBox textBox = cell.expandX().widget();
        textBox.action = () -> setting.set(textBox.get());

        reset(table, setting, () -> textBox.set(setting.get()));
    }

    private <T extends Enum<?>> void enumW(WTable table, EnumSetting<T> setting) {
        WDropdown<T> dropdown = table.add(theme.dropdown(setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(table, setting, () -> dropdown.set(setting.get()));
    }

    private void providedStringW(WTable table, ProvidedStringSetting setting) {
        WDropdown<String> dropdown = table.add(theme.dropdown(setting.supplier.get(), setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(table, setting, () -> dropdown.set(setting.get()));
    }

    private void colorW(WTable table, ColorSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WQuad quad = list.add(theme.quad(setting.get())).widget();

        WButton edit = list.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.setScreen(new ColorSettingScreen(theme, setting));

        reset(table, setting, () -> quad.color = setting.get());
    }

    private void keybindW(WTable table, KeybindSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WKeybind keybind = list.add(theme.keybind(setting.get(), setting.getDefaultValue())).expandX().widget();
        keybind.action = setting::onChanged;
        setting.widget = keybind;

        WButton reset = list.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
        reset.action = keybind::resetBind;
        reset.tooltip = "Reset";
    }

    private void fontW(WTable table, FontFaceSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();
        WLabel label = list.add(theme.label(setting.get().info.family())).widget();

        WButton button = list.add(theme.button("Select")).expandCellX().widget();
        button.action = () -> {
            WidgetScreen screen = new FontFaceSettingScreen(theme, setting);
            screen.onClosed(() -> label.set(setting.get().info.family()));

            mc.setScreen(screen);
        };

        reset(list, setting, () -> label.set(Fonts.DEFAULT_FONT.info.family()));
    }

    // Other

    private void reset(WContainer c, Setting<?> setting, Runnable action) {
        WButton reset = c.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            if (action != null) action.run();
        };
        reset.tooltip = "Reset";
    }

    private static class WSelectedCountLabel extends WTunelLabel {
        private final Setting<?> setting;
        private int lastSize = -1;

        public WSelectedCountLabel(Setting<?> setting) {
            super("", false);

            this.setting = setting;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            int size = getSize(setting);

            if (size != lastSize) {
                set("(" + size + " selected)");
                lastSize = size;
            }

            super.onRender(renderer, mouseX, mouseY, delta);
        }

        public static int getSize(Setting<?> setting) {
            if (setting.get() instanceof Collection<?> collection) return collection.size();
            if (setting.get() instanceof Map<?, ?> map) return map.size();

            return -1;
        }
    }
}
