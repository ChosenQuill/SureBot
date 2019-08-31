package com.suredroid.discord;

import com.suredroid.discord.Annotations.Create;
import org.javacord.api.entity.message.Message;

@Create
public class PinSystem {
    public PinSystem(){
        DUtils.getApi().getServerById(521436239174303761L).ifPresent(server->{
            server.addReactionAddListener(event -> {
                if(event.getEmoji().equalsEmoji("\uD83D\uDCCC") && server.getRoles(event.getUser()).stream().anyMatch(role-> role.getId()==574637239909220352L)){
                    event.getMessage().ifPresent(Message::pin);
                }
            });
            server.addReactionRemoveListener(event->{
                if(event.getEmoji().equalsEmoji("\uD83D\uDCCC") && server.getRoles(event.getUser()).stream().anyMatch(role-> role.getId()==574637239909220352L)){
                    event.getMessage().ifPresent(Message::unpin);
                }
            });
        });
    }
}


