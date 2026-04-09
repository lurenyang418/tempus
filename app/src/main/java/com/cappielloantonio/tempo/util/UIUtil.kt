package com.cappielloantonio.tempo.util

import android.content.Context
import android.graphics.drawable.InsetDrawable
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.cappielloantonio.tempo.App.Companion.getContext
import com.cappielloantonio.tempo.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.String
import java.text.SimpleDateFormat
import java.util.AbstractMap
import java.util.Date
import java.util.Locale
import java.util.Map
import kotlin.Int
import kotlin.intArrayOf

object UIUtil {
    @JvmStatic
    fun getSpanCount(itemCount: Int, maxSpan: Int): Int {
        val itemSize = if (itemCount == 0) 1 else itemCount

        if (itemSize / maxSpan > 0) {
            return maxSpan
        } else {
            return itemSize % maxSpan
        }
    }

    @JvmStatic
    fun getDividerItemDecoration(context: Context): DividerItemDecoration {
        val ATTRS = intArrayOf(android.R.attr.listDivider)

        val a = context.obtainStyledAttributes(ATTRS)
        val divider = a.getDrawable(0)
        val insetDivider = InsetDrawable(divider, 42, 0, 42, 42)
        a.recycle()

        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(insetDivider)

        return itemDecoration
    }

    private fun getLocalesFromResources(context: Context): LocaleListCompat {
        val tagsList: MutableList<String?> = ArrayList<String?>()

        val xpp: XmlPullParser = context.getResources().getXml(R.xml.locale_config)

        try {
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                val tagName = xpp.getName()

                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if ("locale" == tagName && xpp.getAttributeCount() > 0 && xpp.getAttributeName(0) == "name") {
                        tagsList.add(xpp.getAttributeValue(0) as String?)
                    }
                }

                xpp.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return LocaleListCompat.forLanguageTags(String.join(",", tagsList))
    }

    @JvmStatic
    fun getLangPreferenceDropdownEntries(context: Context): MutableMap<kotlin.String?, kotlin.String?> {
        val localeList = getLocalesFromResources(context)

        val localeArrayList: MutableList<MutableMap.MutableEntry<kotlin.String?, kotlin.String?>> =
            ArrayList<MutableMap.MutableEntry<kotlin.String?, kotlin.String?>>()

        val systemDefaultLabel = getContext()!!.getString(R.string.settings_system_language)
        val systemDefaultValue = "default"

        for (i in 0..<localeList.size()) {
            val locale = localeList.get(i)
            if (locale != null) {
                localeArrayList.add(
                    AbstractMap.SimpleEntry<kotlin.String?, kotlin.String?>(
                        Util.toPascalCase(locale.getDisplayName()),
                        locale.toLanguageTag()
                    )
                )
            }
        }

        localeArrayList.sortWith(Map.Entry.comparingByKey<kotlin.String?, kotlin.String?>(String.CASE_INSENSITIVE_ORDER))

        val orderedMap = LinkedHashMap<kotlin.String?, kotlin.String?>()
        orderedMap.put(systemDefaultLabel, systemDefaultValue)
        for (entry in localeArrayList) {
            orderedMap.put(entry.key, entry.value)
        }

        return orderedMap
    }

    @JvmStatic
    fun getReadableDate(date: Date?): kotlin.String {
        if (date == null) {
            return getContext()!!.getString(R.string.share_no_expiration)
        }
        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}
