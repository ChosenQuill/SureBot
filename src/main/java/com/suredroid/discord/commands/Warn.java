package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.Main;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;

@Command(desc = "Warning system set up for team members for inactivity.",usage = "!warn (list,user)")
public class Warn {
    private static HashMap<Long,Integer> list = new HashMap<>();

    public void run(MessageCreateEvent e, String command){
        if(command.equalsIgnoreCase("list")){
            if(DUtils.hasRole(e, Main.roles.get("director")))
            for(int i = 0; i < list.size(); i++){

            }
        }
    }
}