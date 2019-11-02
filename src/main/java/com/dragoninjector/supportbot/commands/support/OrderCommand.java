package com.dragoninjector.supportbot.commands.support;

import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.EditableMessage;
import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

@Command(label = "order", usage = "order <Order Number>", description = "Sets the order number field in an open ticket", minArgs = 1)
public class OrderCommand {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (member.getUser().isBot() || !Util.isTicketChannel(channel)) {
            return CommandResult.noPermission();
        }
        String[] split = channel.getTopic().split(" ");
        long messageId = Long.valueOf(split[8]);
        long authorId = Long.valueOf(split[5]);
        channel.getMessageById(messageId).queue(msg -> {
            ReactionMenu reaction = new ReactionMenu.Import(msg).build();
            EditableMessage originalMessage = reaction.getMessage();
            if (message.getAuthor().getIdLong() == authorId) {
                // Build replacement embed message for the channel containing the new order number.
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setFooter(originalMessage.getEmbeds().get(0).getFooter().getText(), originalMessage.getEmbeds().get(0).getFooter().getProxyIconUrl())
                        .setColor(originalMessage.getEmbeds().get(0).getColorRaw())
                        .addField(originalMessage.getEmbeds().get(0).getFields().get(0).getName(), originalMessage.getEmbeds().get(0).getFields().get(0).getValue(), true) // Author
                        .addField(originalMessage.getEmbeds().get(0).getFields().get(1).getName(), String.join(" ", args), true) // Order
                        .addField(originalMessage.getEmbeds().get(0).getFields().get(2).getName(), originalMessage.getEmbeds().get(0).getFields().get(2).getValue(), true); // Serial
                // If the embed had ticket information make sure to keep it.
                if (originalMessage.getEmbeds().get(0).getFields().size() == 4) {
                    embedBuilder.addField(originalMessage.getEmbeds().get(0).getFields().get(3).getName(), originalMessage.getEmbeds().get(0).getFields().get(3).getValue(), true); // Ticket
                }

                reaction.getMessage().setContent(embedBuilder.build());
            }
        });
        return CommandResult.success();
    }
}
