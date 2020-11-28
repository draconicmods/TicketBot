package com.dragoninjector.supportbot.commands.admin;

import com.dragoninjector.supportbot.SupportBot;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

@Command(label = {"emote", "emoji"}, permission = Permission.ADMINISTRATOR, hideInHelp = true)
public class EmoteCommand {

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, SupportBot main) {
        String str = args.get(0);
        StringBuilder builder = new StringBuilder("Emoji/Character info:");
        str.codePoints().forEachOrdered(code -> {
            char[] chars = Character.toChars(code);
            String hex = Integer.toHexString(code).toUpperCase();
            while (hex.length() < 4)
                hex = "0" + hex;
            builder.append("\n`\\u").append(hex).append("`   ");
            if (chars.length > 1) {
                String hex0 = Integer.toHexString(chars[0]).toUpperCase();
                String hex1 = Integer.toHexString(chars[1]).toUpperCase();
                while (hex0.length() < 4)
                    hex0 = "0" + hex0;
                while (hex1.length() < 4)
                    hex1 = "0" + hex1;
                builder.append("[`\\u").append(hex0).append("\\u").append(hex1).append("`]   ");
            }
            builder.append(String.valueOf(chars)).append("   _").append(Character.getName(code)).append("_");
        });
        main.getMessenger().sendMessage(channel, builder.toString(), 30);
        return CommandResult.success();

    }
}