package dev.spaceseries.spacechat.logging.wrap;

import net.kyori.adventure.text.Component;

import java.util.Date;
import java.util.UUID;

public class LogChatWrapper extends LogWrapper {

    /**
     * The sender's name
     */
    private String senderName;

    /**
     * The sender's UUID
     */
    private UUID senderUUID;

    /**
     * Message
     */
    private Component message;

    /**
     * The date at which the chat message was sent
     */
    private Date at;

    /**
     * Creates a new log chat wrapper
     *
     * @param logType    The log type
     * @param senderName The sender name
     * @param senderUUID The sender uuid
     * @param message    message
     * @param at         The time
     */
    public LogChatWrapper(LogType logType, String senderName, UUID senderUUID, Component message, Date at) {
        super(logType);

        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.message = message;
        this.at = at;
    }

    /**
     * Returns sender name
     *
     * @return sender name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Sets sender name
     *
     * @param senderName sender name
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * Returns sender uuid
     *
     * @return sender uuid
     */
    public UUID getSenderUUID() {
        return senderUUID;
    }

    /**
     * Sets sender uuid
     *
     * @param senderUUID sender uuid
     */
    public void setSenderUUID(UUID senderUUID) {
        this.senderUUID = senderUUID;
    }

    /**
     * Returns date
     *
     * @return date
     */
    public Date getAt() {
        return at;
    }

    /**
     * Sets date
     *
     * @param at date
     */
    public void setAt(Date at) {
        this.at = at;
    }

    /**
     * Returns message
     *
     * @return message
     */
    public Component getMessage() {
        return message;
    }

    /**
     * Sets message
     *
     * @param message message
     */
    public void setMessage(Component message) {
        this.message = message;
    }
}
