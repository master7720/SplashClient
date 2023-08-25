package server.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import server.Data;

public class CreateSerialKeyCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("genkey")) {
            if (event.getInteraction().getChannel().getIdLong() != 1139984399849496598L){
                event.reply("Wrong channel. #serialkeys").queue();
            }else if(!event.getInteraction().getMember().getRoles().contains(event.getGuild().getRoleById(1139982726116692030L))){
                event.reply("Not authorzed!").queue();
            } else event.reply(Data.saveNewSerialKey()).queue();
        }
    }

}