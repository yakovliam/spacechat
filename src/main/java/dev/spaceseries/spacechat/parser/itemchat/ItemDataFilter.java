/*
 * This file is part of LightItems, licensed under the MIT License
 *
 * Copyright (c) 2025 Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.spaceseries.spacechat.parser.itemchat;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;
import com.saicone.rtag.item.ItemObject;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ItemDataFilter {

    private static final String COMPONENTS = "components";
    private static final String CUSTOM_DATA = "minecraft:custom_data";
    private static final String CONTAINER = "minecraft:container";

    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;

    @NotNull
    public static ItemStack filterItemComponents(@NotNull ItemStack item) {
        return RtagItem.edit(item, tag -> {
            if (tag.hasComponent(CUSTOM_DATA)) {
                tag.removeComponent(CUSTOM_DATA);
            }
            if (tag.hasComponent(CONTAINER)) {
                ComponentType.encodeNbt(CONTAINER, tag.getComponent(CONTAINER)).ifPresent(container -> {
                    tag.setComponent(CONTAINER, filterItemComponents(TagList.getValue(container)));
                });
            }
            return tag.loadCopy();
        });
    }

    @NotNull
    public static Object filterItemComponents(@NotNull Object compound) {
        final Map<String, Object> map = TagCompound.getValue(compound);
        if (!map.containsKey(COMPONENTS)) {
            return compound;
        }

        final Map<String, Object> item = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equals(COMPONENTS)) {
                final Map<String, Object> components = new HashMap<>();
                filterItemComponents(TagCompound.getValue(entry.getValue()), components::put);
                item.put(entry.getKey(), TagCompound.newUncheckedTag(components));
            } else {
                item.put(entry.getKey(), entry.getValue());
            }
        }

        return TagCompound.newUncheckedTag(item);
    }

    @NotNull
    public static Object filterItemComponents(@NotNull List<Object> container) {
        final List<Object> list = new ArrayList<>();
        for (Object item : container) {
            list.add(filterItemComponents(item));
        }
        return TagList.newUncheckedTag(list);
    }

    public static void filterItemComponents(@NotNull Map<String, Object> compound, @NotNull BiConsumer<String, Object> consumer) {
        final Object components = compound.get(COMPONENTS);
        if (components != null) {
            for (Map.Entry<String, Object> entry : TagCompound.getValue(components).entrySet()) {
                final String key = entry.getKey();
                if (key.equals(CUSTOM_DATA)) {
                    continue;
                }
                Object value = entry.getValue();
                if (key.equals(CONTAINER)) {
                    value = filterItemComponents(TagList.getValue(value));
                }
                consumer.accept(key, value);
            }
        }
    }

    @NotNull
    public static ItemStack filterItemTag(@NotNull ItemStack item, @NotNull DataPath comparator) {
        Object tag = ItemObject.getCustomDataTag(ItemObject.asNMSCopy(item));
        if (tag == null) {
            return item;
        }
        if (!filter(tag, comparator)) {
            return new ItemStack(item.getType(), item.getAmount());
        }
        final Object result = ItemObject.asNMSCopy(new ItemStack(item.getType(), item.getAmount()));
        ItemObject.setCustomDataTag(result, tag);
        return ItemObject.asCraftMirror(result);
    }

    @NotNull
    public static Object filterItemTag(@NotNull Object compound, @NotNull DataPath comparator) {
        final Object tag = TagCompound.get(compound, "tag");
        if (tag != null) {
            filter(tag, comparator);
        }
        return compound;
    }

    public static boolean filter(@NotNull Object tag, @NotNull DataPath comparator) {
        if (comparator == DataPath.EMPTY) {
            return true;
        } else if (comparator == DataPath.RESET) {
            if (TagBase.getTypeId(tag) == TAG_LIST && TagList.getType(tag) == TAG_COMPOUND) {
                for (Object compound : TagList.getValue(tag)) {
                    filterItemTag(compound, comparator);
                }
            }
        } else if (TagBase.getTypeId(tag) == TAG_COMPOUND) {
            final Map<String, Object> map = TagCompound.getValue(tag);
            map.entrySet().removeIf(entry -> {
                final DataPath children = comparator.get(entry.getKey());
                if (children == null) {
                    return true;
                }
                return !filter(entry.getValue(), children);
            });
            return !map.isEmpty();
        }
        return true;
    }
}
