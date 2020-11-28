package com.dragoninjector.supportbot.commands.support;

import com.dragoninjector.supportbot.EmbedTemplates;
import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

@Command(label = "toggleticket", usage = "toggleticket", description = "Disables or enables the ticket system!")
public class TicketToggleCommand {
    
    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (Util.isMemberStaff(member)) {
            if (main.isTicketsEnabled()) {
                main.setTicketsEnabled(false);
                main.getMessenger().sendEmbed(channel, EmbedTemplates.PRETTY_SUCCESSFULL.getEmbed().setDescription("Disabled tickets!").build(), 10);
            } else {
                main.setTicketsEnabled(true);
                main.getMessenger().sendEmbed(channel, EmbedTemplates.PRETTY_SUCCESSFULL.getEmbed().setDescription("Enabled tickets!").build(), 10);
            }
            return CommandResult.success();
        } else {
            return CommandResult.noPermission();
        }
    }
}
