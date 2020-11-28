package com.dragoninjector.supportbot.commands.support.admin;

import com.dragoninjector.supportbot.EmbedTemplates;
import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import java.awt.Color;
import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.requests.RestAction;

@Command(label = {"closeticket", "ticketclose"}, usage = "closeticket @User#Discriminator", description = "Transfers an open support ticket to the mentioned user", hideInHelp = true)
public class CloseTicketCommand {

    private final ScheduledExecutorService scheduledTask = Executors.newScheduledThreadPool(2);

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (member.getUser().isBot() || !Util.isTicketChannel(channel)) {
            return CommandResult.noPermission();
        }

        String cTopicFull = channel.getTopic();
        String[] cTopicSplit = cTopicFull.split(" "); // https://regex101.com/r/r1zvJ6/1
        Long userId = Long.valueOf(cTopicSplit[5]);
        Long supportMsgId = Long.valueOf(cTopicSplit[8]);
        Long channelId = Long.valueOf(cTopicSplit[11]);

        if (member.getUser().getIdLong() == userId || Util.isMemberStaff(member)) {

            Message message1 = main.getJDA().getGuildById(main.getGuildID()).getTextChannelById(channelId).getHistory().getMessageById(supportMsgId);
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
                    } catch (NumberFormatException ex) {
                        main.getLogger().info("Support Log cannot be sent to user from ticket " + channelName + " because the user has left the server. Sending only to support log channel");
                    }
                    ArrayList<String> staff = new ArrayList<>();
                    channel.getHistory().retrievePast(100).queue(messages -> {
                        for (Message mesg : messages) {
                            if (Util.isMemberStaff(mesg.getMember())) {
                                if (!staff.contains(mesg.getAuthor().getAsMention())) {
                                    staff.add(mesg.getAuthor().getAsMention());
                                }
                            }
                        }
                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle(channelName + " has been closed!")
                                .addField("Date Created", cTopicSplit[2], true)
                                .setColor(new Color(main.getMainConfig().getConfigValue("colour", "Red").getAsInt(), main.getMainConfig().getConfigValue("colour", "Green").getAsInt(), main.getMainConfig().getConfigValue("colour", "Blue").getAsInt()));
                        if (!staff.isEmpty()) {
                            embed.addField("Staff Involved", String.join(",", staff), true);
                        } else {
                            embed.addField("Staff Involved", "None", true);
                        }
                        if (Util.isLocked(channel)) {
                            main.getLockedLogChannel().sendMessage(new MessageBuilder().setEmbed(embed.build()).build()).addFile(main.getLogDirectory().resolve(channelName + ".txt").toFile()).queue();
                        } else {
                            main.getLogChannel().sendMessage(new MessageBuilder().setEmbed(embed.build()).build()).addFile(main.getLogDirectory().resolve(channelName + ".txt").toFile()).queue();
                        }
                        main.getLogDirectory().resolve(channelName + ".txt").toFile().delete();
                        main.getJDA().getGuildById(main.getGuildID()).getTextChannelById(channelId).delete().queue();
                    });
                });
            };

            AtomicBoolean deleteChannel = new AtomicBoolean(true);
            ReactionMenu reactionMenu = new ReactionMenu.Builder(main.getJDA())
                    .setEmbed(new EmbedBuilder().setColor(Color.GREEN).setTitle("Deleting Channel").setDescription("Channel deletion started by " + member.getAsMention() + ", 10 seconds to abort!").setFooter("React with \uD83D\uDED1 to abort", null).build())
                    .onClick("\uD83D\uDED1", (x, user) -> {
                        if (user.getIdLong() == userId || Util.isMemberStaff(channel.getGuild().getMemberById(user.getIdLong()))) {
                            deleteChannel.set(false);
                            x.removeReaction("\uD83D\uDED1");
                            x.getMessage().setContent(new EmbedBuilder().setColor(Color.RED).setTitle("Cancelled!").setDescription("Ticket Deletion cancelled by " + user.getAsMention()).build());
                            x.destroyIn(10);
                        }
                    })
                    .onDisplay(display -> scheduledTask.schedule(() -> {
                if (deleteChannel.get()) {
                    callback.accept(message1);
                }
            }, 10, TimeUnit.SECONDS)).buildAndDisplay(channel);
            reactionMenu.destroyIn(10);
        } else {
            main.getMessenger().sendEmbed(channel, EmbedTemplates.ERROR.getEmbed().setDescription(member.getAsMention() + " You do not have the ability to lock this channel!").build(), 10);
            return CommandResult.noPermission();
        }
        return CommandResult.success();
    }
}
