package com.dragoninjector.supportbot.listeners.discord;

import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class SupportCategoryListener extends ListenerAdapter {

    private final SupportBot main;

    public SupportCategoryListener(SupportBot main) {
        this.main = main;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !Util.isTicketChannel(event.getChannel())) {
            return;
        }

        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("[dd/MM/YY HH:mm]");
            String channelName = event.getChannel().getName().replace("\uD83D\uDD12", "");
            if (!Files.exists(main.getLogDirectory().resolve(channelName + ".txt"))) {
                Files.createFile(main.getLogDirectory().resolve(channelName + ".txt"));
            }
            StringBuilder content = new StringBuilder();
            content.append("[").append(OffsetDateTime.now().format(format)).append("]");
            if (!event.getMessage().getEmbeds().isEmpty()) {
                event.getMessage().getEmbeds().forEach((embed) -> {
                    content.append(event.getMember().getEffectiveName()).append(": ").append("Embed").append(embed.toData().toString());
                });
            }
            if (!event.getMessage().getMentionedMembers().isEmpty()) {
                String message = event.getMessage().getContentRaw();
                for (Member mention : event.getMessage().getMentionedMembers()) {
                    message = message.replace(mention.getAsMention(), mention.getEffectiveName());
                }
                content.append(event.getMember().getEffectiveName()).append(": ").append(message);
            } else {
                content.append(event.getMember().getEffectiveName()).append(": ").append(event.getMessage().getContentRaw());
            }
            if (!event.getMessage().getAttachments().isEmpty()) {
                event.getMessage().getAttachments().forEach((attachment) -> {
                    content.append(event.getMember().getEffectiveName()).append(": ").append("File: ").append(attachment.getProxyUrl());
                });
            }

            Files.write(main.getLogDirectory().resolve(channelName + ".txt"), (content.toString() + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            main.getLogger().error("There was an error writing to a log file for channel: " + event.getChannel().getName(), ex);
        }
    }
}
