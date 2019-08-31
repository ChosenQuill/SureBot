package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.CommonUtils;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.DiscordBot;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.InviteBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

@Command(desc = "Adds a member to the whitelist.", usage = "!whitelist (add,remove,purge,update) [user]", example = "!whitelist remove ChosenQuill#1952 | !whitelist purge")
public class WhiteList {

    public ArrayList<String> users;
    InviteBuilder ib;
    HashMap<Long, Long> roleMap = new HashMap<>();

    public WhiteList() {
        DUtils.getApi().addListener(new ServerLeave());

        users = new ArrayList<>();
        CommonUtils.getJson("whitelist.json", users).ifPresentOrElse(arrayList -> {
            users = arrayList;
        }, () -> CommonUtils.writeJson("whitelist.json", users));

        DUtils.getApi().getServerChannelById(521444000960479232L).ifPresent(serverChannel -> {
            ib = new InviteBuilder(serverChannel).setMaxUses(1).setUnique(true);
        });

        if (DiscordBot.debug) return;
        DUtils.getApi().getServerById(521436239174303761L).ifPresent(server -> {
            ArrayList<String> list = new ArrayList<>();
            for (User user : server.getMembers()) {
                if (!users.contains(user.getIdAsString()) && !user.isBot()) {
                    list.add(user.getName());
                }
            }
            if (list.size() > 0) {
                DiscordBot.report(new MessageBuilder().setEmbed(DUtils.createEmbed("Non-Whitelisted users on server", "On Startup, we detected " + list.size() + " unauthorized users on your server.\nThe users are: " + Arrays.toString(list.toArray())).setColor(Color.RED)));
            }
        });

        refresh();
    }

    public void run(MessageCreateEvent e, String command, String username) {
        if (e.getMessageAuthor().getId() != 217015504932438026L) {
            error(e, 1);
            return;
        }
        if (command.equalsIgnoreCase("add")) {
            DUtils.getUser(username).ifPresentOrElse(user -> {
                if (addUser(user)) {
                    DUtils.sendMessage(e, "User added to whitelist", "User " + user.getName() + " has been added to the whitelist. :thumbsup:");
                    sendInvite(user);
                } else {
                    DUtils.sendMessage(e, "Already on whitelist", "User " + user.getName() + " is already on the whitelist.");
                }
            }, () -> error(e, 2));
        } else if (command.equalsIgnoreCase("remove")) {
            DUtils.getUser(username).ifPresentOrElse(user -> {
                if (users.contains(user.getIdAsString())) {
                    removeUser(user);
                    DUtils.getApi().getServerById(521436239174303761L).ifPresent(server -> server.kickUser(user, "Removed from whitelist."));
                    e.deleteMessage();
                } else {
                    DUtils.sendMessage(e, "Already not on WhiteList", "User " + user.getName() + " is already not on the whitelist.");
                }
            }, () -> error(e, 2));
        } else {
            error(e, 3);
        }
    }

    public void run(MessageCreateEvent e, String command) {
        if (e.getMessageAuthor().getId() != 217015504932438026L) {
            error(e, 1);
            return;
        }
        if (command.equalsIgnoreCase("purge")) {
            DUtils.getApi().getServerById(521436239174303761L).ifPresent(server -> {
                for (User user : server.getMembers()) {
                    if (!users.contains(user.getIdAsString()) && !user.isBot()) {
                        kick(server, user);
                    }
                }
            });
        } else if (command.equalsIgnoreCase("update")) {
            DUtils.getApi().getServerById(521436239174303761L).ifPresent(server ->
                    DUtils.getApi().getServerById(440681682799034408L).ifPresent(server1 -> {
                        Collection<User> users = server.getMembers();
                        for (User user : users) {
                            detect(server, server1, user);
                        }
                    })
            );
            e.deleteMessage();
            DUtils.sendMessage(e, "All users have been updated!", "All users have been updated with positions from the previous server. :+1:");
        } else if (command.equalsIgnoreCase("test")) {
            DUtils.getApi().getServerById(440681682799034408L).ifPresent(server -> {
                Stream.concat(server.getRolesByNameIgnoreCase("FNA Division").get(0).getUsers().stream(), server.getRolesByNameIgnoreCase("Pixel Division").get(0).getUsers().stream())
                        .forEach(user -> addUser(user));
            });
        } else if (command.equalsIgnoreCase("refresh")){
            refresh();
        } else {
            error(e, 3);
        }
    }

    private void refresh() {
        DUtils.getApi().getServerById(521436239174303761L).ifPresent(gamedev ->
                DUtils.getApi().getServerById(440681682799034408L).ifPresent(suredroid -> {
                    suredroid.getRoles().forEach(role -> {
                        if(!gamedev.getRolesByNameIgnoreCase(role.getName()).isEmpty())
                            roleMap.put(role.getId(),gamedev.getRolesByNameIgnoreCase(role.getName()).get(0).getId());
                    });
                })
        );
    }

    public boolean addUser(User user) {
        if (!users.contains(user.getIdAsString())) {
            users.add(user.getIdAsString());
            update();
            return true;
        } else {
            return false;
        }
    }

    public void sendInvite(User user) {
        ib.create().thenAcceptAsync(invite -> {
            user.sendMessage(DUtils.createEmbed("Added to Whitelist - Click here to Join", "You are now on the whitelist.\nClick the link above or use code **" + invite.getCode() + "** to join the server.", user.getName(), user.getAvatar()).setUrl(invite.getUrl().toString()));
        }).exceptionally(ExceptionLogger.get());
    }


    private void detect(Server gamedev, Server suredroid, User user) {
        user.getRoles(suredroid).stream()
                .map(DiscordEntity::getId)
                .filter(id->roleMap.containsKey(id))
                .forEach(id->gamedev.getRoleById(roleMap.get(id))
                        .ifPresent(role -> gamedev.addRoleToUser(user,role)));
    }

    private void error(MessageCreateEvent e, int code) {
        switch (code) {
            case 1:
                DUtils.sendMessage(e, "No perms, no can do.", "You have insufficient permissions to add this user to the whitelist.");
                break;
            case 2:
                DUtils.sendMessage(e, "This is not a valid user", "There is no user with this username.");
                break;
            case 3:
                DUtils.sendMessage(e, "Invalid Argument", "This is not part of the whitelist commands. Use \"!help whitelist\" for more information.");
        }
    }

    public void removeUser(User user) {
        users.remove(user.getIdAsString());
        update();
    }


    private void update() {
        CommonUtils.writeJson("whitelist.json", users);
    }

    private void kick(Server server, User user) {
        user.sendMessage(DUtils.createEmbed("You are not on the whitelist.", "You are not on the whitelist for the Game-Dev team. If you think that this is an error, please contact ChosenQuill.", user.getName(), user.getAvatar()));
        server.kickUser(user);
        DiscordBot.report(DUtils.createMessage("Unauthorized Joining of Server", "User " + user.getDiscriminatedName() + " tried to join the server but is not on the whitelist.", user.getName(), user.getAvatar()));
    }

    //Precondition - Server is GameDev only
    public void check(Server server, User user) {
        if (users.contains(user.getIdAsString()) || user.isBot()) {
            server.getRoleById(522060105823420417L).ifPresentOrElse(role -> {
                server.addRoleToUser(user, role).exceptionally(ExceptionLogger.get());
            }, () -> DiscordBot.report(DUtils.createMessage("Role not present.", "The verified role is not present on the server.", user.getName(), user.getAvatar())));
            DUtils.getApi().getServerById(440681682799034408L).ifPresent(server1 -> {
                detect(server, server1, user);
            });
        } else {
            kick(server, user);
        }
    }

    private class ServerLeave implements ServerMemberLeaveListener {
        @Override
        public void onServerMemberLeave(ServerMemberLeaveEvent e) {
            if (e.getServer().getId() == 521436239174303761L) {
                if (!e.getUser().isBot()) {
                    users.remove(e.getUser().getIdAsString());
                    update();
                }
            }
        }
    }
}
