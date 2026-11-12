/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.themes.tunel.widgets;

import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.themes.tunel.TunelWidget;
import spacyjka.tunnel.client.gui.widgets.WLabel;

public class WTunelLabel extends WLabel implements TunelWidget {
    public WTunelLabel(String text, boolean title) {
        super(text, title);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!text.isEmpty()) {
            renderer.text(text, x, y, color != null ? color : (title ? theme().titleTextColor.get() : theme().textColor.get()), title);
        }
    }
}
