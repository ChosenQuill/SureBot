package com.suredroid.discord;

import com.suredroid.discord.Configs.GlobalConfig;
import com.suredroid.discord.Moderation.AutoMod;
import com.suredroid.discord.Moderation.DefaultSwearFilter;
import com.suredroid.discord.commands.WhiteList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Main {

    private static Date date = new Date();
    public static HashMap<String, Long> roles = new HashMap<>();
    public static final Logger logger = LogManager.getRootLogger();
    public static WhiteList whiteList;
    public static Random random = new Random();

    public static void main(String[] args) {
        boolean debug = false;
        if(args.length > 0 && args[0].equalsIgnoreCase("debug")) debug = true;

        String location;
        if(debug) location = "C:\\Users\\rithv\\Desktop\\IntelliJ\\Discord Bots";
        else location = "/opt/discord";
        location +="/storage/";

        DiscordBot.setStoragePath(location);
        DiscordBot.debug = debug;

        //  DUtils.setFooter(()->"ChosenQuill's bot | Executed At: " + CommonUtils.getDateFormatted());
        DiscordApi api = DiscordBot.start(System.getenv("discord_key"),new GlobalConfig().setLoggingChannelId("440702341537202182").setReportingChannelId("524830688797786123"));

        api.updateActivity(ActivityType.WATCHING,"!help | SureDroid.com"); //Here you will tell what will bot do.
        api.updateStatus(UserStatus.DO_NOT_DISTURB); //Status of bot. ONLINE/IDLE/DO_NOT_DISTURB/INVISIBLE/OFFLINE

        api.addListener(new ServerJoin());

        DiscordBot.getCommandManager().addListener(new AutoMod().setFilter(new DefaultSwearFilter().loadCSVFile("/files/word_filter.csv")));
        whiteList = DiscordBot.getCommandManager().generateObject(WhiteList.class);

        api.getServerTextChannelById("605956505291718679").ifPresent(channel->channel.addMessageCreateListener(event ->
                event.addReactionsToMessage("\u2764").exceptionally(ExceptionLogger.get())));


        //api.getServerById(440681682799034408L).ifPresent(server -> server.getRoles().forEach(role -> roles.put(role.getName().toLowerCase(),role.getId())));
    }

    public static boolean isChosenQuill(MessageCreateEvent e){
        return e.getMessageAuthor().getId() == 217015504932438026L;
    }

}
