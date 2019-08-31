package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@Command(desc="A quick command for creating polls.",usage="[arguments+ (#,*)]")
public class Poll {
    private static String[] numEmoji = {
            "1⃣",
            "2⃣",
            "3⃣",
            "4⃣",
            "5⃣",
            "6⃣",
            "7⃣",
            "8⃣",
            "9⃣"
    };

    public void run(String[] args, MessageCreateEvent e){

        int digit = 0;
        for (String arg : args) {
            if(Character.isDigit(arg.charAt(0))){
                digit = Character.getNumericValue(arg.charAt(0));
            }
        }
        if(digit > 0){
            for(int i = 0; i < digit; i++)
                e.addReactionsToMessage(numEmoji[i]);
        } else {
            e.addReactionsToMessage("\uD83D\uDC4D","\uD83D\uDC4E","\uD83E\uDD37").exceptionally(ExceptionLogger.get());
        }
    }

}
