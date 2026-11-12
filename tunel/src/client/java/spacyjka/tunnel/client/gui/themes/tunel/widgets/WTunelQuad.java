/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.themes.tunel.widgets;

import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.widgets.WQuad;
import spacyjka.tunnel.client.utils.render.color.Color;

public class WTunelQuad extends WQuad {
    public WTunelQuad(Color color) {
        super(color);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(x, y, width, height, color);
    }
}
