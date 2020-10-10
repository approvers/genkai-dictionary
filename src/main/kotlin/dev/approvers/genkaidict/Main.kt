package dev.approvers.genkaidict

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.approvers.genkaidict.lib.Resources
import dev.approvers.genkaidict.lib.bot.GenkaiDictionaryBot
import io.github.loxygen.discord_framework.client.Client
import io.github.loxygen.discord_framework.client.ClientSettingInfo

fun main() {

    val mapper = jacksonObjectMapper()
    val clientSettingInfo: ClientSettingInfo = mapper.readValue(Resources.settingFile)

    val client = Client(clientSettingInfo)
    client.addMessageCommand(GenkaiDictionaryBot())
    client.launch()

}