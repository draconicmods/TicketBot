package com.dragoninjector.supportbot.commands.support.admin;

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
import net.dv8tion.jda.api.Permission;

@Command(label = {"lockticket", "ticketlock"}, usage = "lockticket", description = "Locks a open support ticket only to the owner of the Discord and the creator of the ticket.", hideInHelp = true)
public class LockTicketCommand {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (member.getUser().isBot() || !Util.isTicketChannel(channel)) {
            return CommandResult.noPermission();
        }
        String cTopicFull = channel.getTopic();
        String[] cTopicSplit = cTopicFull.split(" "); // https://regex101.com/r/r1zvJ6/1
        Long userId = Long.valueOf(cTopicSplit[5]);

        if (member.getUser().getIdLong() == userId || Util.isMemberStaff(member)) {
            if (!Util.isLocked(channel)) {
                // This shouldn't be needed as their override is already put in place on channel creation, the member needs a null check here.
                //channel.getManager().putPermissionOverride(channel.getGuild().getMemberById(userId), 101440L, 0L).complete();

                // Add owner to channel permissions so they can see the ticket and remove the mods.
                channel.getGuild().getRoles().forEach(role -> {
                    if (Util.isRoleOwner(role.getName())) {
                        channel.putPermissionOverride(role).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY).queue();
                    } else if (Util.isRoleMod(role.getName())) {
                        channel.getPermissionOverride(role).delete().queue();
                    }
                });
                // Rename the channel adding a lock to the beginning.
                channel.getManager().setName("\uD83D\uDD12" + channel.getName()).queue();
                main.getMessenger().sendEmbed(channel, EmbedTemplates.CHANNEL_LOCKED.getBuilt());
                main.getLogger().info("Locked channel: " + channel.getName());
            }
        } else {
            main.getMessenger().sendEmbed(channel, EmbedTemplates.ERROR.getEmbed().setDescription(member.getAsMention() + " You do not have the ability to lock this channel!").build(), 10);
            return CommandResult.noPermission();
        }
        return CommandResult.success();
    }
}
