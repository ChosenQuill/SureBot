package com.suredroid.discord.Form;

import lombok.Data;
import org.javacord.api.entity.user.User;

public @Data class Results  {
    private final Question[] questions;
    private final String[] answers;
    private final boolean completed;
    private final long userId;
}
