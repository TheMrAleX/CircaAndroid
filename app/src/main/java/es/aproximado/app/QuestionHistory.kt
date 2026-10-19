package es.aproximado.app

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import es.aproximado.core.Selection
import org.json.JSONArray

/** One persisted chronological history shared by offline games and LAN hosts. No drafts or answers. */
@SuppressLint("ApplySharedPref") // Checkpoint only on a newly shown question or explicit clear, never on slider/network frames.
object QuestionHistory {
    private fun prefs(context: Context)=context.applicationContext.getSharedPreferences("aproximado-history",Context.MODE_PRIVATE)
    @Synchronized fun read(context: Context): List<String> = runCatching {
        val a=JSONArray(prefs(context).getString("seen","[]"))
        (0 until a.length()).map { a.getString(it) }.filter { it.isNotBlank() }.takeLast(10000)
    }.getOrDefault(emptyList())
    /** Import old saves once without moving their older IDs ahead of an already established history. */
    @Synchronized fun importLegacy(context: Context,old: List<String>): List<String> {
        val current=read(context)
        if(prefs(context).getBoolean("imported",false)) return current
        val merged=Selection.remember(old,current)
        prefs(context).edit(commit=true) { putString("seen",JSONArray(merged).toString());putBoolean("imported",true) }
        return merged
    }
    @Synchronized fun remember(context: Context,shown: List<String>): List<String> {
        val next=Selection.remember(read(context),shown)
        prefs(context).edit(commit=true) { putString("seen",JSONArray(next).toString()) }
        return next
    }
    @Synchronized fun clear(context: Context) { prefs(context).edit(commit=true) { putString("seen","[]");putBoolean("imported",true) } }
}
