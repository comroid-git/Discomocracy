package de.comroid.discomocracy.commands.voting;

import de.comroid.javacord.util.commands.Command;
import de.comroid.javacord.util.commands.CommandGroup;

import org.javacord.api.entity.server.Server;

@CommandGroup(name = "Emoji Voting Commands", description = "All commands related to emoji voting")
public enum EmojiVoting {
    INSTANCE;

    @Command(
            aliases = "create-emoji",
            description = "Creates a vote for creating an emoji!"
    )
    public Object startEmojiVote(Server srv) {
        throw new UnsupportedOperationException(); // todo
    }
}
