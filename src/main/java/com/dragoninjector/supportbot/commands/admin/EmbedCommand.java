package com.dragoninjector.supportbot.commands.admin;

import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.time.Instant;
import java.util.List;

@Command(label = {"embed", "message"}, usage = "embed {hex} {message}", minArgs = 2, description = "Sends an embed with the text and hex colour", hideInHelp = true)
public class EmbedCommand {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        if (Util.isMemberStaff(member)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTimestamp(Instant.now());
            if (args.get(0).startsWith("#")) {
                embed.setColor(Color.decode(args.get(0)));
            } else {
                embed.setColor(Color.decode("#" + args.get(0)));
            }
            args.remove(0);
            String desc = String.join(" ", args);
            embed.setDescription(desc);
            channel.sendMessage(embed.build()).queue();
            return CommandResult.success();
        } else {
            return CommandResult.noPermission();
        }
    }
}
