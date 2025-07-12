package dev.spaceseries.spacechat.command;

import co.aikar.commands.annotation.*;
import dev.spaceseries.spacechat.Messages;
import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.api.message.Message;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@CommandAlias("spacechat")
@CommandPermission("space.chat.command")
public class SpaceChatCommand extends dev.spaceseries.spacechat.api.command.SpaceChatCommand {

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    public SpaceChatCommand(SpaceChatPlugin plugin) {
        super(plugin);
    }

    @CommandAlias("message|msg|m|tell")
    @Subcommand("message|msg|m|tell")
    @CommandCompletion("@chatplayers")
    @CommandPermission("space.chat.command.message")
    @Syntax(" <playerName> <message>")
    public void onMessage(CommandSender sender, @Split(" ") String[] args) {
        if (args.length <= 1) {
            Messages.getInstance(plugin).messageArgs.message(sender);
            return;
        }

        final String senderName = sender instanceof Player ? sender.getName() : "@console";
        final String targetName = args[0];

        if (senderName.equalsIgnoreCase(targetName)) {
            Messages.getInstance(plugin).messageSelf.message(sender);
            return;
        }

        // Verify if player exist
        if (!plugin.getUserManager().isPlayerOnline(targetName)) {
            Messages.getInstance(plugin).messagePlayerNotFound.message(sender);
            return;
        }

        plugin.getUserManager().getByName(senderName, s -> {
            // Verify if target has blocked by you

            if(s.getIgnoredUsers().contains(targetName)){
                Messages.getInstance(plugin).messageHasIgnoredPlayer.message(sender);
                return;
            }

            // Verify if target has blocked you
            plugin.getUserManager().getByName(targetName, user -> {
                if(user.getIgnoredUsers().contains(senderName)){
                    Messages.getInstance(plugin).messageIgnoredPlayer.message(sender);
                    return;
                }

                if (sender instanceof Player) {
                    final long cooldown = SpaceChatConfigKeys.PRIVATE_COOLDOWN.get(plugin.getSpaceChatConfig().getAdapter());
                    if (cooldown > 0) {
                        final long time = COOLDOWN.getOrDefault(((Player) sender).getUniqueId(), 0L);
                        final long currentTime = System.currentTimeMillis();
                        if (time > currentTime) {
                            Messages.getInstance(plugin).messageCooldown.message(sender,
                                    "%time%", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(time - currentTime))
                            );
                            return;
                        }
                        COOLDOWN.put(((Player) sender).getUniqueId(), currentTime + cooldown);
                    }
                }

                // Construct the message
                final String messageStr = String.join(" ", args)
                        .replace(targetName, "") //Replace target to empty
                        .trim(); //Remove unnecessary spaces

                // put replier in map
                plugin.getUserManager().getReplyTargetMap().put(targetName, senderName);

                // messages
                Message formatSend = Messages.getInstance(plugin).messageFormatSend;

                // send a message to player in the same server if is connected
                final Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    Messages.getInstance(plugin).messageFormatReceive.message(target,
                            "%sender%", senderName,
                            "%message%", messageStr
                    );
                    SpaceChatConfigKeys.PRIVATE_NOTIFICATION_SOUND.get(plugin.getSpaceChatConfig().getAdapter()).play(target);
                }

                // send to player
                plugin.getChatManager().sendPrivateMessage(sender, targetName, messageStr, formatSend);
            });
        });
    }

    @CommandAlias("reply|r")
    @Subcommand("reply|r")
    @CommandCompletion("@chatplayers")
    @CommandPermission("space.chat.command.reply")
    @Syntax(" <message>")
    public void onReply(CommandSender sender, @Split(" ") String[] args) {
        if (args.length == 0) {
            Messages.getInstance(plugin).replyArgs.message(sender);
            return;
        }
        final String senderName = sender instanceof Player ? sender.getName() : "@console";
        final String targetName = plugin.getUserManager().getReplyTarget(senderName);

        // Verify if player has a replier
        if (targetName == null) {
            Messages.getInstance(plugin).replyNoTarget.message(sender);
            return;
        }

        // Verify if player is online
        if (!plugin.getUserManager().isPlayerOnline(targetName)) {
            Messages.getInstance(plugin).replyTargetOffline.message(sender, "%target%", targetName);
            return;
        }

        // Construct reply message
        final String messageStr = String.join(" ", args).trim(); //Remove unnecessary spaces


        // messages
        Message formatSend = Messages.getInstance(plugin).replyFormatSend;

        // send a message to player in the same server if is connected
        final Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            Messages.getInstance(plugin).replyFormatReceive.message(target,
                    "%sender%", senderName,
                    "%message%", messageStr
            );
        }

        // put replier in map
        plugin.getUserManager().getReplyTargetMap().put(targetName, senderName);

        // send to player
        plugin.getChatManager().sendPrivateMessage(sender, targetName, messageStr, formatSend);

    }

    @Subcommand("reload")
    @CommandPermission("space.chat.command.reload")
    public void onReload(CommandSender sender) {
        // run async task
        CompletableFuture.runAsync(() -> {
            try {
                // load configs
                plugin.loadConfigs();
                // load formats
                plugin.loadFormats();
                // load chat manager
                plugin.loadChatManager();
                // load connection managers
                plugin.loadSyncServices();

                // initialize sync services, since they have to be instantiated and used after the chat manager
                // is created
                plugin.getChatManager().initSyncServices();

                // load channels
                plugin.loadChannels();

                // load users
                plugin.loadUsers();

                // renew messages
                Messages.renew();

            } catch (Exception e) {
                Messages.getInstance(plugin).reloadFailure.message(sender);
                e.printStackTrace();
                return;
            }
            Messages.getInstance(plugin).reloadSuccess.message(sender);
        });
    }

    @Subcommand("playerlist")
    @CommandPermission("space.chat.command.playerlist")
    public void onPlayerList(CommandSender sender) {
        if (plugin.getUserManager().getOnlinePlayers().isEmpty()) {
            sender.sendMessage("There's no online players");
            return;
        }
        sender.sendMessage("Online players (" + plugin.getUserManager().getOnlinePlayers().size() + "):");
        plugin.getUserManager().getOnlinePlayersByServer().forEach((server, players) -> sender.sendMessage(server + " = " + String.join(", ", players)));
    }

    @Default
    //@CatchUnknown
    @HelpCommand
    public void onDefault(CommandSender sender) {
        // send help message
        Messages.getInstance(plugin).generalHelp.message(sender);
    }
}
