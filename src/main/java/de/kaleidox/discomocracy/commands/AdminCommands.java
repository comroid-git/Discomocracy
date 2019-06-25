package de.kaleidox.discomocracy.commands;

import de.kaleidox.javacord.util.commands.Command;

import org.javacord.api.entity.user.User;

public enum AdminCommands {
    INSTANCE;

    @Command
    public void shutdown(User user) {
        if (user.isBotOwner())
            System.exit(0);
    }
}
