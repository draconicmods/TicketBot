package com.dragoninjector.supportbot;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;

public enum EmbedTemplates {

    BASE(new EmbedBuilder()),
    ERROR(BASE.getEmbed().setColor(Color.RED)),
    SUCCESS(BASE.getEmbed().setColor(Color.GREEN)),
    PRETTY_SUCCESSFULL(BASE.getEmbed().setColor(Color.CYAN)),
    CHANNEL_LOCKED(SUCCESS.getEmbed().setDescription("\uD83D\uDD12 Channel visibility has been locked to just you and MatinatorX!")),
    CHANNEL_UNLOCKED(SUCCESS.getEmbed().setDescription("\uD83D\uDD13 Channel visibility has been unlocked to you and all support staff!")),
    INVALID_NAME(ERROR.getEmbed().setDescription("\u26D4 Invalid username, please check your spelling and try again!"));

    private final EmbedBuilder embed;

    EmbedTemplates(EmbedBuilder embed) {
        this.embed = embed;
    }

    public EmbedBuilder getEmbed() {
        return new EmbedBuilder(embed);
    }

    public EmbedBuilder getEmbed(Object... replacements) {
        EmbedBuilder embed = getEmbed();
        String description = embed.getDescriptionBuilder().toString();
        for (int i = 0; i < replacements.length; i++) {
            description = description.replace("{" + i + "}", replacements[i].toString());
        }
        return embed.setDescription(description);
    }

    public MessageEmbed getBuilt() {
        return getEmbed().build();
    }
}
