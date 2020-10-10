package dev.approvers.genkaidict.lib.api.db

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.io.InputStream

class GenkaiDictionary private constructor(options: FirebaseOptions) {

    val firestore: Firestore
    val dictionaryDoc: CollectionReference

    init {
        FirebaseApp.initializeApp(options)
        firestore = FirestoreClient.getFirestore()

        dictionaryDoc = firestore.collection("genkai-dict")
    }

    companion object {

        private var instance: GenkaiDictionary? = null

        @Synchronized
        fun connect(setting: InputStream): GenkaiDictionary {

            if (instance != null) return instance as GenkaiDictionary

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(setting))
                .build()

            instance = GenkaiDictionary(options)
            return instance as GenkaiDictionary

        }
    }

    fun registry(content: GenkaiDictionaryContent): Timestamp {
        val ref = dictionaryDoc.document(content.name)
        val data = HashMap<String, Any?>()
        data["description"] = content.description
        data["author"] = content.author
        data["example"] = content.example
        val result = ref.set(data)
        return result.get().updateTime
    }

    fun delete(word: String): Timestamp {
        val ref = dictionaryDoc.document(word)
        val result = ref.delete()
        return result.get().updateTime
    }

    fun getRegisteredWords(): List<GenkaiDictionaryContent> {
        return dictionaryDoc.listDocuments()
            .map { it.get().get() }
            .map {
                generateDictionaryContent(it)
            }
    }

    fun getMeaningOf(word: String): GenkaiDictionaryContent = generateDictionaryContent(fetchDocument(word))

    fun doesExist(word: String): Boolean = fetchDocument(word).exists()

    private fun fetchDocument(word: String): DocumentSnapshot = dictionaryDoc.document(word).get().get()

    private fun generateDictionaryContent(snap: DocumentSnapshot): GenkaiDictionaryContent {
        return GenkaiDictionaryContent(
            snap.id,
            snap["description"] as String,
            (snap["author"] ?: "登録者不明") as String,
            (snap["example"] ?: "例文なし") as String
        )
    }

}