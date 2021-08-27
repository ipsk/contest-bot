package dev.triumphteam.contest.commands.staff

import dev.triumphteam.contest.database.Participants
import dev.triumphteam.contest.func.BotColor
import dev.triumphteam.contest.func.embed
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun GuildMessageReceivedEvent.handleDisband(leader: String) {
    val leaderId = leader.toLongOrNull() ?: return
    val team = transaction {
        Participants.deleteWhere { Participants.leader eq leaderId }
    }

    if (team == 0) {
        message.replyEmbeds(
            embed {
                setColor(BotColor.FAIL.color)
                setDescription("Could not find team for the specified user.")
            }
        ).mentionRepliedUser(false).queue()
        return
    }

    message.replyEmbeds(
        embed {
            setColor(BotColor.SUCCESS.color)
            setDescription("Team disbanded!")
        }
    ).mentionRepliedUser(false).queue()
}