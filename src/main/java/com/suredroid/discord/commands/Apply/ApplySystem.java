package com.suredroid.discord.commands.Apply;

import com.google.common.base.Splitter;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.suredroid.discord.CommonUtils;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.DiscordBot;
import com.suredroid.discord.Form.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class ApplySystem { //extends OldCommandSystem{

    public static ArrayList<Long> ids = new ArrayList<>();

    //String options;



    public static void begin(User userVal, String pos, String t) {
        if(ids.contains(userVal.getId())){
            userVal.sendMessage(DUtils.createEmbed("Already in an application.", "You are already completing an application, finish or stop your application to restart.",userVal.getName(),userVal.getAvatar()));
            return;
        } else
            ids.add(userVal.getId());
        String team = t.toLowerCase();
        String position = pos.toLowerCase();
        if(!Forms.list.containsKey(position)){
            userVal.sendMessage(DUtils.createEmbed("Position not available","The position you selected is not ready for applications yet. Please contact ChosenQuill if you believe this is a mistake.\nPosition: " + position,userVal.getName(),userVal.getAvatar()));
            return;
        }
        if (!Apply.hasTeam(team)) {
            userVal.sendMessage(DUtils.createEmbed("Team not available","The team you selected is not ready for applications yet. Please contact ChosenQuill if you believe this is a mistake.\nTeam: " + team, userVal.getName(),userVal.getAvatar()));
            return;
        }
        userVal.sendMessage("Welcome to the application form. You are applying for the **" + position + "** position for the **" + team + "** team.");
        Forms.list.get(position).fill(userVal).thenAccept(results -> {
            ids.remove(results.getUserId());
            if(!results.isCompleted())
                return;

            DUtils.getApi().getUserById(results.getUserId()).whenComplete((user, throwable) -> {
                if(user!=null){

                    FormSet set = new FormSet(Arrays.stream(results.getQuestions()).map(Question::getQuestion).toArray(String[]::new), results.getAnswers(), Status.PENDING);

                    Type type = new TypeToken<UserProfile>(){}.getType();
                    String userFile = "apps/" + results.getUserId() + ".json";
                    try {
                        CommonUtils.getJson(userFile, type).ifPresentOrElse(profile -> {
                            UserProfile gotProfile = (UserProfile) profile;
                            if(!gotProfile.takenForms.containsKey(position))
                                gotProfile.takenForms.put(position, new ArrayList<>());
                            gotProfile.takenForms.get(position).add(set);
                            CommonUtils.writeJson(userFile, gotProfile);
                        }, () -> {
                            UserProfile profile = new UserProfile(user.getDiscriminatedName(),results.getUserId());
                            profile.takenForms.put(position, new ArrayList<>(Collections.singletonList(set)));
                            CommonUtils.writeJson(userFile, profile);
                        });
                    } catch (JsonParseException e){
                        DiscordBot.report(DUtils.createMessage("Old File","<@" + results.getUserId() + "> has an old user file. Skipping storage for now."));
                    }



                    user.sendMessage(
                            "The next portion of the application is to **talk to other people and provide feedback on other's work while staying active on the server**. This is used to get info on how mature you are, how responsive you are, how active you are, as well as other information.\nIf you haven't gotten a response on the outcome of your application in a week, kindly contact ChosenQuill.\n" +
                                    "**Thank you for your time!**");
                    EmbedBuilder embed = new EmbedBuilder();
                    int totalCount = 0, embedCount = 0;
                    boolean continued = false;
                    for (int i = 0; i < results.getQuestions().length; i++) {
                        totalCount += results.getAnswers()[i].length() + results.getQuestions()[i].getQuestion().length();
                        if(totalCount > 5000 || embedCount > 20){
                            send(embed, user, team, position, continued);
                            embed = new EmbedBuilder();
                            totalCount = results.getAnswers()[i].length();
                            embedCount = 0;
                            continued = true;
                        }

                        boolean first = true;
                        for (String answer : Splitter.fixedLength(1000).split(results.getAnswers()[i])) {
                            embed.addField(first ? results.getQuestions()[i].getQuestion() : "Previous Cont.", answer);
                            embedCount++;
                            first = false;
                        }
                    }
                    send(embed, user, team, position, continued);
                }
            });
        });
    }

    private static void send(EmbedBuilder embed, User user, String team, String position, boolean continued){

        embed.setTitle(StringUtils.capitalize(team) + " Application")
                .setAuthor(user.getName(), null, user.getAvatar())
                .setColor(Color.ORANGE)
                .setFooter("Time Submitted: " + CommonUtils.getDateFormatted());
        MessageBuilder message = new MessageBuilder()
                .append(continued ? "Continued App." : "Team: " + StringUtils.capitalize(team) + " | Position: " + StringUtils.capitalize(position) + " | User: <@" + user.getIdAsString()+"> | <@217015504932438026>")
                .setEmbed(embed);
        DUtils.getApi().getServerTextChannelById(488438032324624386L).ifPresentOrElse(channel -> message.send(channel).exceptionally(ExceptionLogger.get()), () -> {
            DUtils.getApi().getCachedUserById(217015504932438026L).ifPresentOrElse(channel -> message.send(channel).exceptionally(ExceptionLogger.get()), () -> {
                user.sendMessage("Error sending application. No channels available. Please contact ChosenQuill.");
            });
        });
    }


    public static void begin(ReactionAddEvent e, String pos, String team) {
        begin(e.getUser(), pos, team);
    }

}

@Data
@AllArgsConstructor
class FormSet {
    private final String[] questions, answers;
    private Status status;
}

@RequiredArgsConstructor
class UserProfile {
    private final String name; @Getter
    private final long id; @Getter
    public final HashMap<String, ArrayList<FormSet>> takenForms = new HashMap<>();

}

enum Status {
    ACCEPTED,
    REJECTED,
    PENDING
}