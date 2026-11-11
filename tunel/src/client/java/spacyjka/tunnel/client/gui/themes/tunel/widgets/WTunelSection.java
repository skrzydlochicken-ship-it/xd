/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.themes.tunel.widgets;

import spacyjka.tunnel.client.gui.renderer.GuiRenderer;
import spacyjka.tunnel.client.gui.themes.tunel.TunelWidget;
import spacyjka.tunnel.client.gui.widgets.WWidget;
import spacyjka.tunnel.client.gui.widgets.containers.WSection;
import spacyjka.tunnel.client.gui.widgets.pressable.WTriangle;

public class WTunelSection extends WSection {
    public WTunelSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WTunelHeader(title);
    }

    protected class WTunelHeader extends WHeader {
        private WTriangle triangle;

        public WTunelHeader(String title) {
            super(title);
        }

        @Override
        public void init() {
            add(theme.horizontalSeparator(title)).expandX();

            if (headerWidget != null) add(headerWidget);

            triangle = new WHeaderTriangle();
            triangle.theme = theme;
            triangle.action = this::onClick;

            add(triangle);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            triangle.rotation = (1 - animProgress) * -90;
        }
    }

    protected static class WHeaderTriangle extends WTriangle implements TunelWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, theme().textColor.get());
        }
    }
}
