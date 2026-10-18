package es.aproximado.app

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.aproximado.core.Question

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String, val category: String, val text_es: String,
    val answer: Double, val unit: String?, val min_plausible: Double,
    val max_plausible: Double, val scale: String, val difficulty: Int,
    val as_of_year: Int?, val source_hint: String,
    @ColumnInfo(defaultValue = "0") val decimal_places: Int
) {
    fun model() = Question(id,category,text_es,answer,unit,min_plausible,max_plausible,scale,difficulty,as_of_year,source_hint,decimal_places)
}
@Dao interface QuestionDao { @Query("SELECT * FROM questions ORDER BY id") suspend fun all(): List<QuestionEntity> }
@Database(entities = [QuestionEntity::class],version = 2,exportSchema = true)
abstract class QuestionsDb : RoomDatabase() {
    abstract fun questions(): QuestionDao
    companion object {
        fun precisionMigration(context: Context) = object : Migration(1,2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                context.assets.open("migration_1_2.sql").bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() }.forEach { db.execSQL(it) }
                }
            }
        }
        @Volatile private var instance: QuestionsDb? = null
        fun get(context: Context): QuestionsDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,QuestionsDb::class.java,"aproximado-questions-v1.db")
                .createFromAsset("questions.db").addMigrations(precisionMigration(context.applicationContext)).build().also { instance = it }
        }
    }
}
