package server;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import server.commands.CreateSerialKeyCommand;

public class DiscordBot extends ListenerAdapter {
    public static void init(){
        JDA jda = JDABuilder.createDefault("MTEzODIyNjk5ODcxMjQ3OTgwNQ.GC__y7.b2QwjHo1sxGYHsqisvw8baARMFJirGsXDQ2UCU").build();
        jda.addEventListener(new DiscordBot(), new CreateSerialKeyCommand());
        jda.getPresence().setActivity(Activity.playing("with myself"));
        jda.upsertCommand("genkey", "sex");
        jda.updateCommands();
        Runtime.getRuntime().addShutdownHook(new Thread(jda::shutdownNow));
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(Commands.slash("genkey", "sex")).queue();
    }
}