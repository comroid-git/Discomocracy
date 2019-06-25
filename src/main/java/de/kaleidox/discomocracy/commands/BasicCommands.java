package de.kaleidox.discomocracy.commands;

import de.kaleidox.javacord.util.commands.CommandGroup;

@CommandGroup(name = "Basic Commands", description = "All commands for basic interaction with the bot")
public enum BasicCommands {
    INSTANCE;

    public String invite() {
        return "https://discordapp.com/oauth2/authorize?client_id=593050922163240980&scope=bot&permissions=1506094198";
    }
}
