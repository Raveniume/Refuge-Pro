package com.refuge.next.data

import org.json.JSONArray
import org.json.JSONObject

/** Explicitly unwrap JSON nulls; optString otherwise turns them into the URL "null". */
internal fun JSONObject.terminalMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
    fun unwrap(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.terminalMap()
        is JSONArray -> (0 until value.length()).map { unwrap(value.opt(it)) }
        else -> value
    }
    unwrap(opt(key))
}
