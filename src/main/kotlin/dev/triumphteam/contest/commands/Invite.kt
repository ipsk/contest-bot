package dev.triumphteam.contest.commands

import dev.triumphteam.bukkit.feature.feature
import dev.triumphteam.contest.config.Config
import dev.triumphteam.contest.config.Settings
import dev.triumphteam.contest.database.Invites
import dev.triumphteam.contest.database.Participants
import dev.triumphteam.contest.func.BotColor
import dev.triumphteam.contest.func.embed
import dev.triumphteam.contest.func.queueReply
import dev.triumphteam.jda.JdaApplication
import dev.triumphteam.contest.event.on
import dev.triumphteam.contest.func.inBotChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalTime

/**
 * The current commands are temporary, JDA's way is really annoying
 */
fun JdaApplication.invite() {
    val config = feature(Config)

    // Handling commands
    on<SlashCommandEvent> {
        if (name != "invite") return@on
        deferReply(true).queue()

        if (!inBotChannel(config)) return@on

        val partner = getOption("partner")?.asMember ?: run {
            // Should never happen
            queueReply("Could not find member option.")
            return@on
        }

        val leader = member ?: run {
            // Should never happen
            queueReply("Invalid member.")
            return@on
        }

        if (partner.idLong == leader.idLong) {
            queueReply(
                embed {
                    setColor(BotColor.FAIL.color)
                    setDescription("You can't invite yourself!")
                }
            )
            return@on
        }

        if (partner.user.isBot) {
            queueReply(
                embed {
                    setColor(BotColor.FAIL.color)
                    setDescription("You cannot invite bots to your team!")
                }
            )
            return@on
        }

        val team = transaction {
            Participants.select { Participants.leader eq leader.idLong }.firstOrNull()
        }

        if (team == null) {
            queueReply(
                embed {
                    setColor(BotColor.FAIL.color)
                    setDescription("You're not participating in the contest or not the leader of a team!")
                }
            )
            return@on
        }

        if (team[Participants.partner] != null) {
            queueReply(
                embed {
                    setColor(BotColor.FAIL.color)
                    setDescription("Your team is already full!")
                }
            )
            return@on
        }

        val logChannel = guild?.getTextChannelById(config[Settings.CHANNELS].contestLog)

        if (!invitePartner(config, partner, team[Participants.id], leader, logChannel)) return@on

        queueReply(
            embed {
                setColor(BotColor.SUCCESS.color)
                setTitle("Invite sent!")
                setDescription(
                    """
                        An invite was sent to ${partner.asMention}.
                        They need to do `/accept <@inviter>` to accept.
                    """.trimIndent()
                )
            }
        )
    }
}

fun SlashCommandEvent.invitePartner(
    config: Config,
    partnerMember: Member,
    teamId: EntityID<Int>,
    leader: Member,
    logChannel: TextChannel?
): Boolean {
    return transaction {

        val partnerResult =
            Participants.select { Participants.leader eq partnerMember.idLong or (Participants.partner eq partnerMember.idLong) }
                .firstOrNull()
        if (partnerResult != null) {
            queueReply(
                embed {
                    setColor(BotColor.FAIL.color)
                    setDescription(
                        """
                            The partner you're trying to invite is already in a team.
                            Please contact staff if you think this is a mistake.
                        """.trimIndent()
                    )
                }
            )
            return@transaction false
        }

        val invite = Invites.select {
            Invites.team eq teamId and (Invites.partner eq partnerMember.idLong)
        }.firstOrNull()

        if (invite != null) {
            queueReply(
                embed {
                    setColor(BotColor.FAIL.color)
                    setDescription("${partnerMember.asMention} already has a pending invite from you!")
                }
            )
            return@transaction false
        }

        Invites.insert {
            it[team] = teamId
            it[partner] = partnerMember.idLong
        }

        logChannel?.sendMessageEmbeds(
            embed {
                setColor(BotColor.INFO.color)
                setTitle("New invite added.")
                setDescription("${partnerMember.asMention} was invited to join ${leader.asMention}'s team.")
                setTimestamp(Instant.now())
            }
        )?.queue()

        guild?.getTextChannelById(config[Settings.CHANNELS].botCommands)
            ?.sendMessage(partnerMember.asMention)
            ?.setEmbeds(
                embed {
                    setColor(BotColor.SUCCESS.color)
                    setDescription(
                        """
                            ${member?.asMention} has invited you to be part of their team!
                            Do `/accept @${member?.user?.asTag}` to accept.
                        """.trimIndent()
                    )
                }
            )
            ?.queue()

        return@transaction true
    }
}