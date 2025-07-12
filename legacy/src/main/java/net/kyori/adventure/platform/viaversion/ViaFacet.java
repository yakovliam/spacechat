/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
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
package net.kyori.adventure.platform.viaversion;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.viaversion.viaversion.protocols.v1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.data.TranslateRewriter;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.rewriter.ComponentRewriter1_14;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2;
import com.viaversion.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.Facet;
import net.kyori.adventure.platform.facet.FacetBase;
import net.kyori.adventure.platform.facet.Knob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.facet.Knob.logError;

// Non-API
@SuppressWarnings({"checkstyle:FilteringWriteTag", "checkstyle:MissingJavadocType", "checkstyle:MissingJavadocMethod"})
public class ViaFacet<V> extends FacetBase<V> implements Facet.Message<V, String> {
    private static final String PACKAGE = "com.viaversion.viaversion";
    private static final int SUPPORTED_VIA_MAJOR_VERSION = 5;
    private static final boolean SUPPORTED;

    static {
        boolean supported = false;
        try {
            // Check if the ViaVersion API is present and is a supported major version
            Class.forName(PACKAGE + ".api.ViaAPI").getDeclaredMethod("majorVersion");
            supported = Via.getAPI().majorVersion() == SUPPORTED_VIA_MAJOR_VERSION;
        } catch (final Throwable error) {
            // ignore
        }
        SUPPORTED = supported && Knob.isEnabled("viaversion", true);
    }

    private final Function<V, UserConnection> connectionFunction;
    protected final ProtocolVersion minProtocol;
    private final GsonComponentSerializer componentSerializer;

    public ViaFacet(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction, final String minProtocol) {
        super(viewerClass);
        this.connectionFunction = connectionFunction;
        this.minProtocol = ProtocolVersion.getClosest(minProtocol);
        if (this.minProtocol == null) {
            this.componentSerializer = null;
        } else if (this.minProtocol.olderThan(ProtocolVersion.v1_16)) {
            this.componentSerializer = GsonComponentSerializer.colorDownsamplingGson();
        } else {
            int version = protocolToDataVersion(this.minProtocol.getVersion());
            this.componentSerializer = GsonComponentSerializer.builder()
                    .options(JSONOptions.byDataVersion().at(version))
                    .editOptions(builder -> builder.value(JSONOptions.EMIT_HOVER_EVENT_TYPE, JSONOptions.HoverEventValueMode.VALUE_FIELD))
                    .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get())
                    .build();
        }
    }

    private static int protocolToDataVersion(int protocol) {
        if (protocol >= 771) { // 1.21.6
            return 4422;
        } else if (protocol >= 770) { // 1.21.5
            return 4298;
        } else if (protocol >= 769) { // 1.21.4
            return 4174;
        } else if (protocol >= 766) { // 1.20.5
            return 3819;
        } else if (protocol >= 765) { // 1.20.3
            return 3679;
        } else { // 1.16
            return 2526;
        }
    }

    @Override
    public boolean isSupported() {
        return super.isSupported()
                && SUPPORTED
                && this.connectionFunction != null
                && this.minProtocol != null && this.minProtocol.isKnown();
    }

    @Override
    public boolean isApplicable(final @NotNull V viewer) {
        return super.isApplicable(viewer)
                && this.minProtocol.newerThan(Via.getAPI().getServerVersion().lowestSupportedProtocolVersion())
                && this.findProtocol(viewer).newerThanOrEqualTo(this.minProtocol);
    }

    public @Nullable UserConnection findConnection(final @NotNull V viewer) {
        return this.connectionFunction.apply(viewer);
    }

    public ProtocolVersion findProtocol(final @NotNull V viewer) {
        final UserConnection connection = this.findConnection(viewer);
        if (connection != null) {
            return connection.getProtocolInfo().protocolVersion();
        }
        return ProtocolVersion.unknown;
    }

    @NotNull
    @Override
    public String createMessage(final @NotNull V viewer, final @NotNull Component message) {
        return componentSerializer.serialize(message);
    }

    private static class ComponentRewriter {

        private static final Map<ProtocolVersion, BiConsumer<UserConnection, JsonElement>> TEXT_REWRITER = new LinkedHashMap<>();
        private static final Map<ProtocolVersion, BiConsumer<UserConnection, Tag>> TAG_REWRITER = new LinkedHashMap<>();

        static {
            if (ViaFacet.SUPPORTED) {
                try {
                    // Populate until last supported version (This method )
                    TEXT_REWRITER.put(ProtocolVersion.v1_12, TranslateRewriter::toClient);
                    final ProtocolManager manager = Via.getManager().getProtocolManager();
                    text(ProtocolVersion.v1_13, manager.getProtocol(Protocol1_12_2To1_13.class).getComponentRewriter());
                    text(ProtocolVersion.v1_14, new ComponentRewriter1_14(manager.getProtocol(Protocol1_13_2To1_14.class)));
                    text(ProtocolVersion.v1_16, manager.getProtocol(Protocol1_15_2To1_16.class).getComponentRewriter());
                    text(ProtocolVersion.v1_17, manager.getProtocol(Protocol1_16_4To1_17.class).getComponentRewriter());
                    text(ProtocolVersion.v1_18, manager.getProtocol(Protocol1_17_1To1_18.class).getComponentRewriter());
                    text(ProtocolVersion.v1_19, manager.getProtocol(Protocol1_18_2To1_19.class).getComponentRewriter());
                    text(ProtocolVersion.v1_19_3, manager.getProtocol(Protocol1_19_1To1_19_3.class).getComponentRewriter());

                    tag(ProtocolVersion.v1_20_5, manager.getProtocol(Protocol1_20_3To1_20_5.class).getComponentRewriter());
                    tag(ProtocolVersion.v1_21, manager.getProtocol(Protocol1_20_5To1_21.class).getComponentRewriter());
                    tag(ProtocolVersion.v1_21_2, manager.getProtocol(Protocol1_21To1_21_2.class).getComponentRewriter());
                    tag(ProtocolVersion.v1_21_4, manager.getProtocol(Protocol1_21_2To1_21_4.class).getComponentRewriter());
                    tag(ProtocolVersion.v1_21_5, manager.getProtocol(Protocol1_21_4To1_21_5.class).getComponentRewriter());
                    tag(ProtocolVersion.v1_21_6, manager.getProtocol(Protocol1_21_5To1_21_6.class).getComponentRewriter());
                } catch (Throwable ignored) { }
            }
        }

        private static <T extends com.viaversion.viaversion.api.rewriter.ComponentRewriter> void text(final @NotNull ProtocolVersion version, final @NotNull T rewriter) {
            TEXT_REWRITER.put(version, rewriter::processText);
        }

        private static <T extends com.viaversion.viaversion.api.rewriter.ComponentRewriter> void tag(final @NotNull ProtocolVersion version, final @NotNull T rewriter) {
            TAG_REWRITER.put(version, rewriter::processTag);
        }

        public static void processText(final @NotNull UserConnection connection, final @NotNull JsonElement element, final @NotNull ProtocolVersion to) {
            processText(connection, element, Via.getAPI().getServerVersion().highestSupportedProtocolVersion(), to);
        }

        public static void processText(final @NotNull UserConnection connection, final @NotNull JsonElement element, final @NotNull ProtocolVersion from, final @NotNull ProtocolVersion to) {
            for (Map.Entry<ProtocolVersion, BiConsumer<UserConnection, JsonElement>> entry : TEXT_REWRITER.entrySet()) {
                if (from.newerThanOrEqualTo(entry.getKey())) {
                    continue;
                }
                if (to.olderThan(entry.getKey())) {
                    break;
                }
                entry.getValue().accept(connection, element);
            }
        }

        public static void processTag(final @NotNull UserConnection connection, final @NotNull Tag tag, final @NotNull ProtocolVersion to) {
            processTag(connection, tag, Via.getAPI().getServerVersion().highestSupportedProtocolVersion(), to);
        }

        public static void processTag(final @NotNull UserConnection connection, final @NotNull Tag tag, final @NotNull ProtocolVersion from, final @NotNull ProtocolVersion to) {
            for (Map.Entry<ProtocolVersion, BiConsumer<UserConnection, Tag>> entry : TAG_REWRITER.entrySet()) {
                if (from.newerThanOrEqualTo(entry.getKey())) {
                    continue;
                }
                if (to.olderThan(entry.getKey())) {
                    break;
                }
                entry.getValue().accept(connection, tag);
            }
        }
    }

    public static class ProtocolBased<V> extends ViaFacet<V> {
        private final Class<? extends Protocol<?, ?, ?, ?>> protocol;
        private final PacketType packetType;

        protected ProtocolBased(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction, final @NotNull String minProtocol, final @NotNull Class<? extends Protocol<?, ?, ?, ?>> protocol, final @NotNull PacketType packetType) {
            super(viewerClass, connectionFunction, minProtocol);

            this.protocol = protocol;
            this.packetType = packetType;
        }

        public PacketWrapper createPacket(final @NotNull V viewer) {
            return createPacket(this.findConnection(viewer));
        }

        public PacketWrapper createPacket(final @Nullable UserConnection connection) {
            return PacketWrapper.create(this.packetType, null, connection);
        }

        public void sendPacket(final @NotNull PacketWrapper packet) {
            if (packet.user() == null) return;
            try {
                packet.scheduleSend(this.protocol);
            } catch (final Throwable error) {
                logError(error, "Failed to send ViaVersion packet: %s %s", packet.user(), packet);
            }
        }

        public void writeComponent(final @NotNull PacketWrapper packet, final @NotNull String message) {
            final JsonElement element = JsonParser.parseString(message);
            ComponentRewriter.processText(packet.user(), element, this.minProtocol);
            packet.write(Types.COMPONENT, element);
        }
    }

    public static class Chat<V> extends ProtocolBased<V> implements ChatPacket<V, String> {
        public Chat(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
            super(viewerClass, connectionFunction, PROTOCOL_HEX_COLOR, Protocol1_15_2To1_16.class, ClientboundPackets1_16.CHAT);
        }

        @Override
        public void sendMessage(final @NotNull V viewer, final @NotNull Identity source, final @NotNull String message, final @NotNull Object type) {
            final PacketWrapper packet = this.createPacket(viewer);

            writeComponent(packet, message);
            packet.write(Types.BYTE, this.createMessageType(type instanceof MessageType ? (MessageType) type : MessageType.SYSTEM));
            packet.write(Types.UUID, source.uuid());

            this.sendPacket(packet);
        }
    }

    public static class ActionBar<V> extends Chat<V> implements Facet.ActionBar<V, String> {
        public ActionBar(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
            super(viewerClass, connectionFunction);
        }

        @Override
        public byte createMessageType(final @NotNull MessageType type) {
            return TYPE_ACTION_BAR;
        }

        @Override
        public void sendMessage(final @NotNull V viewer, final @NotNull String message) {
            this.sendMessage(viewer, Identity.nil(), message, MessageType.CHAT);
        }
    }

    public static class ActionBarTitle<V> extends ProtocolBased<V> implements Facet.ActionBar<V, String> {
        public ActionBarTitle(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
            super(viewerClass, connectionFunction, TitlePacket.PROTOCOL_ACTION_BAR, Protocol1_10To1_11.class, ClientboundPackets1_9_3.SET_TITLES);
        }

        @Override
        public void sendMessage(final @NotNull V viewer, final @NotNull String message) {
            final PacketWrapper packet = this.createPacket(viewer);

            packet.write(Types.VAR_INT, TitlePacket.ACTION_ACTIONBAR);
            writeComponent(packet, message);

            this.sendPacket(packet);
        }
    }

    public static class Title<V> extends ProtocolBased<V> implements Facet.TitlePacket<V, String, List<Consumer<PacketWrapper>>, Consumer<V>> {
        public Title(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
            super(viewerClass, connectionFunction, PROTOCOL_HEX_COLOR, Protocol1_15_2To1_16.class, ClientboundPackets1_16.SET_TITLES);
        }

        @Override
        public @NotNull List<Consumer<PacketWrapper>> createTitleCollection() {
            return new ArrayList<>();
        }

        @Override
        public void contributeTitle(final @NotNull List<Consumer<PacketWrapper>> coll, final @NotNull String title) {
            coll.add(packet -> {
                packet.write(Types.VAR_INT, ACTION_TITLE);
                writeComponent(packet, title);
            });
        }

        @Override
        public void contributeSubtitle(final @NotNull List<Consumer<PacketWrapper>> coll, final @NotNull String subtitle) {
            coll.add(packet -> {
                packet.write(Types.VAR_INT, ACTION_SUBTITLE);
                writeComponent(packet, subtitle);
            });
        }

        @Override
        public void contributeTimes(final @NotNull List<Consumer<PacketWrapper>> coll, final int inTicks, final int stayTicks, final int outTicks) {
            coll.add(packet -> {
                packet.write(Types.VAR_INT, ACTION_TIMES);
                packet.write(Types.INT, inTicks);
                packet.write(Types.INT, stayTicks);
                packet.write(Types.INT, outTicks);
            });
        }

        @Override
        public @Nullable Consumer<V> completeTitle(final @NotNull List<Consumer<PacketWrapper>> coll) {
            return v -> {
                for (int i = 0, length = coll.size(); i < length; i++) {
                    final PacketWrapper packet = this.createPacket(v);
                    coll.get(i).accept(packet);
                    this.sendPacket(packet);
                }
            };
        }

        @Override
        public void showTitle(final @NotNull V viewer, final @NotNull Consumer<V> title) {
            title.accept(viewer);
        }

        @Override
        public void clearTitle(final @NotNull V viewer) {
            final PacketWrapper packet = this.createPacket(viewer);
            packet.write(Types.VAR_INT, ACTION_CLEAR);
            this.sendPacket(packet);
        }

        @Override
        public void resetTitle(final @NotNull V viewer) {
            final PacketWrapper packet = this.createPacket(viewer);
            packet.write(Types.VAR_INT, ACTION_RESET);
            this.sendPacket(packet);
        }
    }

    public static final class BossBar<V> extends ProtocolBased<V> implements Facet.BossBarPacket<V> {
        private final Set<V> viewers;
        private UUID id;
        private String title;
        private float health;
        private int color;
        private int overlay;
        private byte flags;

        private BossBar(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction, final @NotNull Class<? extends Protocol<?, ?, ?, ?>> protocol, final @NotNull PacketType packetType, final @NotNull Collection<V> viewers) {
            super(viewerClass, connectionFunction, PROTOCOL_BOSS_BAR, protocol, packetType);
            this.viewers = new CopyOnWriteArraySet<>(viewers);
        }

        public static class Builder<V> extends ViaFacet<V> implements Facet.BossBar.Builder<V, Facet.BossBar<V>> {
            public Builder(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
                super(viewerClass, connectionFunction, PROTOCOL_HEX_COLOR);
            }

            @Override
            public Facet.@NotNull BossBar<V> createBossBar(final @NotNull Collection<V> viewer) {
                return new ViaFacet.BossBar<>(this.viewerClass, this::findConnection, Protocol1_15_2To1_16.class, ClientboundPackets1_16.BOSS_EVENT, viewer);
            }
        }

        public static class Builder1_9_To_1_15<V> extends ViaFacet<V> implements Facet.BossBar.Builder<V, Facet.BossBar<V>> {
            public Builder1_9_To_1_15(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> connectionFunction) {
                super(viewerClass, connectionFunction, PROTOCOL_BOSS_BAR);
            }

            @Override
            public Facet.@NotNull BossBar<V> createBossBar(final @NotNull Collection<V> viewer) {
                return new ViaFacet.BossBar<>(this.viewerClass, this::findConnection, Protocol1_8To1_9.class, ClientboundPackets1_9.BOSS_EVENT, viewer);
            }
        }

        @Override
        public void bossBarInitialized(final net.kyori.adventure.bossbar.@NotNull BossBar bar) {
            Facet.BossBarPacket.super.bossBarInitialized(bar);
            this.id = UUID.randomUUID();
            this.broadcastPacket(ACTION_ADD);
        }

        @Override
        public void bossBarNameChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
            if (!this.viewers.isEmpty()) {
                this.title = this.createMessage(this.viewers.iterator().next(), newName);
                this.broadcastPacket(ACTION_TITLE);
            }
        }

        @Override
        public void bossBarProgressChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final float oldPercent, final float newPercent) {
            this.health = newPercent;
            this.broadcastPacket(ACTION_HEALTH);
        }

        @Override
        public void bossBarColorChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Color oldColor, final net.kyori.adventure.bossbar.BossBar.@NotNull Color newColor) {
            this.color = this.createColor(newColor);
            this.broadcastPacket(ACTION_STYLE);
        }

        @Override
        public void bossBarOverlayChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay oldOverlay, final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay newOverlay) {
            this.overlay = this.createOverlay(newOverlay);
            this.broadcastPacket(ACTION_STYLE);
        }

        @Override
        public void bossBarFlagsChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
            this.flags = this.createFlag(this.flags, flagsAdded, flagsRemoved);
            this.broadcastPacket(ACTION_FLAG);
        }

        public void sendPacket(final @NotNull V viewer, final int action) {
            final PacketWrapper packet = createPacket(viewer);

            packet.write(Types.UUID, this.id);
            packet.write(Types.VAR_INT, action);
            if (action == ACTION_ADD || action == ACTION_TITLE) {
                writeComponent(packet, this.title);
            }
            if (action == ACTION_ADD || action == ACTION_HEALTH) {
                packet.write(Types.FLOAT, this.health);
            }
            if (action == ACTION_ADD || action == ACTION_STYLE) {
                packet.write(Types.VAR_INT, this.color);
                packet.write(Types.VAR_INT, this.overlay);
            }
            if (action == ACTION_ADD || action == ACTION_FLAG) {
                packet.write(Types.BYTE, this.flags);
            }

            sendPacket(packet);
        }

        public void broadcastPacket(final int action) {
            if (this.isEmpty()) return;
            for (final V viewer : this.viewers) {
                this.sendPacket(viewer, action);
            }
        }

        @Override
        public void addViewer(final @NotNull V viewer) {
            if (this.viewers.add(viewer)) {
                this.sendPacket(viewer, ACTION_ADD);
            }
        }

        @Override
        public void removeViewer(final @NotNull V viewer) {
            if (this.viewers.remove(viewer)) {
                this.sendPacket(viewer, ACTION_REMOVE);
            }
        }

        @Override
        public boolean isEmpty() {
            return this.id == null || this.viewers.isEmpty();
        }

        @Override
        public void close() {
            this.broadcastPacket(ACTION_REMOVE);
            this.viewers.clear();
        }
    }

    public static final class TabList<V> extends ProtocolBased<V> implements Facet.TabList<V, String> {

        public TabList(final @NotNull Class<? extends V> viewerClass, final @NotNull Function<V, UserConnection> userConnection) {
            super(viewerClass, userConnection, PROTOCOL_HEX_COLOR, Protocol1_15_2To1_16.class, ClientboundPackets1_16.TAB_LIST);
        }

        @Override
        public void send(final V viewer, final @Nullable String header, final @Nullable String footer) {
            final PacketWrapper packet = createPacket(viewer);

            writeComponent(packet, header);
            writeComponent(packet, footer);

            sendPacket(packet);
        }
    }
}