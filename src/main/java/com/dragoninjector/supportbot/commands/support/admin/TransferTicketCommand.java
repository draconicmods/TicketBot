package com.dragoninjector.supportbot.commands.support.admin;

import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import java.io.File;
import java.io.IOException;
import me.bhop.bjdautilities.EditableMessage;
import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@Command(label = {"transferticket", "tickettransfer"}, usage = "transferticket @User#Discriminator", description = "Transfers an open support ticket to the mentioned user", minArgs = 1, hideInHelp = true)
public class TransferTicketCommand {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (member.getUser().isBot() || !Util.isTicketChannel(channel)) {
            return CommandResult.noPermission();
        }
        if (args.isEmpty()) {
            return CommandResult.invalidArguments();
        }
        if (Util.isMemberStaff(member)) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/YY");
            String[] split = channel.getTopic().split(" ");
            Long userId = Long.valueOf(split[5]);
            Long messageId = Long.valueOf(split[8]);
            Pattern p = Pattern.compile("\\d+");
            Matcher m = p.matcher(args.get(0).trim());
            String mentionId = "";
            while (m.find()) {
                mentionId = m.group();
            }

            String finalMentionId = mentionId;
            Member oldAuthor = channel.getGuild().getMemberById(userId);
            Member newAuthor = channel.getGuild().getMemberById(finalMentionId);
            if (newAuthor != null) {
                // Remove old author permissions, this check is here because they may have left.
                if (oldAuthor != null) {
                    channel.getManager().removePermissionOverride(oldAuthor).complete();
                }
                // Replace author in topic and permissions.
                channel.getManager().setTopic(channel.getTopic().replace(userId.toString(), finalMentionId)).complete();
                channel.getManager().putPermissionOverride(newAuthor, 101440L, 0L).complete();
                // Replace author information in first message embed.
                channel.getMessageById(messageId).queue(msg -> {
                    ReactionMenu reaction = new ReactionMenu.Import(msg).build();
                    EditableMessage originalMessage = reaction.getMessage();
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setFooter(originalMessage.getEmbeds().get(0).getFooter().getText(), originalMessage.getEmbeds().get(0).getFooter().getProxyIconUrl())
                            .setColor(originalMessage.getEmbeds().get(0).getColorRaw())
                            .addField(originalMessage.getEmbeds().get(0).getFields().get(0).getName(), newAuthor.getAsMention(), true) // Author
                            .addField(originalMessage.getEmbeds().get(0).getFields().get(1).getName(), originalMessage.getEmbeds().get(0).getFields().get(1).getValue(), true) // Order
                            .addField(originalMessage.getEmbeds().get(0).getFields().get(2).getName(), originalMessage.getEmbeds().get(0).getFields().get(2).getValue(), true) // Serial
                            .addField(originalMessage.getEmbeds().get(0).getFields().get(3).getName(), originalMessage.getEmbeds().get(0).getFields().get(3).getValue(), true); // Ticket
                    reaction.getMessage().setContent(embedBuilder.build());
                    channel.getManager().setTopic("Creation date " + channel.getCreationTime().format(dateFormat) + " Authors ID: " + finalMentionId + " Message ID: " + reaction.getMessage().getIdLong() + " Channel ID: " + channel.getIdLong()).queue();
                });
                // Build the new channel name using the new author and the old channel random ID for history purposes.
                String newChannel = Util.getSafeName(newAuthor) + StringUtils.right(channel.getName(), 6);
                // Rename the log files so the channel history will be complete when the ticket is closed.
                try {
                    File oldFile = main.getLogDirectory().resolve(channel.getName().replace("\uD83D\uDD12", "").toLowerCase() + ".txt").toFile();
                    File newFile = main.getLogDirectory().resolve(newChannel.replace("\uD83D\uDD12", "").toLowerCase() + ".txt").toFile();
                    if (oldFile.exists() && !newFile.exists()) {
                        FileUtils.moveFile(oldFile, newFile);
                    }
                } catch (IOException ex) {
                    main.getLogger().error("Failed to move log file when transfering ticket: " + channel.getName(), ex);
                }
                // Rename the channel with the new authors name.
                channel.getManager().setName(newChannel).queueAfter(500, TimeUnit.MILLISECONDS);
            }
            return CommandResult.success();
        } else {
            return CommandResult.noPermission();
        }
    }
}
