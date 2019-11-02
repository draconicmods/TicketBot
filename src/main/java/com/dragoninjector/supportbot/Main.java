package com.dragoninjector.supportbot;

import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends ListenerAdapter {

    public static void main(String[] args) throws Exception {
        final ExecutorService console = Executors.newSingleThreadExecutor();
        final SupportBot bot = new SupportBot();
        Path mainDirectory = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        Path configDirectory = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

        if (args.length > 0) {
            if (args[0].startsWith("configDirectory=")) {
                configDirectory = mainDirectory.resolve(args[0].replace("configDirectory=", ""));
            } else {
                System.out.println("You have given a startup argument! But it doesn't match any we use!");
            }
        }

        console.submit(() -> {
            final Scanner input = new Scanner(System.in);

            String cmd;
            do {
                cmd = input.nextLine();
                switch (cmd) {
                    default:
                        System.out.println("Invalid Command.");
                }
            } while (!cmd.equalsIgnoreCase("exit"));

            bot.shutdown();
            System.exit(0);
        });
        if (!configDirectory.toFile().exists()) {
            configDirectory.toFile().mkdir();
        }
        bot.init(mainDirectory, configDirectory);
    }
}
