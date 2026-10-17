package es.aproximado.app

import android.content.Context
import androidx.room.*
import es.aproximado.core.Question

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String, val category: String, val text_es: String,
    val answer: Double, val unit: String?, val min_plausible: Double,
    val max_plausible: Double, val scale: String, val difficulty: Int,
    val as_of_year: Int?, val source_hint: String
) {
    fun model() = Question(id,category,text_es,answer,unit,min_plausible,max_plausible,scale,difficulty,as_of_year,source_hint)
}
@Dao interface QuestionDao { @Query("SELECT * FROM questions ORDER BY id") suspend fun all(): List<QuestionEntity> }
@Database(entities = [QuestionEntity::class],version = 1,exportSchema = true)
abstract class QuestionsDb : RoomDatabase() {
    abstract fun questions(): QuestionDao
    companion object {
        @Volatile private var instance: QuestionsDb? = null
        fun get(context: Context): QuestionsDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,QuestionsDb::class.java,"aproximado-questions-v1.db")
                .createFromAsset("questions.db").build().also { instance = it }
        }
    }
}
