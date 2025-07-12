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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataPath {

    public static final DataPath EMPTY = new DataPath() {
        @Override
        public void put(@NotNull String key, @NotNull DataPath children) {
            throw new IllegalStateException();
        }
    };
    public static final DataPath RESET = new DataPath() {
        @Override
        public void put(@NotNull String key, @NotNull DataPath children) {
            throw new IllegalStateException();
        }
    };

    @NotNull
    public static DataPath valueOf(@NotNull List<String> paths) {
        final DataPath root = new DataPath();
        for (String path : paths) {
            final String[] keys = path.split("\\.");
            DataPath parent = root;
            for (int i = 0; i < keys.length; i++) {
                final String key = keys[i];
                if (i + 1 == keys.length) { // Last key
                    parent.put(key, EMPTY);
                } else if (i + 2 == keys.length && keys[i + 1].equals("[]")) { // Reset key
                    parent.put(key, RESET);
                    break;
                } else {
                    final DataPath children = new DataPath();
                    parent.put(key, children);
                    parent = children;
                }
            }
        }
        return root;
    }

    private final Map<String, DataPath> children = new LinkedHashMap<>();

    public void put(@NotNull String key, @NotNull DataPath children) {
        this.children.put(key, children);
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Nullable
    public DataPath get(@NotNull String key) {
        return children.get(key);
    }
}
