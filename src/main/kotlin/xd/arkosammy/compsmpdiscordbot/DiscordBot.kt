package xd.arkosammy.compsmpdiscordbot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Member
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import xd.arkosammy.compsmpdiscordbot.config.ConfigSettings
import xd.arkosammy.monkeyconfig.managers.getSettingValue
import xd.arkosammy.monkeyconfig.settings.NumberSetting
import xd.arkosammy.monkeyconfig.settings.StringSetting

object DiscordBot {

    lateinit var KORD: Kord

     suspend fun start() {

         println("e")
         val botToken: String =
             CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<String, StringSetting>(ConfigSettings.BOT_TOKEN.settingLocation)
         val guildId: Long =
             CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.GUILD_ID.settingLocation)
         // add event listeners before calling login

         val guildSnowFlake: Snowflake = Snowflake(guildId)
         val kord: Kord = Kord(botToken)
         KORD = kord

         kord.on<MessageCreateEvent> { // runs every time a message is created that our bot can read

             // ignore other bots, even ourselves. We only serve humans here!
             if (message.author?.isBot != false) return@on

             // check if our command is being invoked
             if (message.content != "!ping") return@on

             // all clear, give them the pong!
             message.channel.createMessage("pong!")
         }
         kord.on<ReactionAddEvent> {

             println("Testing reacting add event")

             val reactionEmoji: ReactionEmoji = this.emoji
             if (reactionEmoji !is ReactionEmoji.Custom) {
                 return@on
             }

             // Check if the reactor is an admin
             val reactor: Member = this.user.asMember(guildSnowFlake)
             val roles: Set<Snowflake> = reactor.roleIds
             val adminRoleId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.ADMIN_ROLE_ID.settingLocation)
             if (!roles.any { roleId -> roleId.value.toLong() == adminRoleId }) {
                 return@on
             }

             // Check if the message is in the applications channel
             val applicationChannelId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.APPROVAL_CHANNEL_ID.settingLocation)
             if (this.message.channelId.value.toLong() != applicationChannelId) {
                 return@on
             }

             // Check if the reacted emoji is the approved emoji
             val approvedEmojiId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.APPROVAL_EMOJI_ID.settingLocation)
             if (reactionEmoji.id.value.toLong() != approvedEmojiId) {
                 return@on
             }

             val applicant: Member = this.messageAuthor?.asMember(guildSnowFlake) ?: return@on
             val approvalRoleId: Long = CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.APPROVAL_ROLE_ID.settingLocation)

                 CompSMPDiscordBot.LOGGER.info("Giving approval role to user: ${applicant.nickname}")
             applicant.addRole(Snowflake(approvalRoleId))
         }

         kord.on<ReactionRemoveEvent> {
             println("Testing reaction remove event")

             val reactionEmoji: ReactionEmoji = this.emoji
             if (reactionEmoji !is ReactionEmoji.Custom) {
                 return@on
             }

             val reactor: Member = this.user.asMember(guildSnowFlake)
             val roles: Set<Snowflake> = reactor.roleIds
             val adminRoleId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.ADMIN_ROLE_ID.settingLocation)
             if (!roles.any { roleId -> roleId.value.toLong() == adminRoleId }) {
                 return@on
             }

             val applicationChannelId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.APPROVAL_CHANNEL_ID.settingLocation)
             if (this.message.channelId.value.toLong() != applicationChannelId) {
                 return@on
             }

             val approvedEmojiId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.APPROVAL_EMOJI_ID.settingLocation)
             if (reactionEmoji.id.value.toLong() != approvedEmojiId) {
                 return@on
             }

             val applicant = this.message.asMessage().author?.asMember(guildSnowFlake) ?: return@on
             val applicantRoles: Set<Snowflake> = applicant.roleIds
             val approvalRoleId: Long =
                 CompSMPDiscordBot.CONFIG_MANAGER.getSettingValue<Long, NumberSetting<Long>>(ConfigSettings.APPROVAL_ROLE_ID.settingLocation)
             val hasApprovedRole: Boolean = applicantRoles.any { role -> role.value.toLong() == approvalRoleId }

             if (!hasApprovedRole) return@on

             var hasApprovedRoleByOtherAdmin = false

             runBlocking {
                 this@on.message.getReactors(ReactionEmoji.Custom(Snowflake(approvedEmojiId), "Approved", false))
                     .filter { user ->
                         user.id != this@on.userId
                     }.collect { user ->
                         val member = user.asMember(guildSnowFlake)
                         if (member.roleIds.any { roleId -> roleId.value.toLong() == adminRoleId }) {
                             hasApprovedRoleByOtherAdmin = true
                             return@collect
                         }
                     }
             }

             if (!hasApprovedRoleByOtherAdmin) {
                 applicant.removeRole(Snowflake(approvalRoleId))
             }
         }
     }
}