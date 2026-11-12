/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.gui.tabs;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import spacyjka.tunnel.client.gui.tabs.builtin.*;

import java.util.ArrayList;
import java.util.List;

public class Tabs {
    private static final List<Tab> tabs = new ArrayList<>();
    private static final Reference2ReferenceOpenHashMap<Class<? extends Tab>, Tab> tabInstances = new Reference2ReferenceOpenHashMap<>();

    private Tabs() {
    }

    public static void init() {
        add(new ModulesTab());
        add(new ConfigTab());
        add(new GuiTab());
    }

    public static void add(Tab tab) {
        tabs.add(tab);
        tabInstances.put(tab.getClass(), tab);
    }

    public static List<Tab> get() {
        return tabs;
    }

    public static Tab get(Class<? extends Tab> klass) {
        return tabInstances.get(klass);
    }
}
