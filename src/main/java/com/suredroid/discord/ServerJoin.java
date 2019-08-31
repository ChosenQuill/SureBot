package com.suredroid.discord;

import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;

public class ServerJoin implements ServerMemberJoinListener {

    private String welcomeMsg;



    public ServerJoin(){
        CommonUtils.readInternalFile("/files/welcome.txt").ifPresent(s -> welcomeMsg = s);
    }
    @Override
    public void onServerMemberJoin(ServerMemberJoinEvent event) {
        if(event.getServer().getId() == 440681682799034408L) {
            event.getUser().sendMessage(welcomeMsg);
        }
        else if(event.getServer().getId() == 521436239174303761L){
            Main.whiteList.check(event.getServer(),event.getUser());
        }
    }

}
