package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.CommonUtils;
import com.suredroid.discord.DUtils;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.concurrent.ThreadLocalRandom;

@Command(desc="Picks a number between your specified values.", usage="(num1) (num2)")
public class RandNum {
    public void run(MessageCreateEvent e, String Snum1, String Snum2) {
        if (!(CommonUtils.isInteger(Snum1) && CommonUtils.isInteger(Snum2))) {
            DUtils.sendMessage(e, "Invalid Input", "This value is not an integer. Please provide a valid integer.");
            return;
        }
        int num1 = Integer.parseInt(Snum1), num2 = Integer.parseInt(Snum2);
        int max, min;
        if (num1 > num2) {
            max = num1;
            min = num2;
        } else {
            min = num1;
            max = num2;
        }
        DUtils.sendMessage(e, "Random Number", "Your random number is " + ThreadLocalRandom.current().nextInt(min, max + 1) + "!");
    }
}
