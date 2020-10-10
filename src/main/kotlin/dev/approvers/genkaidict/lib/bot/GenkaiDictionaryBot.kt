package dev.approvers.genkaidict.lib.bot

import dev.approvers.genkaidict.lib.Resources
import dev.approvers.genkaidict.lib.api.db.GenkaiDictionary
import dev.approvers.genkaidict.lib.api.db.GenkaiDictionaryContent
import io.github.loxygen.discord_framework.commands.CommandResult
import io.github.loxygen.discord_framework.commands.abc.PrefixnessCommand
import io.github.loxygen.discord_framework.commands.annotations.Argument
import io.github.loxygen.discord_framework.commands.annotations.SubCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.lang.Integer.min

class GenkaiDictionaryBot : PrefixnessCommand(
    identify = "dict",
    name = "限界辞書です。",
    description = "限界開発鯖で使われている単語を登録しましょう。\nFirebaseなのでいろいろ生かせます。"
) {

    private val genkaiDictionary = GenkaiDictionary.connect(Resources.firebaseOptionFile)

    @SubCommand(identify = "add", name = "追加", description = "単語を追加します。")
    @Argument(count = 2, denyMore = false)
    fun addWord(args: List<String>, event: MessageReceivedEvent): CommandResult {

        if (genkaiDictionary.doesExist(args[0])) {
            event.channel.sendMessage("その単語はもうあるみたいです").queue()
            return CommandResult.FAILED
        }

        if (args.map { it.length }.sum() > 300) {
            event.channel.sendMessage("文量が多いな、もうちょっと削ってみてください").queue()
            return CommandResult.FAILED
        }

        val example = args.subList(2, args.size).joinToString(" ").ifEmpty { "例文なし" }

        event.channel.typing {
            genkaiDictionary.registry(
                GenkaiDictionaryContent(
                    args[0], args[1], event.member?.nickname ?: event.author.name, example
                )
            )
            event.channel.sendMessage(genkaiDictionary.getMeaningOf(args[0]).toEmbed()).queue()
        }
        event.channel.sendMessage("`${args[0]}`、登録しました").queue()

        return CommandResult.SUCCESS

    }

    @SubCommand(identify = "del", name = "削除", description = "単語を削除します。一応フライさんだけが使えます。")
    @Argument(count = 1)
    fun delWord(args: List<String>, event: MessageReceivedEvent): CommandResult {

        if (!genkaiDictionary.doesExist(args[0])) {
            event.channel.sendMessage("その単語はないみたいです").queue()
            return CommandResult.FAILED
        }

        if (event.author.idLong != 599423913877045258) {
            val mention = event.jda.retrieveUserById(599423913877045258).complete()?.asMention ?: "@フライさん(いない)"
            event.channel.sendMessage("$mention `${args[0]}`を消してほしいらしい、どうですか").queue()
            return CommandResult.FAILED
        }

        event.channel.typing {
            genkaiDictionary.delete(args[0])
        }

        event.channel.sendMessage("`${args[0]}`が消えました").queue()
        return CommandResult.SUCCESS

    }

    @SubCommand(identify = "search", name = "検索", description = "単語を探します。")
    @Argument(count = 1)
    fun searchWord(args: List<String>, event: MessageReceivedEvent): CommandResult {

        if (!genkaiDictionary.doesExist(args[0])) {
            event.channel.sendMessage("ん〜ないみたいです").queue()
            return CommandResult.FAILED
        }

        event.channel.typing {
            event.channel.sendTyping().queue {
                event.channel.sendMessage(genkaiDictionary.getMeaningOf(args[0]).toEmbed()).queue()
            }
        }

        return CommandResult.SUCCESS

    }

    @SubCommand(identify = "get", name = "検索", description = "searchと一緒。")
    @Argument(count = 1)
    fun getWord(args: List<String>, event: MessageReceivedEvent): CommandResult {
        return searchWord(args, event)
    }

    @SubCommand(identify = "list", name = "パラパラ読み", description = "辞書をイッキ見します。")
    @Argument(count = 2, denyLess = false)
    fun listWord(args: List<String>, event: MessageReceivedEvent): CommandResult {

        val count = args.getOrElse(0) { "5" }.toIntOrNull()
        val page = args.getOrElse(1) { "1" }.toIntOrNull()

        if (count == null || page == null) {
            event.channel.sendMessage("引数は数字で渡してくれ〜〜〜").queue()
            return CommandResult.FAILED
        }
        if (count < 1) {
            event.channel.sendMessage("少ない〜〜〜〜").queue()
            return CommandResult.FAILED
        }
        if (count > 5) {
            event.channel.sendMessage("多い！多い！").queue()
            return CommandResult.FAILED
        }

        if (count < 0 || page < 0) {
            event.channel.sendMessage("それはどうすればええんや").queue()
            return CommandResult.FAILED
        }

        var contents: List<GenkaiDictionaryContent> = listOf()
        event.channel.typing {
            contents = genkaiDictionary.getRegisteredWords()
        }

        val contentSublist = contents.subEnoughList(count * (page - 1), count * page)

        if (contentSublist.isEmpty()) {
            event.channel.sendMessage("うーんそんなに多くは登録されてないみたいです").queue()
            return CommandResult.FAILED
        }

        val message = contentSublist.joinToString("") {
            "```【${it.name}】(執筆: ${it.author})\n" +
                    it.description + "\n" +
                    "(${it.example})```"
        } + "\n%d〜%d(%d件中)".format(count * (page - 1) + 1, min(count * page, contents.size), contents.size)

        event.channel.sendMessage(message).queue()
        return CommandResult.SUCCESS

    }

    @SubCommand(identify = "random", name = "ランダム", description = "ランダムに単語を見ます")
    @Argument(count = 1, denyLess = false)
    fun randomWord(args: List<String>, event: MessageReceivedEvent): CommandResult {

        val count = args.getOrElse(0) { "5" }.toIntOrNull()

        if (count == null) {
            event.channel.sendMessage("引数は数字で渡してくれ〜〜〜").queue()
            return CommandResult.FAILED
        }
        if (count < 1) {
            event.channel.sendMessage("少ない〜〜〜〜").queue()
            return CommandResult.FAILED
        }
        if (count > 5) {
            event.channel.sendMessage("多い！多い！").queue()
            return CommandResult.FAILED
        }

        var contents: List<GenkaiDictionaryContent> = listOf()
        event.channel.typing {
            contents = genkaiDictionary.getRegisteredWords()
        }
        val contentSublist = contents.shuffled().subEnoughList(0, count)

        val message = contentSublist.joinToString("") {
            "```【${it.name}】(執筆: ${it.author})\n" +
                    it.description + "\n" +
                    "(${it.example})```"
        }

        event.channel.sendMessage(message).queue()
        return CommandResult.SUCCESS

    }

    private fun GenkaiDictionaryContent.toEmbed(): MessageEmbed {
        return EmbedBuilder()
            .setTitle(this.name)
            .addField("意味", this.description, false)
            .addField("例文", this.example.replace(this.name, "__${this.name}__"), false)
            .addField("登録した人", this.author, false)
            .setColor(Color.GREEN)
            .build()
    }
}

fun MessageChannel.typing(unit: () -> Unit) {
    var operating = true
    this.sendTyping().queue {
        unit()
        operating = false
    }

    while (operating) Thread.sleep(50)
}

fun <E> List<E>.subEnoughList(fromIndex: Int, endIndex: Int): List<E> {

    if (this.size < fromIndex) return listOf()
    if (this.size < endIndex) {
        return this.subList(fromIndex, this.size)
    }
    return this.subList(fromIndex, endIndex)

}