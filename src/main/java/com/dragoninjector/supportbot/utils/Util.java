package com.dragoninjector.supportbot.utils;

import com.dragoninjector.supportbot.SupportBot;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

public class Util {

    public static String getSafeName(Member member) {
        String name = member.getEffectiveName().replaceAll("\\P{Print}", "");
        if (name.length() == 0) {
            name = member.getUser().getName().replaceAll("\\P{Print}", "");
        }
        if (name.length() == 0) {
            name = member.getUser().getId();
        }
        return name;
    }

    public static boolean isInSupportCategory(TextChannel channel) {
        return channel.getParent().getIdLong() == SupportBot.getInstance().getMainConfig().getConfigValue("supportCategoryId").getAsLong();
    }
    
    public static boolean isCreateChannel(TextChannel channel) {
        return channel.getIdLong() == SupportBot.getInstance().getMainConfig().getConfigValue("TicketCreationChannelID").getAsLong();
    }

    public static boolean isLogsChannel(TextChannel channel) {
        return channel.getIdLong() == SupportBot.getInstance().getMainConfig().getConfigValue("lockedLogChannelId").getAsLong()
                || channel.getIdLong() == SupportBot.getInstance().getMainConfig().getConfigValue("logChannelId").getAsLong();
    }
    
    public static boolean isInternalChannel(TextChannel channel) {
        return isCreateChannel(channel) || isLogsChannel(channel);
    }

    public static boolean isTicketChannel(TextChannel channel) {
        if (channel.getTopic() != null && isInSupportCategory(channel) && !isInternalChannel(channel)) {
            String cTopicFull = channel.getTopic();
            String[] cTopicSplit = cTopicFull.split(" "); // https://regex101.com/r/r1zvJ6/1
            String supportMsgId = cTopicSplit[8];
            Message message = channel.getMessageById(supportMsgId).complete();
            if (message != null) {
                return message.getAuthor().isBot() && message.getAuthor().getIdLong() == message.getJDA().getSelfUser().getIdLong();
            }
        }
        return false;
    }

    public static boolean isLocked(TextChannel channel) {
        return channel.getName().startsWith("\uD83D\uDD12");
    }

    public static boolean isMemberStaff(Member member) {
        return member.getRoles().stream().map(Role::getName).anyMatch(s -> s.equalsIgnoreCase("Creator") || s.equalsIgnoreCase("The Fuzzier") || s.equalsIgnoreCase("The Fuzz"));
    }

    public static boolean isMemberOwner(Member member) {
        return member.isOwner() || member.getRoles().stream().map(Role::getName).anyMatch(s -> s.equalsIgnoreCase("Creator"));
    }

    public static boolean isRoleMod(String roleName) {
        return roleName.equalsIgnoreCase("The Fuzzier") || roleName.equalsIgnoreCase("The Fuzz");
    }

    public static boolean isRoleOwner(String roleName) {
        return roleName.equalsIgnoreCase("Creator");
    }
}
