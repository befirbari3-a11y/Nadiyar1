package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object ExamJsonCodec {

    fun encodeDocument(doc: ExamDocumentState): String {
        val root = JSONObject()
        root.put("language", doc.language.key)
        root.put("numeralMode", doc.numeralMode.key)
        root.put("header", encodeHeaderObj(doc.header))
        root.put("style", encodeStyleObj(doc.style))

        val qArr = JSONArray()
        doc.questions.forEach { q -> qArr.put(encodeQuestion(q)) }
        root.put("questions", qArr)

        val pArr = JSONArray()
        doc.pages.forEach { p -> pArr.put(encodePage(p)) }
        root.put("pages", pArr)
        return root.toString()
    }

    fun decodeDocument(json: String): ExamDocumentState {
        val root = JSONObject(json)
        val lang = ExamLanguage.fromKey(root.optString("language", "badini"))
        val numMode = NumeralMode.fromKey(root.optString("numeralMode", "auto"))
        val header = root.optJSONObject("header")?.let { decodeHeaderObj(it, lang) }
            ?: HeaderState(values = LocalizationPresets.strings(lang).headerDefaults)
        val style = root.optJSONObject("style")?.let { decodeStyleObj(it) } ?: ExamStyle()

        val questions = mutableListOf<Question>()
        val qArr = root.optJSONArray("questions")
        if (qArr != null) {
            for (i in 0 until qArr.length()) {
                questions.add(decodeQuestion(qArr.getJSONObject(i)))
            }
        }

        val pages = mutableListOf<ExamPage>()
        val pArr = root.optJSONArray("pages")
        if (pArr != null) {
            for (i in 0 until pArr.length()) {
                pages.add(decodePage(pArr.getJSONObject(i)))
            }
        }
        if (pages.isEmpty()) pages.add(ExamPage(showHeader = true))

        return ExamDocumentState(
            language = lang,
            numeralMode = numMode,
            header = header,
            questions = questions,
            pages = pages,
            style = style
        )
    }

    fun encodeHeader(header: HeaderState): String = encodeHeaderObj(header).toString()

    fun decodeHeader(json: String, fallbackLang: ExamLanguage): HeaderState =
        decodeHeaderObj(JSONObject(json), fallbackLang)

    fun encodeCertificate(cert: CertificateState): String {
        val o = JSONObject()
        o.put("templateId", cert.templateId)
        o.put("isLandscape", cert.isLandscape)
        o.put("fontKey", cert.fontKey)
        o.put("fontScale", cert.fontScale.toDouble())
        o.put("bgColorHex", cert.bgColorHex)
        o.put("textColorHex", cert.textColorHex)
        o.put("borderStyle", cert.borderStyle.key)
        o.put("topLine1", cert.topLine1)
        o.put("topLine2", cert.topLine2)
        o.put("badgeTitle", cert.badgeTitle)
        o.put("bodyTop", cert.bodyTop)
        o.put("recipientName", cert.recipientName)
        o.put("bodyBottom", cert.bodyBottom)
        o.put("footerLeft", cert.footerLeft)
        o.put("footerCenter", cert.footerCenter)
        o.put("footerRight", cert.footerRight)
        val fl = JSONArray()
        cert.floatingElements.forEach { fl.put(encodeFloating(it)) }
        o.put("floatingElements", fl)
        return o.toString()
    }

    fun decodeCertificate(json: String): CertificateState {
        val o = JSONObject(json)
        val flArr = o.optJSONArray("floatingElements")
        val fl = mutableListOf<FloatingElement>()
        if (flArr != null) {
            for (i in 0 until flArr.length()) fl.add(decodeFloating(flArr.getJSONObject(i)))
        }
        return CertificateState(
            templateId = o.optString("templateId", "turq"),
            isLandscape = o.optBoolean("isLandscape", true),
            fontKey = o.optString("fontKey", "scheherazade"),
            fontScale = o.optDouble("fontScale", 1.0).toFloat().coerceIn(0.6f, 1.8f),
            bgColorHex = o.optString("bgColorHex", "#F3FCFB"),
            textColorHex = o.optString("textColorHex", "#0F766E"),
            borderStyle = CertBorderStyle.fromKey(o.optString("borderStyle", "turq-gold")),
            topLine1 = o.optString("topLine1", ""),
            topLine2 = o.optString("topLine2", ""),
            badgeTitle = o.optString("badgeTitle", ""),
            bodyTop = o.optString("bodyTop", ""),
            recipientName = o.optString("recipientName", ""),
            bodyBottom = o.optString("bodyBottom", ""),
            footerLeft = o.optString("footerLeft", ""),
            footerCenter = o.optString("footerCenter", ""),
            footerRight = o.optString("footerRight", ""),
            floatingElements = fl
        )
    }

    private fun encodeHeaderObj(h: HeaderState): JSONObject {
        val o = JSONObject()
        val vals = JSONObject()
        h.values.forEach { (k, v) -> vals.put(k.key, v) }
        val vis = JSONObject()
        h.visible.forEach { (k, v) -> vis.put(k.key, v) }
        o.put("values", vals)
        o.put("visible", vis)
        o.put("locked", h.locked)
        o.put("logoBase64", h.logoBase64)
        o.put("logoPos", h.logoPos.key)
        o.put("logoSizeDp", h.logoSizeDp)
        return o
    }

    private fun decodeHeaderObj(o: JSONObject, lang: ExamLanguage): HeaderState {
        val defaults = LocalizationPresets.strings(lang).headerDefaults
        val valsObj = o.optJSONObject("values")
        val visObj = o.optJSONObject("visible")
        val vals = HeaderFieldKey.entries.associateWith { k ->
            valsObj?.optString(k.key, defaults[k].orEmpty()) ?: defaults[k].orEmpty()
        }
        val vis = HeaderFieldKey.entries.associateWith { k ->
            visObj?.optBoolean(k.key, true) ?: true
        }
        return HeaderState(
            values = vals,
            visible = vis,
            locked = o.optBoolean("locked", false),
            logoBase64 = o.optString("logoBase64", ""),
            logoPos = LogoPosition.fromKey(o.optString("logoPos", "center")),
            logoSizeDp = o.optInt("logoSizeDp", 54)
        )
    }

    private fun encodeStyleObj(s: ExamStyle): JSONObject = JSONObject().apply {
        put("fontKey", s.fontKey)
        put("fontSizeSp", s.fontSizeSp)
        put("headerScalePct", s.headerScalePct)
        put("headerColorHex", s.headerColorHex)
        put("questionTitleColorHex", s.questionTitleColorHex)
        put("bodyTextColorHex", s.bodyTextColorHex)
        put("paperBgColorHex", s.paperBgColorHex)
        put("borderStyle", s.borderStyle.key)
        put("borderColorHex", s.borderColorHex)
    }

    private fun decodeStyleObj(o: JSONObject): ExamStyle = ExamStyle(
        fontKey = o.optString("fontKey", "scheherazade"),
        fontSizeSp = o.optInt("fontSizeSp", 15),
        headerScalePct = o.optInt("headerScalePct", 93),
        headerColorHex = o.optString("headerColorHex", "#000000"),
        questionTitleColorHex = o.optString("questionTitleColorHex", "#1E3A8A"),
        bodyTextColorHex = o.optString("bodyTextColorHex", "#000000"),
        paperBgColorHex = o.optString("paperBgColorHex", "#FFFFFF"),
        borderStyle = BorderStyle.fromKey(o.optString("borderStyle", "solid")),
        borderColorHex = o.optString("borderColorHex", "#0F172A")
    )

    private fun encodeQuestion(q: Question): JSONObject = JSONObject().apply {
        put("id", q.id)
        put("type", q.type.key)
        put("title", q.title)
        put("isDefaultTitle", q.isDefaultTitle)
        put("marks", q.marks)
        put("isDefaultMarks", q.isDefaultMarks)
        put("pageIndex", q.pageIndex)
        put("answerLines", q.answerLines)
        put("lineSize", q.lineSize.key)
        put("spacerHeightDp", q.spacerHeightDp)
        put("bgColorHex", q.bgColorHex)
        put("noSeparatorLine", q.noSeparatorLine)
        put("imageBase64", q.imageBase64)
        put("imageWidthDp", q.imageWidthDp)
        put("conceptCenter", q.conceptCenter)
        put("isDefaultConcept", q.isDefaultConcept)
        val br = JSONArray()
        q.conceptBranches.forEach { br.put(it) }
        put("conceptBranches", br)
        val its = JSONArray()
        q.items.forEach { itObj ->
            val io = JSONObject()
            io.put("id", itObj.id)
            io.put("text", itObj.text)
            io.put("isDefault", itObj.isDefault)
            io.put("matchRight", itObj.matchRight)
            val opts = JSONArray()
            itObj.options.forEach { op ->
                opts.put(JSONObject().apply {
                    put("id", op.id)
                    put("text", op.text)
                    put("isDefault", op.isDefault)
                })
            }
            io.put("options", opts)
            its.put(io)
        }
        put("items", its)
    }

    private fun decodeQuestion(o: JSONObject): Question {
        val branches = mutableListOf<String>()
        val brArr = o.optJSONArray("conceptBranches")
        if (brArr != null) {
            for (i in 0 until brArr.length()) branches.add(brArr.getString(i))
        }
        val items = mutableListOf<QuestionItem>()
        val itArr = o.optJSONArray("items")
        if (itArr != null) {
            for (i in 0 until itArr.length()) {
                val io = itArr.getJSONObject(i)
                val opts = mutableListOf<McqOption>()
                val opArr = io.optJSONArray("options")
                if (opArr != null) {
                    for (j in 0 until opArr.length()) {
                        val oo = opArr.getJSONObject(j)
                        opts.add(
                            McqOption(
                                id = oo.optString("id"),
                                text = oo.optString("text"),
                                isDefault = oo.optBoolean("isDefault", false)
                            )
                        )
                    }
                }
                items.add(
                    QuestionItem(
                        id = io.optString("id"),
                        text = io.optString("text"),
                        isDefault = io.optBoolean("isDefault", false),
                        options = opts,
                        matchRight = io.optString("matchRight", "")
                    )
                )
            }
        }
        return Question(
            id = o.optString("id"),
            type = QuestionType.fromKey(o.optString("type", "free")),
            title = o.optString("title", ""),
            isDefaultTitle = o.optBoolean("isDefaultTitle", false),
            marks = o.optString("marks", "10"),
            isDefaultMarks = o.optBoolean("isDefaultMarks", false),
            pageIndex = o.optInt("pageIndex", 0),
            items = items,
            answerLines = o.optInt("answerLines", 0),
            lineSize = LineSize.fromKey(o.optString("lineSize", "md")),
            spacerHeightDp = o.optInt("spacerHeightDp", 0),
            bgColorHex = o.optString("bgColorHex", ""),
            noSeparatorLine = o.optBoolean("noSeparatorLine", false),
            imageBase64 = o.optString("imageBase64", ""),
            imageWidthDp = o.optInt("imageWidthDp", 180),
            conceptCenter = o.optString("conceptCenter", ""),
            conceptBranches = branches,
            isDefaultConcept = o.optBoolean("isDefaultConcept", false)
        )
    }

    private fun encodePage(p: ExamPage): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("showHeader", p.showHeader)
        put("freeWriteText", p.freeWriteText)
        val fl = JSONArray()
        p.floatingElements.forEach { fl.put(encodeFloating(it)) }
        put("floatingElements", fl)
        val st = JSONArray()
        p.penStrokes.forEach { s ->
            st.put(JSONObject().apply {
                put("id", s.id)
                put("colorHex", s.colorHex)
                put("widthDp", s.widthDp.toDouble())
                put("dashed", s.dashed)
                put("isArrow", s.isArrow)
                val pts = JSONArray()
                s.points.forEach { pt ->
                    pts.put(JSONObject().apply {
                        put("x", pt.x.toDouble())
                        put("y", pt.y.toDouble())
                    })
                }
                put("points", pts)
            })
        }
        put("penStrokes", st)
    }

    private fun decodePage(o: JSONObject): ExamPage {
        val fl = mutableListOf<FloatingElement>()
        val flArr = o.optJSONArray("floatingElements")
        if (flArr != null) {
            for (i in 0 until flArr.length()) fl.add(decodeFloating(flArr.getJSONObject(i)))
        }
        val strokes = mutableListOf<PenStroke>()
        val stArr = o.optJSONArray("penStrokes")
        if (stArr != null) {
            for (i in 0 until stArr.length()) {
                val so = stArr.getJSONObject(i)
                val pts = mutableListOf<NormalizedPoint>()
                val pArr = so.optJSONArray("points")
                if (pArr != null) {
                    for (j in 0 until pArr.length()) {
                        val po = pArr.getJSONObject(j)
                        pts.add(NormalizedPoint(po.optDouble("x", 0.0).toFloat(), po.optDouble("y", 0.0).toFloat()))
                    }
                }
                strokes.add(
                    PenStroke(
                        id = so.optString("id"),
                        points = pts,
                        colorHex = so.optString("colorHex", "#000000"),
                        widthDp = so.optDouble("widthDp", 2.0).toFloat(),
                        dashed = so.optBoolean("dashed", false),
                        isArrow = so.optBoolean("isArrow", false)
                    )
                )
            }
        }
        return ExamPage(
            id = o.optString("id"),
            showHeader = o.optBoolean("showHeader", true),
            freeWriteText = o.optString("freeWriteText", ""),
            floatingElements = fl,
            penStrokes = strokes
        )
    }

    private fun encodeFloating(f: FloatingElement): JSONObject = JSONObject().apply {
        put("id", f.id)
        put("type", f.type.key)
        put("xFrac", f.xFrac.toDouble())
        put("yFrac", f.yFrac.toDouble())
        put("wFrac", f.wFrac.toDouble())
        put("hFrac", f.hFrac.toDouble())
        put("rotationDeg", f.rotationDeg.toDouble())
        put("locked", f.locked)
        put("text", f.text)
        put("secondaryText", f.secondaryText)
        put("strokeColorHex", f.strokeColorHex)
        put("bgColorHex", f.bgColorHex)
        put("showBorder", f.showBorder)
        put("fontSizeSp", f.fontSizeSp.toDouble())
        put("shapeKind", f.shapeKind.key)
        put("imageBase64", f.imageBase64)
        val rows = JSONArray()
        f.tableRows.forEach { row ->
            val r = JSONArray()
            row.forEach { cell -> r.put(cell) }
            rows.put(r)
        }
        put("tableRows", rows)
    }

    private fun decodeFloating(o: JSONObject): FloatingElement {
        val rows = mutableListOf<List<String>>()
        val rArr = o.optJSONArray("tableRows")
        if (rArr != null) {
            for (i in 0 until rArr.length()) {
                val cArr = rArr.getJSONArray(i)
                val row = mutableListOf<String>()
                for (j in 0 until cArr.length()) row.add(cArr.getString(j))
                rows.add(row)
            }
        }
        return FloatingElement(
            id = o.optString("id"),
            type = FloatingElementType.fromKey(o.optString("type", "text_box")),
            xFrac = o.optDouble("xFrac", 0.1).toFloat(),
            yFrac = o.optDouble("yFrac", 0.25).toFloat(),
            wFrac = o.optDouble("wFrac", 0.35).toFloat(),
            hFrac = o.optDouble("hFrac", 0.08).toFloat(),
            rotationDeg = o.optDouble("rotationDeg", 0.0).toFloat(),
            locked = o.optBoolean("locked", false),
            text = o.optString("text", ""),
            secondaryText = o.optString("secondaryText", ""),
            strokeColorHex = o.optString("strokeColorHex", "#000000"),
            bgColorHex = o.optString("bgColorHex", "#FFFFFF"),
            showBorder = o.optBoolean("showBorder", true),
            fontSizeSp = o.optDouble("fontSizeSp", 13.0).toFloat(),
            shapeKind = ShapeKind.fromKey(o.optString("shapeKind", "circle")),
            tableRows = rows,
            imageBase64 = o.optString("imageBase64", "")
        )
    }
}
