package de.kaleidox.discomocracy.commands;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import de.kaleidox.javacord.dialogue.input.option.BooleanInput;
import de.kaleidox.javacord.dialogue.input.text.TextInput;
import de.kaleidox.javacord.util.commands.Command;
import de.kaleidox.javacord.util.commands.CommandGroup;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;

import me.xdrop.fuzzywuzzy.FuzzyWuzzy;
import me.xdrop.fuzzywuzzy.levenshtein.Levenshtein;
import me.xdrop.fuzzywuzzy.model.Result;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.embed.EmbedField;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.DiscordRegexPattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static de.kaleidox.discomocracy.Main.API;
import static de.kaleidox.discomocracy.Main.PROPERTY;

@CommandGroup(name = "Basic Commands", description = "All commands for basic interaction with the bot")
public enum BasicCommands {
    INSTANCE;

    @Command(description = "Sends the Bot's invite link", convertStringResultsToEmbed = true)
    public String invite() {
        return "https://discordapp.com/oauth2/authorize?client_id=593050922163240980&scope=bot&permissions=1506094198";
    }

    @Command(
            description = "Set the bot up",
            ordinal = 0,
            requiredDiscordPermission = PermissionType.MANAGE_SERVER,
            convertStringResultsToEmbed = true,
            async = true
    )
    public Object setup(ServerTextChannel stc, Server srv, User usr) {
        // results
        ServerTextChannel voteChannel;
        boolean createEmojisFromVoting = false;
        boolean createChannelsFromVoting = false;
        boolean kickMembersFromVoting = false;
        boolean banMembersFromVoting = false;
        boolean deleteMessagesFromVoting = false;
        boolean muteMembersFromVoting = false;
        boolean deafenMembersFromVoting = false;
        boolean voicekickMembersFromVoting = false;

        // request the vote channel
        Message requestMsg = stc.sendMessage(DefaultEmbedFactory.create()
                .addField("Input required",
                        "Please specify the channel that should be used as the voting channel!"))
                .join();
        String[] embedAccumulation = new String[1];

        String voteChannelResult = new TextInput(stc)
                .withTarget(usr)
                .withTimeout(60, SECONDS)
                .withDefaultValue("")
                .enableResponseDeletion()
                .listenBlocking();

        // check for default value, if, abort
        if (voteChannelResult.equals(""))
            return "No vote channel was provided.";

        if (voteChannelResult.matches("[0-9]+")) {
            // channel id was given
            Optional<ServerTextChannel> textChannelById = srv.getTextChannelById(voteChannelResult);

            if (textChannelById.isPresent())
                voteChannel = textChannelById.get();
            else return "No channel with ID `" + voteChannelResult + "` was found!";
        } else if (voteChannelResult.matches(DiscordRegexPattern.CHANNEL_MENTION.pattern())) {
            // channel mention was given
            Matcher matcher = DiscordRegexPattern.CHANNEL_MENTION.matcher(voteChannelResult);
            matcher.matches();
            String id = matcher.group("id");

            Optional<ServerTextChannel> textChannelById = srv.getTextChannelById(id);

            if (textChannelById.isPresent())
                voteChannel = textChannelById.get();
            else return "No channel with ID `" + id + "` was found!";
        } else {
            // maybe a channel name was given
            final FuzzyWuzzy<Levenshtein> fuzzyWuzzy = FuzzyWuzzy.algorithm(Levenshtein.FACTORY);

            List<ServerTextChannel> results = fuzzyWuzzy
                    .extractLimited(voteChannelResult, srv.getTextChannels(), Nameable::getName, 80)
                    .stream()
                    .map(Result::getReferent)
                    .collect(Collectors.toList());

            if (results.size() == 0)
                return "No channels matching name `" + voteChannelResult + "` were found!";

            voteChannel = results.get(0);
        }
        embedAccumulation[0] = "Voting channel was set to " + voteChannel.getMentionTag() + "!";

        if (srv.hasPermission(API.getYourself(), PermissionType.MANAGE_EMOJIS)) {
            // request emoji creation

            createEmojisFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow creating new emojis through voting?",
                    "Users can vote for new emojis to be created.",
                    "Users can not vote for new emojis to be created.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.MANAGE_CHANNELS)) {
            // request channel creation

            createChannelsFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow creating new channels through voting?",
                    "Users can vote for new channels to be created.",
                    "Users can not vote for new channels to be created.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.KICK_MEMBERS)) {
            // request votekick

            kickMembersFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow kicking members through voting?",
                    "Users can vote for members to be kicked.",
                    "Users can not vote for members to be kicked.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.BAN_MEMBERS)) {
            // request voteban

            banMembersFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow banning members through voting?",
                    "Users can vote for members to be banned.",
                    "Users can not vote for members to be banned.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.MANAGE_MESSAGES)) {
            // request message delete

            deleteMessagesFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow members to delete a message through voting?",
                    "Users can vote for a message being deleted.",
                    "Users can not vote for a message being deleted.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.MUTE_MEMBERS)) {
            // request vote muting

            muteMembersFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow muting members through voting?",
                    "Users can vote for members to be muted.",
                    "Users can not vote for members to be muted.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.DEAFEN_MEMBERS)) {
            // request vote deafening

            deafenMembersFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow deafening members through voting?",
                    "Users can vote for members to be deafened.",
                    "Users can not vote for members to be deafened.");
        }

        if (srv.hasPermission(API.getYourself(), PermissionType.MOVE_MEMBERS)) {
            // request voice kicking

            voicekickMembersFromVoting = booleanRequest(requestMsg, usr, embedAccumulation,
                    "Do you want to allow kicking members from voicechats through voting?",
                    "Users can vote for members to be kicked from voicechats.",
                    "Users can not vote for members to be kicked from voicechats.");
        }

        requestMsg.delete();

        PROPERTY.COMMAND_CHANNEL_ID.setValue(srv).toLong(voteChannel.getId());
        PROPERTY.VOTE_EMOJIS.setValue(srv).toBoolean(createEmojisFromVoting);
        PROPERTY.VOTE_CHANNELS.setValue(srv).toBoolean(createChannelsFromVoting);
        PROPERTY.VOTE_KICK.setValue(srv).toBoolean(kickMembersFromVoting);
        PROPERTY.VOTE_BAN.setValue(srv).toBoolean(banMembersFromVoting);
        PROPERTY.VOTE_MESSAGEDELETE.setValue(srv).toBoolean(deleteMessagesFromVoting);
        PROPERTY.VOTE_MUTE.setValue(srv).toBoolean(muteMembersFromVoting);
        PROPERTY.VOTE_DEAFEN.setValue(srv).toBoolean(deafenMembersFromVoting);
        PROPERTY.VOTE_VOICEKICK.setValue(srv).toBoolean(voicekickMembersFromVoting);

        return "Setup complete!";
    }

    private boolean booleanRequest(Message requestMsg,
                                   User target,
                                   String[] embedAccumulation,
                                   String question,
                                   String yes,
                                   String no) {
        requestMsg.edit(accumulateEmbed(requestMsg, embedAccumulation[0], "Input Required", question));

        boolean yield = new BooleanInput(requestMsg.getChannel())
                .withTarget(target)
                .withTimeout(60, SECONDS)
                .withDefaultValue(true) // assume true because bot has permission to do this already
                .enableResponseDeletion()
                .listenBlocking();

        embedAccumulation[0] = yield ? yes : no;
        return yield;
    }

    private EmbedBuilder accumulateEmbed(
            Message inMessage,
            String previousAccumulation,
            @SuppressWarnings("SameParameterValue") String name,
            String value
    ) {
        Embed embed = inMessage.getEmbeds()
                .stream()
                .filter(test -> !test.getVideo().isPresent()) // filter all embeds with videos
                .findFirst()
                .orElse(null);
        EmbedBuilder builder = embed == null ? DefaultEmbedFactory.create() : embed.toBuilder();

        if (embed == null || embed.getFields().size() <= 1 || embed.getFields().size() > 2)
            return builder.addField(name, value);

        // field count is > 1
        EmbedField zeroField = embed.getFields().get(0);
        builder.removeFields(field -> field.getName().equals(zeroField.getName())
                && field.getValue().equals(zeroField.getValue()));
        EmbedField oneField = embed.getFields().get(1);
        builder.updateFields(field -> field.getName().equals(oneField.getName())
                        && field.getValue().equals(oneField.getValue()),
                field -> {
                    field.setName("Done!");
                    field.setValue(oneField.getValue() + "\n\n" + previousAccumulation);
                });
        return builder.addField(name, value);
    }
}
