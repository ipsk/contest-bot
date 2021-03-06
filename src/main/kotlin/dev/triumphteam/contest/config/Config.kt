package dev.triumphteam.contest.config

import dev.triumphteam.bukkit.configuration.BaseConfig
import dev.triumphteam.bukkit.feature.ApplicationFeature
import dev.triumphteam.bukkit.feature.attribute.key
import dev.triumphteam.jda.JdaApplication
import me.mattstudios.config.SettingsHolder
import me.mattstudios.config.annotations.Name
import me.mattstudios.config.properties.Property
import java.io.File
import java.nio.file.Path

class Config(dataFolder: File) : BaseConfig(Path.of(dataFolder.absolutePath, "config.yml"), Settings::class.java) {

    companion object Feature : ApplicationFeature<JdaApplication, Config, Config> {

        override val key = key<Config>("config")

        override fun install(application: JdaApplication, configure: Config.() -> Unit): Config {
            return Config(application.applicationFolder)
        }
    }
}

object Settings : SettingsHolder {

    @me.mattstudios.config.annotations.Path("votes")
    val VOTES = Property.create(Votes())

    @me.mattstudios.config.annotations.Path("channels")
    val CHANNELS = Property.create(Channels())

    @me.mattstudios.config.annotations.Path("roles")
    val ROLES = Property.create(Roles())

}

data class Channels(
    @Name("bot-commands")
    var botCommands: String = "",
    @Name("event-info")
    var eventInfo: String = "",
    @Name("contest-log")
    var contestLog: String = "",
)

data class Roles(
    @Name("staff-role")
    var staffRole: String = "",
    var manager: String = "",
    var trusted: String = "",
    var admin: String = "",
    var participant: String = "",
)

data class Votes(
    @Name("votes-started")
    var votesStarted: Boolean = false,
    @Name("votes-message")
    var votesMessage: String = "",
    @Name("votes-channel")
    var votesChannel: String = "",
    var closed: Boolean = false,
)