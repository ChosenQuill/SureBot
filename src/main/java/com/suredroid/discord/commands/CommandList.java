package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.Error;
import com.suredroid.discord.*;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CommandList {
    @Command(desc = "Gets the id of a role",usage="[@role]")
    public void roleid(MessageCreateEvent e, String role){
        if(!Main.isChosenQuill(e)){
            Error.NoPermission.send(e);
            return;
        }
        e.getServer().ifPresentOrElse(server -> {
            DUtils.getRole(role,server).ifPresentOrElse(id->DUtils.sendTimedMessage(DUtils.createMessage(e,id.getName() + " Role","The id for this role is ``" + id.getId() + "``"),e.getChannel()),()->Error.RoleNotFound.send(e));
        },()-> Error.ServerOnly.send(e));
        e.deleteMessage();
    }

    @Command(desc="Multiplies two numbers",usage = "firstNumber secondNumber")
    public void multiply(MessageCreateEvent e, String numberOne, String numberTwo)
    {
        if(CommonUtils.isInteger(numberOne) && CommonUtils.isInteger(numberTwo)) {
            DUtils.sendMessage(e,"Multiplied number " + numberOne + " by " + numberTwo,
                    "The result is " + (Integer.parseInt(numberOne) * Integer.parseInt(numberTwo)));
        } else {
            Error.NotANumber.send(e);
        }
    }

    @Command(desc="Tells you the member count of the server.", serverOnly = true)
    public EmbedMessage count(MessageCreateEvent e) {
        return new EmbedMessage("Server Count", "This server has " + e.getServer().get().getMemberCount() + " members.");
    }

    @Command(desc = "Flips a coin.")
    public void flipacoin(MessageCreateEvent e) {
        String image, value;
        if (Main.random.nextBoolean()) {
            value = "heads";
            image = "https://i.imgur.com/bY5ya4L.png";
        } else {
            value = "tails";
            image = "https://i.imgur.com/RI0kbcx.png";
        }
        DUtils.sendTimedMessage(DUtils.createMessage(DUtils.createEmbed("You flipped a coin!", "It landed " + value + ".", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar())
                .setImage(image)), e.getChannel());
    }

    @Command(desc = "Gets the time in CST timezone")
    public EmbedMessage time(MessageCreateEvent e) {
        return new EmbedMessage("Time - CST", CommonUtils.getDateFormatted());
    }

    @Command(desc = "Shows you official SureDroid Website")
    public EmbedMessage website(MessageCreateEvent e) {
        return new EmbedMessage("Visit our Official Website!", "Website is https://www.suredroid.com ! Check it out!");
    }

    @Command(desc = "Reminds you with a message",usage = "!remind [message]")
    public void remind(MessageCreateEvent e, String period, String message) {
        Hook.executors.schedule(()-> DUtils.sendMessage("Scheduled Reminder", "Message: " + message,"<@" +e.getMessageAuthor().getIdAsString() + ">",e.getMessageAuthor().getDisplayName(),e.getMessageAuthor().getAvatar(),e.getChannel()),CommonUtils.parsePeriod(period).getEpochSecond(), TimeUnit.SECONDS);
    }

    ArrayList<Long> lockedChannels = new ArrayList<>();
    @Command(desc = "Locks down a channel preventing anyone from sending messages.", serverOnly = true, permissions = {PermissionType.ADMINISTRATOR})
    public void lockdown(MessageCreateEvent e) {
        if(lockedChannels.contains(e.getChannel().getId())){
            new ServerTextChannelUpdater(e.getServerTextChannel().get()).addPermissionOverwrite(e.getServer().get().getEveryoneRole(), e.getChannel().asServerTextChannel().get().getOverwrittenPermissions(e.getServer().get().getEveryoneRole()).toBuilder().setState(PermissionType.SEND_MESSAGES,PermissionState.UNSET).build()).update();
            lockedChannels.remove(e.getChannel().getId());
            DUtils.sendMessage(e,"Disabling the lockdown","Phew, situation resolved.").exceptionally(ExceptionLogger.get());
        } else {
            DUtils.sendMessage(e, "<a:siren:607016485373739039> Activating Lockdown <a:siren:607016485373739039>","The bots are taking control over the channel until the situation is resolved.");
            new ServerTextChannelUpdater(e.getServerTextChannel().get()).addPermissionOverwrite(e.getServer().get().getEveryoneRole(), e.getChannel().asServerTextChannel().get().getOverwrittenPermissions(e.getServer().get().getEveryoneRole()).toBuilder().setState(PermissionType.SEND_MESSAGES,PermissionState.DENIED).build()).update();
            lockedChannels.add(e.getChannel().getId());
        }
    }
}
