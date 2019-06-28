package de.kaleidox.discomocracy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import de.kaleidox.botstats.BotListSettings;
import de.kaleidox.botstats.javacord.JavacordStatsClient;
import de.kaleidox.botstats.model.StatsClient;
import de.kaleidox.discomocracy.commands.AdminCommands;
import de.kaleidox.discomocracy.commands.BasicCommands;
import de.kaleidox.discomocracy.commands.voting.ChannelVoting;
import de.kaleidox.discomocracy.commands.voting.EmojiVoting;
import de.kaleidox.discomocracy.commands.voting.MemberVoting;
import de.kaleidox.discomocracy.commands.voting.VoiceVoting;
import de.kaleidox.javacord.util.commands.CommandHandler;
import de.kaleidox.javacord.util.server.properties.PropertyGroup;
import de.kaleidox.javacord.util.server.properties.ServerPropertiesManager;
import de.kaleidox.javacord.util.ui.embed.DefaultEmbedFactory;
import de.kaleidox.util.files.FileProvider;
import de.kaleidox.util.files.OSValidator;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

import static de.kaleidox.util.files.FileProvider.getFile;

public class Main {
    public final static Color THEME = new Color(0x7289da);

    public static final DiscordApi API;
    public static final CommandHandler CMD;
    public static final ServerPropertiesManager PROP;
    public static final StatsClient STATS;
    public static final Properties PROPERTY;

    static {
        try {
            File file = getFile("login/token.cred");
            System.out.println("Looking for token file at " + file.getAbsolutePath());
            API = new DiscordApiBuilder()
                    .setToken(new BufferedReader(new FileReader(file)).readLine())
                    .login()
                    .exceptionally(ExceptionLogger.get())
                    .join();

            API.updateStatus(UserStatus.DO_NOT_DISTURB);
            API.updateActivity("Booting up...");

            BotListSettings settings = BotListSettings.builder()
                    .postStatsTester(OSValidator::isUnix)
                    .tokenFile(FileProvider.getFile("login/botlist-tokens.properties"))
                    .build();
            STATS = new JavacordStatsClient(settings, API);

            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder()
                    .setColor(THEME)
                    .setAuthor(
                            "Made by Kaleidox#0001",
                            "http://kaleidox.de/#github",
                            "http://kaleidox.de/img/controller.gif")
            );

            CMD = new CommandHandler(API);
            CMD.prefixes = new String[]{"vote!"};
            CMD.useDefaultHelp(null);
            CMD.registerCommands(BasicCommands.INSTANCE);
            CMD.registerCommands(AdminCommands.INSTANCE);

            CMD.registerCommands(
                    EmojiVoting.INSTANCE, ChannelVoting.INSTANCE, MemberVoting.INSTANCE, VoiceVoting.INSTANCE
            );

            PROP = new ServerPropertiesManager(FileProvider.getFile("data/props.json"));
            PROP.usePropertyCommand(null, CMD);
            PROPERTY = new Properties();

            CMD.withCustomPrefixProvider(PROPERTY.PREFIX);
            CMD.withCommandChannelProvider(PROPERTY.COMMAND_CHANNEL_ID);
            CMD.useBotMentionAsPrefix = true;

            API.getThreadPool().getScheduler()
                    .scheduleAtFixedRate(Main::tick, 5, 5, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));


            API.updateActivity(ActivityType.LISTENING, CMD.prefixes[0] + "help");
            API.updateStatus(UserStatus.ONLINE);
        } catch (Exception e) {
            throw new RuntimeException("Error in initializer", e);
        }
    }

    public static void main(String[] args) {
    }

    private static void tick() {
        try {
            STATS.updateTokensFromFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void shutdown() {
    }

    public static class Properties {
        // bot setup
        public final PropertyGroup PREFIX = PROP.register("bot.prefix", CMD.prefixes[0])
                .withDisplayName("Custom Command Prefix")
                .withDescription("A custom prefix to call bot commands with");
        public final PropertyGroup COMMAND_CHANNEL_ID = PROP.register("bot.commandchannel", -1)
                .withDisplayName("Command Channel ID")
                .withDescription("The ID of the only channel where the commands should be executed\n" +
                        "If the ID is invalid, every channel is accepted");

        // vote functionality
        public final PropertyGroup VOTE_CHANNEL_ID = PROP.register("vote.channel.id", -1)
                .withDisplayName("Voting Channel ID")
                .withDescription("The ID of the channel where votes should be posted");
        public final PropertyGroup VOTE_EMOJIS = PROP.register("vote.emojis", false)
                .withDisplayName("Allow Emoji voting")
                .withDescription("Allow Emojis to be created via Uservoting");
        public final PropertyGroup VOTE_CHANNELS = PROP.register("vote.channels", false)
                .withDisplayName("Allow Channel voting")
                .withDescription("Allow Channels to be created via Uservoting");
        public final PropertyGroup VOTE_KICK = PROP.register("vote.kick", false)
                .withDisplayName("Allow User votekick")
                .withDescription("Allow Users to be kicked via Uservoting");
        public final PropertyGroup VOTE_BAN = PROP.register("vote.ban", false)
                .withDisplayName("Allow User voteban")
                .withDescription("Allow Users to be banned via Uservoting");
        public final PropertyGroup VOTE_MESSAGEDELETE = PROP.register("vote.messagedelete", false)
                .withDisplayName("Allow voted message deletion")
                .withDescription("Allow Messages to be deleted via Uservoting");
        public final PropertyGroup VOTE_MUTE = PROP.register("vote.mute", false)
                .withDisplayName("Allow voted User voice-muting")
                .withDescription("Allow Users to be muted via Uservoting");
        public final PropertyGroup VOTE_DEAFEN = PROP.register("vote.deafen", false)
                .withDisplayName("Allow voted User voice-deafening")
                .withDescription("Allow Users to be deafened via Uservoting");
        public final PropertyGroup VOTE_VOICEKICK = PROP.register("vote.voicekick", false)
                .withDisplayName("Allow voice-votekick voting")
                .withDescription("Allow Users to be kicked from VoiceChannels via Uservoting");

        // vote settings
        public final PropertyGroup VOTE_EMOJIS_LIMIT = PROP.register("vote.emojis.limit", 10)
                .withDisplayName("Maximum voted Emojis")
                .withDescription("Maximum amount of voted Emojis; `-1` for infinite");
        public final PropertyGroup VOTE_BAN_DURATION = PROP.register("vote.ban.duration", "1h")
                .withDisplayName("Voteban duration")
                .withDescription("The duration for a voteban; `-1` for infinite");
        public final PropertyGroup VOTE_MUTE_DURATION = PROP.register("vote.mute.duration", "1h")
                .withDisplayName("Votemute duration")
                .withDescription("The duration for a voted voice-mute; `-1` for infinite");
        public final PropertyGroup VOTE_DEAFEN_DURATION = PROP.register("vote.deafen.duration", "1h")
                .withDisplayName("Votedeafen duration")
                .withDescription("The duration for a voted voice-deafen; `-1` for infinite");

        private Properties() {}
    }
}
