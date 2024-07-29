package xd.arkosammy.compsmpdiscordbot

import net.fabricmc.api.DedicatedServerModInitializer
import org.slf4j.LoggerFactory
import xd.arkosammy.compsmpdiscordbot.config.ConfigSettings
import xd.arkosammy.compsmpdiscordbot.config.SettingGroups
import xd.arkosammy.monkeyconfig.managers.ConfigManager
import xd.arkosammy.monkeyconfig.managers.TomlConfigManager
import xd.arkosammy.monkeyconfig.registrars.DefaultConfigRegistrar

object CompSMPDiscordBot : DedicatedServerModInitializer {

	const val MOD_ID: String = "compsmpdiscordbot"
    private val LOGGER = LoggerFactory.getLogger(MOD_ID)
	val CONFIG_MANAGER: ConfigManager = TomlConfigManager(MOD_ID, SettingGroups.settingGroups, ConfigSettings.settingBuilders)

	override fun onInitializeServer() {
		DefaultConfigRegistrar.registerConfigManager(CONFIG_MANAGER)
	}
}