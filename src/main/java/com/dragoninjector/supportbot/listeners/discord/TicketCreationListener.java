package com.dragoninjector.supportbot.listeners.discord;

import com.dragoninjector.supportbot.EmbedTemplates;
import com.dragoninjector.supportbot.SupportBot;
import com.dragoninjector.supportbot.utils.Util;
import me.bhop.bjdautilities.ReactionMenu;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class TicketCreationListener extends ListenerAdapter {

    private final SupportBot main;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private int userCount;

    public TicketCreationListener(SupportBot main) {
        this.main = main;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !Util.isCreateChannel(event.getChannel()) || event.getMessage().getContentRaw().contains("supportsetup") || event.getMessage().getContentRaw().contains("setupsupport")) {
            return;
        }

        Member member = event.getMember();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/YY");

        if (main.isTicketsEnabled()) {
            for (TextChannel channel : main.getJDA().getCategoryById(Long.valueOf(main.getMainConfig().getConfigValue("supportCategoryId").getAsString())).getTextChannels()) {
                if (channel.getName().startsWith(event.getAuthor().getName().toLowerCase())) {
                    userCount++;
                    if (userCount >= 1) {
                        event.getMessage().delete().queueAfter(500, TimeUnit.MILLISECONDS);
                        main.getMessenger().sendEmbed(event.getChannel(), EmbedTemplates.ERROR.getEmbed().appendDescription("No channel has been created because you have multiple channels open already. Please complete these issue first!").build(), 10);
                        return;
                    }
                }
            }

            String userMessage = event.getMessage().getContentRaw();
            event.getMessage().delete().queueAfter(500, TimeUnit.MILLISECONDS);

            TextChannel supportChannel = (TextChannel) event.getJDA().getCategoryById(main.getMainConfig().getConfigValue("supportCategoryId").getAsLong())
                    .createTextChannel(Util.getSafeName(member) + "-" + ThreadLocalRandom.current().nextInt(99999)).complete();

            supportChannel.putPermissionOverride(member).setAllow(101440L).complete();
            EmbedBuilder message = new EmbedBuilder()
                    .addField("Author: ", member.getAsMention(), true)
                    .addField("Order: ", "Run `" + main.getPrefix() + "order <Order Number>` to set this field", true)
                    .addField("Serial:", "Run `" + main.getPrefix() + "serial <Serial Number>` to set this field", true)
                    .setFooter("If you have resolved your issue, please click \u2705 to close this ticket.", event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                    .setColor(new Color(main.getMainConfig().getConfigValue("colour", "Red").getAsInt(), main.getMainConfig().getConfigValue("colour", "Green").getAsInt(), main.getMainConfig().getConfigValue("colour", "Blue").getAsInt()));
            ReactionMenu supportMessage;
            if (userMessage.length() > 1024) {
                supportMessage = new ReactionMenu.Builder(event.getJDA()).setStartingReactions("\u2705", "\uD83D\uDD12").setEmbed(message.build()).setMessage(userMessage).buildAndDisplay(supportChannel);
            } else {
                supportMessage = new ReactionMenu.Builder(event.getJDA()).setStartingReactions("\u2705", "\uD83D\uDD12").setEmbed(message.addField("Ticket: ", userMessage, true).build()).buildAndDisplay(supportChannel);
            }
            supportChannel.getManager().setTopic("Creation date: " + supportChannel.getCreationTime().format(dateFormat) + " Authors ID: " + event.getAuthor().getIdLong() + " Message ID: " + supportMessage.getMessage().getIdLong() + " Channel ID: " + supportChannel.getIdLong()).queue();

            Runnable task = () -> main.getMessenger().sendEmbed(supportChannel, EmbedTemplates.ERROR.getEmbed().setDescription("For quicker support, send ``/order`` followed by your order number, then ``/serial`` followed by your serial number.").build());
            executorService.schedule(task, 5, TimeUnit.SECONDS);
        } else {
            main.getMessenger().sendEmbed(event.getChannel(), EmbedTemplates.ERROR.getEmbed().setDescription("Tickets are currently disabled from being made!").build(), 10);
        }
    }
}
