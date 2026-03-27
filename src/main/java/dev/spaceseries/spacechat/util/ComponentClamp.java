package dev.spaceseries.spacechat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ComponentClamp {

    @NotNull
    public static Component clamp(@NotNull Component component, int maxLength) {
        if (maxLength < 1) {
            return component;
        }
        return new ComponentClamp(maxLength).run(component);
    }

    private final int maxLength;
    private transient int count;

    public ComponentClamp(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @NotNull
    public Component run(@NotNull Component component) {
        if (this.count >= this.maxLength) {
            return Component.empty();
        }

        if (component instanceof TextComponent) {
            String content = ((TextComponent) component).content();
            int remaining = this.maxLength - this.count;

            if (content.length() > remaining) {
                content = content.substring(0, remaining) + "...";
            }

            this.count += content.length();

            return Component.text(content, component.style());
        }

        if (component.children().isEmpty()) {
            return component;
        }

        final List<Component> children = component.children();
        Component result = component.children(List.of());

        for (Component child : children) {
            if (this.count >= this.maxLength) break;
            result = result.append(run(child));
        }

        return result;
    }
}
