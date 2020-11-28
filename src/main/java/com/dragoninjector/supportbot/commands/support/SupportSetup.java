package com.dragoninjector.supportbot.commands.support;

import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.EditableMessage;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.util.List;

@Command(label = {"supportsetup", "setupsupport"}, usage = "setupsupport", description = "Provides information that needs added to the configuration file for support to work properly", permission = Permission.ADMINISTRATOR, hideInHelp = true)
public class SupportSetup {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (Util.isMemberOwner(member)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(new Color(main.getMainConfig().getConfigValue("colour", "Red").getAsInt(), main.getMainConfig().getConfigValue("colour", "Blue").getAsInt(), main.getMainConfig().getConfigValue("colour", "Green").getAsInt()));
            builder.setDescription("To open a support ticket, send a message in this channel which clearly describes your issue. Please include as much information as possible.\n"
                    + "\n"
                    + "Remember to fill out the following information after creating your ticket:\n"
                    + "```/order <Order Number>\n"
                    + "/serial <Serial Number>```\n"
                    + "\n"
                    + "\n"
                    + "\n"
                    + "Press \uD83D\uDD12 to restrict ticket visibility to MatinatorX before sending personal information.\n"
                    + "\n"
                    + "When you open a new ticket, a dedicated channel will be created for you. Within this channel, one of our staff will contact you as soon as possible and attempt to resolve your issue.\n"
                    + "\n"
                    + "Please refrain from opening tickets for questions that can be easily answered in #project-related. Abuse of the ticket system will result in a warning. Repeated abuse will result in a temporary or permanent ban from the server.");
            EditableMessage editableMessage = main.getMessenger().sendEmbed(channel, builder.build(), 0);
            Message message2 = editableMessage;
            message2.pin().queue();
            channel.getManager().setTopic("Read the pinned message to learn how to get support!").queue();
            EmbedBuilder pmBuilder = new EmbedBuilder()
                    .setColor(new Color(main.getMainConfig().getConfigValue("colour", "Red").getAsInt(), main.getMainConfig().getConfigValue("colour", "Green").getAsInt(), main.getMainConfig().getConfigValue("colour", "Blue").getAsInt()))
                    .addField("SupportId", channel.getId(), true)
                    .addField("SupportCategoryId", message.getCategory().getId(), true)
                    .addField("GuildID", String.valueOf(channel.getGuild().getIdLong()), true)
                    .setDescription("Add these values in their corresponding places in your config file. Save the config,"
                            + " then restart the bot!");
            try {
                message.getAuthor().openPrivateChannel().complete().sendMessage(pmBuilder.build()).queue();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return CommandResult.success();
        }
        return CommandResult.noPermission();
    }
}
