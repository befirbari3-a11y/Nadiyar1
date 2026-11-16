package com.example.util

sealed interface RichSegment {
    data class Plain(val text: String) : RichSegment
    data class Styled(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val highlightHex: String = ""
    ) : RichSegment
    data class Fraction(val num: String, val den: String) : RichSegment
    data class Mixed(val whole: String, val num: String, val den: String) : RichSegment
    data class Root(val index: String, val radicand: String) : RichSegment
    data class Power(val base: String, val exp: String) : RichSegment
    data class Subscript(val base: String, val sub: String) : RichSegment
    data class RastaLine(val label: String) : RichSegment
    data class Overline(val label: String) : RichSegment
}

object MathMarkup {

    private val tokenRegex = Regex(
        """\[(frac|mix|sqrt|root|sup|sub|rasta|over|b|i|u|hi):([^\]]+)\]"""
    )

    fun parse(input: String): List<RichSegment> {
        if (input.isEmpty()) return emptyList()
        val result = mutableListOf<RichSegment>()
        var lastIndex = 0
        for (match in tokenRegex.findAll(input)) {
            if (match.range.first > lastIndex) {
                result.add(RichSegment.Plain(input.substring(lastIndex, match.range.first)))
            }
            val kind = match.groupValues[1]
            val payload = match.groupValues[2]
            val parts = payload.split("|")
            val segment = when (kind) {
                "frac" -> RichSegment.Fraction(parts.getOrElse(0) { "1" }, parts.getOrElse(1) { "2" })
                "mix" -> RichSegment.Mixed(
                    parts.getOrElse(0) { "3" },
                    parts.getOrElse(1) { "1" },
                    parts.getOrElse(2) { "2" }
                )
                "sqrt" -> RichSegment.Root("", parts.getOrElse(0) { "x" })
                "root" -> RichSegment.Root(parts.getOrElse(0) { "3" }, parts.getOrElse(1) { "x" })
                "sup" -> RichSegment.Power(parts.getOrElse(0) { "x" }, parts.getOrElse(1) { "2" })
                "sub" -> RichSegment.Subscript(parts.getOrElse(0) { "x" }, parts.getOrElse(1) { "1" })
                "rasta" -> RichSegment.RastaLine(parts.getOrElse(0) { "AB" })
                "over" -> RichSegment.Overline(parts.getOrElse(0) { "AB" })
                "b" -> RichSegment.Styled(text = payload, bold = true)
                "i" -> RichSegment.Styled(text = payload, italic = true)
                "u" -> RichSegment.Styled(text = payload, underline = true)
                "hi" -> RichSegment.Styled(
                    text = parts.drop(1).joinToString("|").ifEmpty { parts.getOrElse(0) { "" } },
                    highlightHex = if (parts.size > 1) parts[0] else "#FEF08A"
                )
                else -> RichSegment.Plain(match.value)
            }
            result.add(segment)
            lastIndex = match.range.last + 1
        }
        if (lastIndex < input.length) {
            result.add(RichSegment.Plain(input.substring(lastIndex)))
        }
        return result
    }

    fun toPlainReadable(input: String): String {
        return parse(input).joinToString("") { seg ->
            when (seg) {
                is RichSegment.Plain -> seg.text
                is RichSegment.Styled -> seg.text
                is RichSegment.Fraction -> "(${seg.num}/${seg.den})"
                is RichSegment.Mixed -> "${seg.whole} (${seg.num}/${seg.den})"
                is RichSegment.Root -> if (seg.index.isBlank()) "√(${seg.radicand})" else "${seg.index}√(${seg.radicand})"
                is RichSegment.Power -> "${seg.base}^${seg.exp}"
                is RichSegment.Subscript -> "${seg.base}_${seg.sub}"
                is RichSegment.RastaLine -> "↔(${seg.label})"
                is RichSegment.Overline -> "${seg.label}̄"
            }
        }
    }

    fun toWordHtml(input: String): String {
        val sb = StringBuilder()
        parse(input).forEach { seg ->
            when (seg) {
                is RichSegment.Plain -> sb.append(escapeHtml(seg.text).replace("\n", "<br>"))
                is RichSegment.Styled -> {
                    var inner = escapeHtml(seg.text).replace("\n", "<br>")
                    if (seg.bold) inner = "<b>$inner</b>"
                    if (seg.italic) inner = "<i>$inner</i>"
                    if (seg.underline) inner = "<u>$inner</u>"
                    if (seg.highlightHex.isNotBlank()) {
                        inner = """<span style="background-color:${escapeHtml(seg.highlightHex)};padding:0 2px">$inner</span>"""
                    }
                    sb.append(inner)
                }
                is RichSegment.Fraction -> sb.append(
                    """<span class="fr"><span class="ft">${escapeHtml(seg.num)}</span><span class="fb">${escapeHtml(seg.den)}</span></span>"""
                )
                is RichSegment.Mixed -> sb.append(
                    """<b>${escapeHtml(seg.whole)}</b><span class="fr"><span class="ft">${escapeHtml(seg.num)}</span><span class="fb">${escapeHtml(seg.den)}</span></span>"""
                )
                is RichSegment.Root -> sb.append(
                    """<span>${if (seg.index.isNotBlank()) "<sup>${escapeHtml(seg.index)}</sup>" else ""}√<span style="border-top:1.5px solid #000;padding:0 3px">${escapeHtml(seg.radicand)}</span></span>"""
                )
                is RichSegment.Power -> sb.append("${escapeHtml(seg.base)}<sup>${escapeHtml(seg.exp)}</sup>")
                is RichSegment.Subscript -> sb.append("${escapeHtml(seg.base)}<sub>${escapeHtml(seg.sub)}</sub>")
                is RichSegment.RastaLine -> sb.append(
                    """<span style="display:inline-block;text-align:center;line-height:0.8">↔<br><b>${escapeHtml(seg.label)}</b></span>"""
                )
                is RichSegment.Overline -> sb.append(
                    """<span style="border-top:1.5px solid #000;padding:0 3px"><b>${escapeHtml(seg.label)}</b></span>"""
                )
            }
        }
        return sb.toString()
    }

    fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
