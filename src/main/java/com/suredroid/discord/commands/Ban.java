package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.Main;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Arrays;

@Command(desc="Bans an user from the discord for a provided reason.",usage="(user) [# of days] [reason]")
public class Ban {

    public void run(String[] args, MessageCreateEvent e) {
        if (!Main.isChosenQuill(e)) {
            DUtils.sendMessage(e, "No Permissions", "You do not have the permissions to ban someone. Please Contact ChosenQuill if you think there is a problem.");
            return;
        }
        e.getServer().ifPresentOrElse(server -> {
            if (args.length == 0) {
                DUtils.sendTimedMessage(DUtils.createMessage(e, "No User Provided", "Please provide a user to ban."), e.getChannel());
                return;
            }
            if (args.length == 1) {
                checkban(e, server, args[0], 0, null);
            } else {
                if (args[1].chars().allMatch(Character::isDigit)) {
                    try {
                        int daysRemove = Integer.parseInt(args[1]);
                        if (args.length == 2) checkban(e, server, args[0], daysRemove, null);
                        else
                            checkban(e, server, args[0], daysRemove, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                    } catch (NumberFormatException nfe) {
                        DUtils.sendTimedMessage(DUtils.createMessage(e, "Not an integer", "This number is not an integer."), e.getChannel());
                    }
                } else {
                    checkban(e, server, args[0], 0, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                }
            }
        }, () -> DUtils.sendMessage(e, "Unavailable", "You can only run this command on a server."));
    }

    private void checkban(MessageCreateEvent e, Server server, String userval, int time, String reason) {
        DUtils.getUser(userval).ifPresentOrElse(user -> {
            ban(e, server, user, time, reason);
        }, () -> {
            if (userval.chars().allMatch(Character::isDigit)) {
                DUtils.getApi().getUserById(userval).whenComplete(((user, throwable) -> {
                    if (user != null) {
                        ban(e, server, user, time, reason);
                    } else {
                        DUtils.sendMessage(e, "Invalid User", "This user id doesn't match anyone on discord.");
                    }
                }));
            } else
                DUtils.sendTimedMessage(DUtils.createMessage(e, "Invalid User", "This user is not a valid user or is not on the server. To ban users out of the server, please provide their id."), e.getChannel());
        });
    }

    private void ban(MessageCreateEvent e, Server server, User user, int time, String reason) {
        System.out.println("Server: " + server.getName() + "\nUser: " + user.getName() + "\nTime: " + time + "\nReason: " + reason);
        //server.banUser(user,time,reason == null || reason.isEmpty() ? null : reason);
        DUtils.sendTimedMessage(DUtils.createMessage(e, "Ban Successful", "User " + user.getDiscriminatedName() + " has successfully been banned."), e.getChannel());

    }
}