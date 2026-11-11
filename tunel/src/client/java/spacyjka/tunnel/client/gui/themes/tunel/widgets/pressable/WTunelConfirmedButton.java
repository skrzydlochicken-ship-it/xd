/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.themes.tunel.widgets.pressable;

import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.renderer.packer.GuiTexture;
import spacyjka.tunnel.client.gui.themes.tunel.TunelGuiTheme;
import spacyjka.tunnel.client.gui.themes.tunel.TunelWidget;
import spacyjka.tunnel.client.gui.widgets.pressable.WButton;
import spacyjka.tunnel.client.gui.widgets.pressable.WConfirmedButton;
import spacyjka.tunnel.client.utils.render.color.Color;

public class WTunelConfirmedButton extends WConfirmedButton implements TunelWidget {
    public WTunelConfirmedButton(String text, String confirmText, GuiTexture texture) {
        super(text, confirmText, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        TunelGuiTheme theme = theme();
        double pad = pad();

        Color outline = theme.outlineColor.get(pressed, mouseOver);
        Color fg = pressedOnce ? theme.backgroundColor.get(pressed, mouseOver) : theme.textColor.get();
        Color bg = pressedOnce ? theme.textColor.get() : theme.backgroundColor.get(pressed, mouseOver);

        renderBackground(renderer, this, outline, bg);

        String text = getText();

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, fg, false);
        }
        else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, fg);
        }
    }
}
