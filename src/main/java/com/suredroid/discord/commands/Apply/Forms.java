package com.suredroid.discord.commands.Apply;

import com.suredroid.discord.DiscordBot;
import com.suredroid.discord.Form.Form;
import com.suredroid.discord.Form.Question;

import java.util.HashMap;

public class Forms{
    static HashMap<String, Form> list = new HashMap<>();
    static {
        Question[] base = {
                new Question("Please state your full name."),
                new Question("What is your email address.","This is required for documentation purposes."),
                new Question("What is your timezone?","Please use https://www.timeanddate.com/time/zone/ to get your timezone and paste it here."),
                new Question("Why do you want to join SureDroid? What makes it different from others?"),
                new Question("Why do you want to work on this project/team?"),
                new Question("Are you currently working on any other projects or participating in other teams?","And if so, will it take up your time?"),
                new Question("How much time will you have available to dedicate to projects on this team?")
        };

        Question[] ending = {
                new Question("What is your past experience in this occupation?"),
                new Question("Please provide some examples of your work."),
                new Question("Do you have any ideas that you may want to see in the project/team?"),
                new Question("Do you have any questions or concerns before you get started?")
        };

        Question[] artist = {
                new Question("What is your preferred art style?"),
                new Question("Are you willing to try different styles of art?"),
        };
        list.put("artist",new Form(base,artist,ending));
        Question[] writer = {
                new Question("What genres/styles do you write in?"),
                new Question("Are you willing to try different genres and styles?"),
        };
        list.put("writer", new Form(base, writer, ending));
        Question[] developer = {
                new Question("What languages can you program in?"),
                new Question("Are you willing to try different and new programming languages?"),
        };
        list.put("developer",new Form(base,developer,ending));
        Question[] musicproducer = {
                new Question("What music genres do you create music in?"),
                new Question("Are you willing to try different genres/styles?"),
                new Question("What DAW (Digital Audio Workstation) do you use?")
        };
        list.put("music-producer",new Form(base,musicproducer,ending));
        Question[] gamedesigner = {
                new Question("Share an effective method you have used to generate new game design ideas."),
                new Question("Describe an effective mission, challenge, or puzzle you devised for a game. How about one that was not as effective?"),
                new Question("How do you plan of effectively communicating your ideas for the game?"),
                new Question("Are you willing to do tasks outside your comfort zone?","IE. Design difficult game elements"),
        };
        list.put("game-designer", new Form(base,gamedesigner,ending));
        Question[] uidesigner = {
                new Question("What’s your understanding of UI design?","Definition of UI design and principles."),
                new Question("What elements have you designed before?"),
                new Question("Are you willing to try to design challenging and new elements?"),
        };
        list.put("ui-designer", new Form(base,uidesigner,ending));

        Question[] videoeditor = {
                new Question("What can you do while editing?","Ie. Transitions, subtitles, chroma-keying, etc. (If you do vfx, list your skills here.)"),
                new Question("Are you willing to try to employ new editing techniques?"),
                new Question("What video editing software do you use?"),
        };
        list.put("video-editor",new Form(base,videoeditor,ending));

        Question[] staff = {
                new Question("How do you plan on contributing to our community?"),
                new Question("What would you change about our community to make it better?"),
                new Question("How has your background prepared you for this role?"),
                new Question("How well do you work in a team environment?"),
                new Question("Tell me about a time you had a conflict with a co-worker. How did you handle it?"),
        };

        Question[] moderator = {
                new Question("If a moderator was abusing staff commands, and no other staff are online, what would you do?"),
                new Question("If a friend posted links to his youtube page, what would you do?"),
                new Question("If a community member was talking about potentially harming him/herself, what would you do?"),
                new Question("Are you willing to do tasks outside your comfort zone?","IE. Dealing with pornographic spam, death threats, etc"),
        };

        list.put("moderator",new Form(base,staff,moderator,ending));
        Question[] coordinator = {
                new Question("How do you plan on contributing to our community?"),
                new Question("What would you change about our community to make it better?"),
                new Question("How well do you work in a team environment?"),
                new Question("Do you work well with others?"),
                new Question("Imagine you have to schedule and plan a conference from start to finish. What things would you consider and what steps would you follow?"),
                new Question("What kind of technology tools/software should a program coordinator be familiar with?","How proficient are you in using them?"),
                new Question("Do you have experience in dealing with diversity?", "How do you increase diversity awareness?"),
                new Question("What's your experience with fundraising?"),
                new Question("How do you manage your time and prioritize tasks?","This role involves managing multiple objectives simultaneously.")
        };
        list.put("coordinator",new Form(base,staff,coordinator,ending));

        if(DiscordBot.debug){
            list.put("test", new Form(new Question[]{new Question("Question 1"),new Question("Question 2")}));
        }
    }
}