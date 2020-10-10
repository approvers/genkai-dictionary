package dev.approvers.genkaidict.lib

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

object Resources {
    enum class EnvironmentVariable(val envName: String) {
        DiscordBotSettingFile("GENKAI_DICTIONARY_DISCORD_BOT_SETTING_JSON_B64"),
        FirebaseSettingFile("GENKAI_DICTIONARY_FIREBASE_SETTING_JSON_B64");

        fun getEnv(): String {
            return System.getenv(this.envName)
                ?: throw RuntimeException("environment variable for ${this.name} (\"${envName}\") is not set.")
        }
    }

    val settingFile: InputStream
        get() = ByteArrayInputStream(
            Base64.getDecoder().decode(EnvironmentVariable.DiscordBotSettingFile.getEnv())
        )
    val firebaseOptionFile: InputStream
        get() = ByteArrayInputStream(
            Base64.getDecoder().decode(EnvironmentVariable.FirebaseSettingFile.getEnv())
        )
}
