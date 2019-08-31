package com.suredroid.discord.commands.Apply;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import org.javacord.api.event.message.MessageCreateEvent;

@Command(desc="Adds a new member to the team! (Director Only)",usage ="{user} (pixel, fna) {artist, writer, music, programmer}",hidden = true)
public class AddTeam {
    public void error(MessageCreateEvent e, int errorCode) {
        switch (errorCode) {
            case 1: //Not Enough Permissions
                DUtils.sendMessage("You do not have the permissions to do this.", "Seems like you do not have permission to access to this command.", e.getMessage().getAuthor().getDisplayName(), e.getMessage().getAuthor().getAvatar(), e.getChannel());
                break;
            case 2:
                DUtils.sendMessage("Invalid team!", "The team you entered is an invalid team. The options are \"fna\" and \"pixel\".", e.getMessage().getAuthor().getDisplayName(), e.getMessage().getAuthor().getAvatar(), e.getChannel());
                break;
            case 3:
                DUtils.sendMessage("Invalid position!", "The position you entered is invalid. The options are \"artist\", \"music\", \"music\", and \"programmer\".", e.getMessage().getAuthor().getDisplayName(), e.getMessage().getAuthor().getAvatar(), e.getChannel());
        }
    }

    public void run(MessageCreateEvent e, String name, String r) {
        String role = r.toLowerCase();
        if (!e.getMessage().getAuthor().canManageRolesOnServer()){ //&& !Main.hasRole(e,510622792266547209L)) {
            error(e,1);
            return;
        }

        if(!role.matches("artist|music|writer|developer")) {
            error(e,3);
            return;
        }

        DUtils.getUser(name).ifPresentOrElse(user -> {
            e.getServer().ifPresent(server -> {
                if (server.getRoles(user).contains(server.getRoleById(488892992896368644L).get()) || server.getRoles(user).contains(server.getRoleById(510595696941072394L).get()) ) {
                    DUtils.sendMessage("User is already part of the group!", "The user is already part of the group. No need to re-add him or her!", e.getMessage().getAuthor().getDisplayName(), e.getMessage().getAuthor().getAvatar(), e.getChannel());
                    return;
                }

                if(e.getMessageAuthor().getId() != 217015504932438026L)
                    DUtils.getApi().getTextChannelById(440702341537202182L)
                            .ifPresent(textChannel -> textChannel.sendMessage("<@217015504932438026> New User Has Been Added - " + user + " | By " + e.getMessageAuthor().getDisplayName()));


                switch(role){
                    case "artist":
                        user.addRole(DUtils.getApi().getRoleById(488572396253478926L).get());
                        break;
                    case "music":
                        user.addRole(DUtils.getApi().getRoleById(498211846369837066L).get());
                        break;
                    case "writer":
                        user.addRole(DUtils.getApi().getRoleById(498211444882538497L).get());
                        break;
                    case "developer":
                        user.addRole(DUtils.getApi().getRoleById(441107003041906709L).get());
                        break;

                }
                user.openPrivateChannel().thenAcceptAsync(channel -> DUtils.sendMessage("Welcome to the game dev team!", "You have been added by " + user.getName() + ". New categories have just unlocked for you on the server! Read up on the stuff you just unlocked and you will be set on your way.", user.getName(), user.getAvatar(), channel));
                DUtils.sendMessage("Congratulations!", "Congradulations to " + user.getName() + " for making it onto the game-dev team!", user.getName(), user.getAvatar(), e.getChannel());
                e.getMessage().delete();
            });
        },() -> DUtils.sendMessage(e,"This is not a valid user","There is no user with this username."));
    }
}