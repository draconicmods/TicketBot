package com.dragoninjector.supportbot.listeners.discord;

import com.dragoninjector.supportbot.EmbedTemplates;
import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.ReactionMenu;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.dv8tion.jda.core.entities.Member;

public class TicketChannelsReactionListener extends ListenerAdapter {

    private final SupportBot main;
    private final ScheduledExecutorService scheduledTask = Executors.newScheduledThreadPool(2);

    public TicketChannelsReactionListener(SupportBot main) {
        this.main = main;
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot() || event.getReaction().isSelf() || !event.isFromType(ChannelType.TEXT)) {
            return;
        }

        TextChannel channel = (TextChannel) event.getChannel();
        if (Util.isTicketChannel(channel)) {

            String topic = channel.getTopic();
            String[] split = topic.split(" "); // https://regex101.com/r/r1zvJ6/1
            String creation = split[2];
            Long userId = Long.valueOf(split[5]);
            Long messageId = Long.valueOf(split[8]);

            if (event.getReactionEmote().getName().equals("\u2705")) {
                if (userId == event.getUser().getIdLong() || Util.isMemberStaff(event.getMember())) {
                    onCheckClicked(event, channel, creation, messageId, userId);
                } else {
                    main.getMessenger().sendEmbed(channel, EmbedTemplates.ERROR.getEmbed().setDescription(event.getMember().getAsMention() + " You do not have the ability to close this ticket!").build(), 10);
                }
            } else if (event.getReactionEmote().getName().equals("\uD83D\uDD12")) {
                if (userId == event.getUser().getIdLong() || Util.isMemberStaff(event.getMember())) {
                    onLockClicked(event, channel, userId);
                } else {
                    main.getMessenger().sendEmbed(channel, EmbedTemplates.ERROR.getEmbed().setDescription(event.getMember().getAsMention() + " You do not have the ability to control the lock status of this channel!").build(), 10);
                }
            }
        }
    }

    public void onCheckClicked(MessageReactionAddEvent event, TextChannel channel, String creation, Long messageId, Long userId) {
        event.getReaction().removeReaction(event.getUser()).queue();
        RestAction<Message> message = event.getJDA().getGuildById(main.getGuildID()).getTextChannelById(channel.getId()).getMessageById(messageId);
        Consumer<Message> callback = (m) -> {
            scheduledTask.execute(() -> {
                String channelName = channel.getName().replace("\uD83D\uDD12", "");
                try {
                    main.getJDA().getUserById(userId).openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(new EmbedBuilder()
                            .setTitle("Issue Completed")
                            .setDescription("Because of this we have sent you a log file containing the history, so that you may look at it in case you encounter the issue again!")
                            .setColor(new Color(main.getMainConfig().getConfigValue("colour", "Red").getAsInt(), main.getMainConfig().getConfigValue("colour", "Green").getAsInt(), main.getMainConfig().getConfigValue("colour", "Blue").getAsInt())).addField("Next Step: ", "If the issue still persists, please create a new ticket!", false)
                            .build()).queue());
                    main.getJDA().getUserById(userId).openPrivateChannel().queue((privateChannel
                            -> privateChannel.sendFile(main.getLogDirectory().resolve(channelName + ".txt").toFile()).queue()));
                } catch (Exception ex) {
                    main.getLogger().info("Support Log cannot be sent to user from ticket " + channelName + " because the user has left the server. Sending only to support log channel");
                }
                ArrayList<String> staff = new ArrayList<>();
                channel.getHistory().retrievePast(100).queue(messages -> {
                    for (Message msg : messages) {
                        if (Util.isMemberStaff(msg.getMember())) {
                            if (!staff.contains(msg.getAuthor().getAsMention())) {
                                staff.add(msg.getAuthor().getAsMention());
                            }
                        }
                    }
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(channelName + " has been closed!")
                            .addField("Date Created", creation, true)
                            .setColor(new Color(main.getMainConfig().getConfigValue("colour", "Red").getAsInt(), main.getMainConfig().getConfigValue("colour", "Green").getAsInt(), main.getMainConfig().getConfigValue("colour", "Blue").getAsInt()));
                    if (!staff.isEmpty()) {
                        embed.addField("Staff Involved", String.join(",", staff), true);
                    } else {
                        embed.addField("Staff Involved", "None", true);
                    }
                    if (Util.isLocked(channel)) {
                        main.getLockedLogChannel().sendFile(main.getLogDirectory().resolve(channelName + ".txt").toFile(), new MessageBuilder().setEmbed(embed.build()).build()).queue();
                    } else {
                        main.getLogChannel().sendFile(main.getLogDirectory().resolve(channelName + ".txt").toFile(), new MessageBuilder().setEmbed(embed.build()).build()).queue();
                    }
                    main.getLogDirectory().resolve(channelName + ".txt").toFile().delete();
                    channel.delete().queue();
                });
            });
        };

        AtomicBoolean deleteChannel = new AtomicBoolean(true);
        ReactionMenu reactionMenu = new ReactionMenu.Builder(event.getJDA())
                .setEmbed(new EmbedBuilder().setColor(Color.GREEN).setTitle("Deleting Channel").setDescription("Channel deletion started by " + event.getMember().getAsMention() + ", 30 seconds to abort!").setFooter("React with \uD83D\uDED1 to abort", null).build())
                .onClick("\uD83D\uDED1", (x, user) -> {
                    if (user.getIdLong() == main.getJDA().getUserById(userId).getIdLong() || Util.isMemberStaff(event.getGuild().getMemberById(user.getIdLong()))) {
                        deleteChannel.set(false);
                        x.removeReaction("\uD83D\uDED1");
                        x.getMessage().setContent(new EmbedBuilder().setColor(Color.RED).setTitle("Cancelled!").setDescription("Ticket Deletion cancelled by " + user.getAsMention()).build());
                        x.destroyIn(10);
                    }
                })
                .onDisplay(display -> scheduledTask.schedule(() -> {
            if (deleteChannel.get()) {
                message.queue(callback);
            }
        }, 30, TimeUnit.SECONDS)).buildAndDisplay(channel);
        reactionMenu.destroyIn(30);
    }

    public void onLockClicked(MessageReactionAddEvent event, TextChannel channel, Long userId) {
        event.getReaction().removeReaction(event.getUser()).queue();

        if (!Util.isLocked(channel)) {
            // This shouldn't be needed as their override is already put in place on channel creation, the member needs a null check here.
            //channel.getManager().putPermissionOverride(channel.getGuild().getMemberById(userId), 101440L, 0L).complete();
            channel.getGuild().getRoles().forEach(role -> {
                if (Util.isRoleOwner(role.getName())) {
                    channel.putPermissionOverride(role).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).queue();
                } else if (Util.isRoleMod(role.getName())) {
                    channel.getPermissionOverride(role).delete().queue();
                }
            });
            channel.getManager().setName("\uD83D\uDD12" + channel.getName()).queue();
            main.getMessenger().sendEmbed(channel, EmbedTemplates.CHANNEL_LOCKED.getBuilt());
            main.getLogger().info("Locked channel: " + channel.getName());
        } else {
            if (Util.isMemberOwner(event.getMember())) {
                channel.getGuild().getRoles().forEach(role -> {
                    if (Util.isRoleMod(role.getName())) {
                        channel.putPermissionOverride(role).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).queue();
                    }
                });
                //channel.getManager().sync().queue();
                channel.getManager().setName(channel.getName().replace("\uD83D\uDD12", "")).queue();
                main.getMessenger().sendEmbed(channel, EmbedTemplates.CHANNEL_UNLOCKED.getBuilt());
                main.getLogger().info("Fully unlocked channel: " + channel.getName());
            } else {
                main.getMessenger().sendEmbed(channel, EmbedTemplates.ERROR.getEmbed().setDescription(event.getMember().getAsMention() + " To protect your personal information this channel can only be unlocked by MatinatorX. Please ask to have the channel unlocked if you feel it necessary.").build(), 10);
            }
        }
    }
}
