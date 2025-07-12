package dev.spaceseries.spacechat;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.api.message.Message;

public class Messages {

    /**
     * Instance of this class
     */
    private static Messages instance;

    /**
     * Renews the messages
     */
    public static void renew() {
        instance = null;
    }

    /**
     * Gets instance of messages class
     * <p>(Singleton)</p>
     *
     * @return messages
     */
    public static Messages getInstance(SpaceChatPlugin context) {
        if (instance == null) {
            instance = new Messages(context);
        }
        return instance;
    }

    private final SpaceChatPlugin plugin;

    /* General */

    // help
    public Message generalHelp;

    /* Reload */

    // success
    public Message reloadSuccess;

    // failure
    public Message reloadFailure;
    /**
     * Broadcast
     */

    // args
    public Message broadcastArgs;

    // wrapper
    public Message broadcastWrapper;

    /*
      Message
     */
    // args
    public Message messageArgs;

    // self message
    public Message messageSelf;

    // player not found
    public Message messagePlayerNotFound;

    // Ignored player
    public Message messageIgnoredPlayer;

    // Has ignored player
    public Message messageHasIgnoredPlayer;
    // format send
    public Message messageFormatSend;

    // format receive
    public Message messageFormatReceive;

    // cooldown message
    public Message messageCooldown;

    /*
       Reply
     */
    // args
    public Message replyArgs;

    // player not found
    public Message replyNoTarget;

    // player offline
    public Message replyTargetOffline;

    // format send
    public Message replyFormatSend;

    // format receive
    public Message replyFormatReceive;

    /**
     * Channel
     */

    // join
    public Message channelJoin;

    // leave
    public Message channelLeave;

    // listen
    public Message channelListen;

    // mute
    public Message channelMute;
    // invalid

    public Message channelInvalid;

    // access denied
    public Message channelAccessDenied;

    /**
     * Ignore
     */

    // player not found
    public Message playerNotFound, selfIgnore;
    public Message ignoreHelp;
    public Message ignoreAdded, ignoreRemoved, ignoreAlready, ignoreNotFound;

    public Message ignoreListHeader, ignoreListFormat, ignoreListFooter;

    public Message ignorePageNotFound, ignorePageEmpty;
    public Message ignoreNotLoaded;

    public Messages(SpaceChatPlugin plugin) {
        this.plugin = plugin;

        generalHelp = Message.fromConfigurationSection("general.help", this.getLangConfiguration());
        reloadSuccess = Message.fromConfigurationSection("reload.success", this.getLangConfiguration());
        reloadFailure = Message.fromConfigurationSection("reload.failure", this.getLangConfiguration());
        broadcastArgs = Message.fromConfigurationSection("broadcast.args", this.getLangConfiguration());
        broadcastWrapper = Message.fromConfigurationSection("broadcast.wrapper", this.getLangConfiguration());

        messageArgs = Message.fromConfigurationSection("message.args", this.getLangConfiguration());
        messagePlayerNotFound = Message.fromConfigurationSection("message.player-not-found.text", this.getLangConfiguration());
        messageIgnoredPlayer = Message.fromConfigurationSection("message.ignored-player", this.getLangConfiguration());
        messageHasIgnoredPlayer = Message.fromConfigurationSection("message.player-has-ignored", this.getLangConfiguration());
        messageSelf = Message.fromConfigurationSection("message.self-message", this.getLangConfiguration());
        messageFormatSend = Message.fromConfigurationSection("message.format.send", this.getLangConfiguration());
        messageFormatReceive = Message.fromConfigurationSection("message.format.receive", this.getLangConfiguration());
        messageCooldown = Message.fromConfigurationSection("message.cooldown", this.getLangConfiguration());

        replyArgs = Message.fromConfigurationSection("reply.args", this.getLangConfiguration());
        replyNoTarget = Message.fromConfigurationSection("reply.no-target", this.getLangConfiguration());
        replyTargetOffline = Message.fromConfigurationSection("reply.offline-target", this.getLangConfiguration());
        replyFormatSend = Message.fromConfigurationSection("reply.format.send", this.getLangConfiguration());
        replyFormatReceive = Message.fromConfigurationSection("reply.format.receive", this.getLangConfiguration());


        channelJoin = Message.fromConfigurationSection("channel.join", this.getLangConfiguration());
        channelLeave = Message.fromConfigurationSection("channel.leave", this.getLangConfiguration());
        channelListen = Message.fromConfigurationSection("channel.listen", this.getLangConfiguration());
        channelMute = Message.fromConfigurationSection("channel.mute", this.getLangConfiguration());
        channelAccessDenied = Message.fromConfigurationSection("channel.access-denied", this.getLangConfiguration());
        channelInvalid = Message.fromConfigurationSection("channel.invalid", this.getLangConfiguration());

        playerNotFound = Message.fromConfigurationSection("ignore.player-not-found", this.getLangConfiguration());
        selfIgnore = Message.fromConfigurationSection("ignore.self-ignore", this.getLangConfiguration());
        ignoreHelp = Message.fromConfigurationSection("ignore.help", this.getLangConfiguration());
        ignoreAdded = Message.fromConfigurationSection("ignore.added", this.getLangConfiguration());
        ignoreRemoved = Message.fromConfigurationSection("ignore.removed", this.getLangConfiguration());
        ignoreAlready = Message.fromConfigurationSection("ignore.already-ignored", this.getLangConfiguration());
        ignoreNotFound = Message.fromConfigurationSection("ignore.not-ignoring-player", this.getLangConfiguration());

        ignoreListHeader = Message.fromConfigurationSection("ignore.list.header", this.getLangConfiguration());
        ignoreListFormat = Message.fromConfigurationSection("ignore.list.format", this.getLangConfiguration());
        ignoreListFooter = Message.fromConfigurationSection("ignore.list.footer", this.getLangConfiguration());

        ignorePageNotFound = Message.fromConfigurationSection("ignore.list.page.not-found", this.getLangConfiguration());
        ignorePageEmpty = Message.fromConfigurationSection("ignore.list.page.empty", this.getLangConfiguration());
        ignoreNotLoaded = Message.fromConfigurationSection("ignore.not-loaded", this.getLangConfiguration());
    }

    /**
     * Gets the lang configuration from the main class
     *
     * @return The lang configuration
     */
    private ConfigurationAdapter getLangConfiguration() {
        return plugin.getLangConfig().getAdapter();
    }

    @Override
    public String toString() {
        return "Messages{" +
                "plugin=" + plugin +
                ", generalHelp=" + generalHelp +
                ", reloadSuccess=" + reloadSuccess +
                ", reloadFailure=" + reloadFailure +
                ", broadcastArgs=" + broadcastArgs +
                ", broadcastWrapper=" + broadcastWrapper +
                ", messageArgs=" + messageArgs +
                ", messagePlayerNotFound=" + messagePlayerNotFound +
                ", formatSend=" + messageFormatSend +
                ", formatReceive=" + messageFormatReceive +
                ", channelJoin=" + channelJoin +
                ", channelLeave=" + channelLeave +
                ", channelListen=" + channelListen +
                ", channelMute=" + channelMute +
                ", channelInvalid=" + channelInvalid +
                ", channelAccessDenied=" + channelAccessDenied +
                ", playerNotFound=" + playerNotFound +
                '}';
    }
}

