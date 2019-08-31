package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.Error;
import com.suredroid.discord.Main;
import org.javacord.api.event.message.MessageCreateEvent;

@Command(desc = "Deletes messages in an order.", usage="!purge #")
public class Purge {
    public void run(MessageCreateEvent e, String numString){
        if(!Main.isChosenQuill(e)){
            Error.NoPermission.send(e);
            return;
        }
        int num;
        try {
            num = Integer.parseInt(numString);
        } catch (NumberFormatException e1){
            Error.NotANumber.send(e);
            return;
        }
        int realNum = num + 1;
        DUtils.confirm(e,"delete " + num + " message(s)").thenAccept(aBoolean -> {
            if(aBoolean){
                e.getChannel().getMessages(realNum).whenComplete((messages, throwable) -> {
                    if(messages!=null){
                        messages.deleteAll();
                    }else {
                        throwable.printStackTrace();
                    }
                });
            } else {
                Error.FriendlyCancel.send(e);
            }
        });


    }
}
