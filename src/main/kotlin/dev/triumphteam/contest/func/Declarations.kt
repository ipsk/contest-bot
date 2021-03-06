package dev.triumphteam.contest.func

import dev.triumphteam.contest.config.Config
import dev.triumphteam.contest.config.Settings
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.Url
import io.ktor.http.takeFrom
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

fun tokenFromFlag(args: Array<String>): String {
    val cli = DefaultParser().parse(
        Options().apply {
            addOption(Option.builder("t").hasArg().argName("token").required().build())
        },
        args
    )

    return cli.getOptionValue("t")
}

fun embed(builder: EmbedBuilder.() -> Unit): MessageEmbed {
    return EmbedBuilder().apply(builder).build()
}

fun String.plural(list: List<*>) = if (list.size != 1) plus('s') else this

suspend inline fun <reified T> HttpClient.getOrNull(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T? {
    return try {
        get {
            url.takeFrom(urlString)
            block()
        }
    } catch (exception: ClientRequestException) {
        null
    }
}

fun SlashCommandEvent.queueReply(embed: MessageEmbed) {
    hook.sendMessageEmbeds(embed).setEphemeral(true).queue()
}

fun SlashCommandEvent.queueReply(message: String) {
    hook.sendMessage(message).setEphemeral(true).queue()
}

fun SlashCommandEvent.inBotChannel(config: Config): Boolean {
    val staffRole = guild?.getRoleById(config[Settings.ROLES].staffRole)
    if (member?.roles?.contains(staffRole) == true) return true

    val botChannel = guild?.getTextChannelById(config[Settings.CHANNELS].botCommands)
    if (channel.idLong != botChannel?.idLong) {
        queueReply(
            embed {
                setColor(BotColor.FAIL.color)
                setDescription("Please move to ${botChannel?.asMention} to run the command.")
            }
        )
        return false
    }

    return true
}

fun Member.isManager(config: Config) = !roles.none {
    it.id == config[Settings.ROLES].manager
            || it.id == config[Settings.ROLES].admin
            || it.id == config[Settings.ROLES].trusted
}