package xd.arkosammy.compsmpdiscordbot

import dev.kord.common.annotation.KordPreview
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.*
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory
import xd.arkosammy.compsmpdiscordbot.config.ConfigSettings
import xd.arkosammy.compsmpdiscordbot.config.SettingGroups
import xd.arkosammy.monkeyconfig.managers.ConfigManager
import xd.arkosammy.monkeyconfig.managers.TomlConfigManager

object CompSMPDiscordBot : DedicatedServerModInitializer {

    const val MOD_ID: String = "compsmpdiscordbot"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)
    val CONFIG_MANAGER: ConfigManager =
        TomlConfigManager(MOD_ID, SettingGroups.settingGroups, ConfigSettings.settingBuilders)
    private lateinit var discordJob: Job

    override fun onInitializeServer() {

        ServerLifecycleEvents.SERVER_STARTING.register(::onServerStarting)
        ServerLifecycleEvents.SERVER_STOPPING.register(::onServerStopping)

        // Start the Discord bot in a coroutine
        runBlocking {
            DiscordBot.start()
        }

    }

    @OptIn(PrivilegedIntent::class)
    private fun onServerStarting(server: MinecraftServer) {
        // Log in the bot with required intents in a coroutine
        discordJob = CoroutineScope(Dispatchers.IO).launch {
            DiscordBot.KORD.login {
                this.intents = Intents(Intent.Guilds, Intent.GuildMessages, Intent.GuildEmojis, Intent.MessageContent, Intent.DirectMessagesReactions, Intent.GuildMessageReactions)
            }
        }
    }

    private fun onServerStopping(server: MinecraftServer) {
        // Log out the bot and cancel the discordJob
        runBlocking {
            DiscordBot.KORD.logout()
            discordJob.cancelAndJoin()
        }
    }
}