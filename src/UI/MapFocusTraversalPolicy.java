package UI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom focus traversal policy that allows components to be set
 * and knows about tabbed panes.
 */
class MapFocusTraversalPolicy extends FocusTraversalPolicy {
    private FocusTraversalPolicy defaultPolicy;
    private JTabbedPane tabbedPane;

    private Map<Component, Component> before =
            new HashMap<Component, Component>();
    private Map<String, Component> tabBefore =
            new HashMap<String, Component>();
    private Map<Component, Component> after =
            new HashMap<Component, Component>();

    public MapFocusTraversalPolicy(FocusTraversalPolicy def,
                                   JTabbedPane tab) {
        defaultPolicy = def;
        tabbedPane = tab;
    }

    public void putAfter(Component a, Component b) {
        after.put(a, b);
    }

    public void putBefore(Component a, Component b) {
        before.put(a, b);
    }

    public void putTabBefore(String a, Component b) {
        tabBefore.put(a, b);
    }

    public Component getComponentAfter(Container cont,
                                       Component comp) {
        Component next = after.get(comp);
        if (next != null)
            return next;
        return defaultPolicy.getComponentAfter(cont, comp);
    }

    public Component getComponentBefore(Container cont,
                                        Component comp) {
        if (comp == tabbedPane) {
            String tabTitle =
                    tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            Component prev = tabBefore.get(tabTitle);
            if (prev != null)
                return prev;
        }
        Component prev = before.get(comp);
        if (prev != null)
            return prev;
        return defaultPolicy.getComponentBefore(cont, comp);
    }

    public Component getDefaultComponent(Container root) {
        return defaultPolicy.getDefaultComponent(root);
    }

    public Component getLastComponent(Container root) {
        return defaultPolicy.getLastComponent(root);
    }

    public Component getFirstComponent(Container root) {
        return defaultPolicy.getFirstComponent(root);
    }
}