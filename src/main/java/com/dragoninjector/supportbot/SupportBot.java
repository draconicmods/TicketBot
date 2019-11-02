package com.dragoninjector.supportbot;

import com.dragoninjector.supportbot.listeners.discord.TicketChannelsReactionListener;
import com.dragoninjector.supportbot.listeners.discord.TicketCreationListener;
import com.dragoninjector.supportbot.listeners.discord.SupportCategoryListener;
import com.dragoninjector.supportbot.commands.support.TicketToggleCommand;
import com.dragoninjector.supportbot.commands.support.OrderCommand;
import com.dragoninjector.supportbot.commands.support.SerialCommand;
import com.dragoninjector.supportbot.commands.support.SupportSetup;
import com.dragoninjector.supportbot.commands.admin.EmoteCommand;
import com.dragoninjector.supportbot.commands.admin.EmbedCommand;
import com.dragoninjector.supportbot.commands.support.admin.CloseTicketCommand;
import com.dragoninjector.supportbot.commands.support.admin.LockTicketCommand;
import com.dragoninjector.supportbot.commands.support.admin.TransferTicketCommand;
import com.dragoninjector.supportbot.commands.support.admin.UnlockTicketCommand;
import com.google.gson.JsonObject;
import me.bhop.bjdautilities.Messenger;
import me.bhop.bjdautilities.command.CommandHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import net.dv8tion.jda.core.entities.TextChannel;

public class SupportBot {

    private static SupportBot instance;

    private Path logDirectory;
    private Path attachmentDir;
    private Messenger messenger;
    private Path directory;
    private Path configDirectory;
    private final SupportBot bot = this;
    private Config mainConfig;
    private JsonObject mainConf;
    private Logger logger;
    private JDA jda;
    private boolean ticketsEnabled = true;
    private final CommandResponses responses = new CommandResponses();

    public void init(Path directory, Path configDirectory) throws Exception {
        instance = this;
        this.directory = directory;
        this.configDirectory = configDirectory;
        logger = LoggerFactory.getLogger("SupportBot");
        logger.info("Initializing Config!");

        try {
            initConfig(configDirectory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize mainConfig!", e);
        }

        try {

            if (mainConfig.getConfigValue("token").getAsString().equals("add_me")) {
                System.out.println("Please add the bot token into the Config.");
                System.exit(1);
            }

            logger.info("Setting Preferences...");
            this.jda = new JDABuilder(AccountType.BOT)
                    .setToken(mainConfig.getConfigValue("token").getAsString())
                    .setEventManager(new ThreadedEventManager())
                    .setGame(Game.playing("with tickets"))
                    .build();
            jda.awaitReady();

            logger.info("Starting Messenger...");
            this.messenger = new Messenger();

            logger.info("Registering Commands...");

            CommandHandler handler = new CommandHandler.Builder(jda).setGenerateHelp(true).addCustomParameter(this).setResponses(responses).setEntriesPerHelpPage(6).guildIndependent().setCommandLifespan(10).setPrefix(getPrefix()).setResponseLifespan(90).build();

            handler.register(new SupportSetup());
            handler.register(new EmbedCommand());
            handler.register(new TicketToggleCommand());
            handler.register(new OrderCommand());
            handler.register(new SerialCommand());
            handler.register(new CloseTicketCommand());
            handler.register(new LockTicketCommand());
            handler.register(new TransferTicketCommand());
            handler.register(new UnlockTicketCommand());
            handler.register(new EmoteCommand());
            logger.info("Registering Listeners...");
            this.jda.addEventListener(new TicketCreationListener(this));
            this.jda.addEventListener(new SupportCategoryListener(this));
            this.jda.addEventListener(new TicketChannelsReactionListener(this));

        } catch (LoginException ex) {
            ex.printStackTrace();
        }

        try {
            logger.info("Checking Log directories...");
            Path logs = Paths.get(directory + "/logs");
            if (!Files.exists(logs)) {
                Files.createDirectories(logs);
                logDirectory = logs;
            }
            logDirectory = logs;
            Path attachments = Paths.get(directory + "/attachments");
            if (!Files.exists(attachments)) {
                Files.createDirectories(attachments);
                attachmentDir = attachments;
            }
            attachmentDir = attachments;
        } catch (IOException ex) {
            logger.error("Error creating directories!", ex);
        }

        logger.info("Everything Loaded Successfully | Ready to accept input!");
    }

    public void shutdown() {
        logger.info("Initiating Shutdown...");
        getJDA().shutdown();
        logger.info("Shutdown Complete.");
    }

    public void reloadConfig() {
        try {
            this.initConfig(getDirectory());
        } catch (Exception e) {
            getLogger().error("", e);
        }
    }

    private void initConfig(Path configDirectory) {
        try {
            mainConfig = new Config(this, configDirectory);
            mainConf = mainConfig.newConfig("config", Config.writeDefaults());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Path getDirectory() {
        return directory;
    }

    public JDA getJDA() {
        return this.jda;
    }

    public JsonObject getMainConf() {
        return mainConf;
    }

    public Config getMainConfig() {
        return mainConfig;
    }

    public String getPrefix() {
        return mainConfig.getConfigValue("commandPrefix").getAsString();
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isTicketsEnabled() {
        return ticketsEnabled;
    }

    public void setTicketsEnabled(boolean ticketsEnabled) {
        this.ticketsEnabled = ticketsEnabled;
    }

    public Path getLogDirectory() {
        return logDirectory;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public long getGuildID() {
        return mainConfig.getConfigValue("guildID").getAsLong();
    }

    public TextChannel getLogChannel() {
        return getJDA().getGuildById(getGuildID()).getTextChannelById(mainConfig.getConfigValue("logChannelId").getAsLong());
    }

    public TextChannel getLockedLogChannel() {
        return getJDA().getGuildById(getGuildID()).getTextChannelById(mainConfig.getConfigValue("lockedLogChannelId").getAsLong());
    }

    public Path getAttachmentDir() {
        return attachmentDir;
    }

    public static SupportBot getInstance() {
        return instance;
    }

    private final class ThreadedEventManager extends InterfacedEventManager {

        private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

        @Override
        public void handle(Event e) {
            executor.submit(() -> super.handle(e));
        }
    }
}
