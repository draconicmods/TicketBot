package com.dragoninjector.supportbot.commands.support.admin;

import com.dragoninjector.supportbot.EmbedTemplates;
import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;
import net.dv8tion.jda.core.Permission;

@Command(label = {"unlockticket", "ticketunlock"}, usage = "unlockticket", description = "Unlocks an open support ticket so staff can see.", hideInHelp = true)
public class UnlockTicketCommand {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (member.getUser().isBot() || !Util.isTicketChannel(channel)) {
            return CommandResult.noPermission();
        }
        if (Util.isLocked(channel)) {
            if (Util.isMemberOwner(member)) {
                // Adding the mods back to the channel permissions so they can see the ticket, no need to remove the author or owner role.
                channel.getGuild().getRoles().forEach(role -> {
                    if (Util.isRoleMod(role.getName())) {
                        channel.putPermissionOverride(role).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).queue();
                    }
                });
                // Rename the channel removing the lock from the name.
                channel.getManager().setName(channel.getName().replace("\uD83D\uDD12", "")).queue();
                main.getMessenger().sendEmbed(channel, EmbedTemplates.CHANNEL_UNLOCKED.getBuilt());
                main.getLogger().info("Fully unlocked channel: " + channel.getName());
            } else {
                main.getMessenger().sendEmbed(channel, EmbedTemplates.ERROR.getEmbed().setDescription(member.getAsMention() + " To protect your personal information this channel can only be unlocked by MatinatorX. Please ask to have the channel unlocked if you feel it necessary.").build(), 10);
                return CommandResult.noPermission();
            }
        }
        return CommandResult.success();
    }
}
