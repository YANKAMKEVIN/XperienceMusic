// Guards the UI rules of the code-quality audit against regressions. Applied by :shared.
//
// Scans shared/src/commonMain/kotlin/<package>/ui/** and navigation/** and fails on:
//   text-literal            Text("...") with a literal that holds letters (interpolations do not count)
//   content-description     contentDescription = "..."
//   inline-size             <n>.dp / <n>.sp in an expression (0.dp, top-level `val NAME = 12.dp`, ui/theme are fine)
//   color-literal           Color(0x...) outside ui/theme
//   rounded-corner-literal  RoundedCornerShape(<n>...) instead of a Radius token
//   koin-inject             koinInject<T>() where T is not a use case (repositories never reach the UI)
// Code inside a @Preview function is exempt. Other exceptions go to config/ui-literals/allowlist.txt.
val uiRoot = layout.projectDirectory.dir("src/commonMain/kotlin")
val allowlistFile = rootProject.layout.projectDirectory.file("config/ui-literals/allowlist.txt")

val checkUiLiterals by tasks.registering {
    group = "verification"
    description = "Fails on hardcoded UI text, sizes, colours and repository injection in the shared UI."
    val sources = fileTree(uiRoot) {
        include("**/ui/**/*.kt", "**/navigation/**/*.kt")
        exclude("**/ui/theme/**")
    }
    inputs.files(sources).withPropertyName("sources").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(allowlistFile).withPropertyName("allowlist")
    val rootDir = uiRoot.asFile
    val allowFile = allowlistFile.asFile

    doLast {
        val allowed = allowFile.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        val textLiteral = Regex("""\bText\(\s*"((?:[^"\\]|\\.)*)"""")
        val interpolation = Regex("""\$\{[^}]*}|\$\w+""")
        val contentDescription = Regex("""\bcontentDescription\s*=\s*"""")
        val inlineSize = Regex("""(?<![\w.])(\d+(?:\.\d+)?)\.(?:dp|sp)\b""")
        val topLevelValue = Regex("""^(?:private |internal |public )?(?:const )?val \w+(?:\s*:\s*[\w<>?, ]+)?\s*=""")
        val colorLiteral = Regex("""\bColor\(\s*0x""")
        val roundedCorner = Regex("""\bRoundedCornerShape\(\s*\d""")
        val koinInject = Regex("""\bkoinInject<\s*([\w.]+)""")

        val violations = mutableListOf<String>()
        sources.files.sorted().forEach { file ->
            val path = file.relativeTo(rootDir).path.substringAfter("/shared/", file.relativeTo(rootDir).path)
            val short = path.substringAfter("/ui/", "").ifEmpty { path.substringAfter("/navigation/", path) }
                .let { if (path.contains("/navigation/")) "navigation/$it" else it }
            val text = file.readText()
            val lines = text.lines()
            // Lines inside a @Preview function: from the annotation to the closing brace at column 0.
            val inPreview = BooleanArray(lines.size)
            var previewOpen = false
            lines.forEachIndexed { i, line ->
                if (line.trimStart().startsWith("@Preview")) previewOpen = true
                if (previewOpen) inPreview[i] = true
                if (previewOpen && (line == "}" || (line.isBlank() && !lines.getOrElse(i - 1) { "" }.trimStart().startsWith("@")))) {
                    previewOpen = false
                }
            }
            val lineStarts = IntArray(lines.size)
            var offset = 0
            lines.forEachIndexed { i, line -> lineStarts[i] = offset; offset += line.length + 1 }
            fun lineOf(position: Int): Int {
                var low = 0
                var high = lines.size - 1
                while (low < high) {
                    val mid = (low + high + 1) / 2
                    if (lineStarts[mid] <= position) low = mid else high = mid - 1
                }
                return low
            }
            fun report(rule: String, lineIndex: Int, message: String) {
                if (inPreview[lineIndex] || "$short:$rule" in allowed) return
                violations += "$short:${lineIndex + 1}: [$rule] $message"
            }
            fun isComment(i: Int) = lines[i].trimStart().let { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }

            textLiteral.findAll(text).forEach { m ->
                val i = lineOf(m.range.first)
                if (!isComment(i) && m.groupValues[1].replace(interpolation, "").any { it.isLetter() }) {
                    report("text-literal", i, "Text(\"${m.groupValues[1]}\"): use stringResource(Res.string...)")
                }
            }
            contentDescription.findAll(text).forEach { m ->
                val i = lineOf(m.range.first)
                if (!isComment(i)) report("content-description", i, "contentDescription literal: use stringResource(Res.string...)")
            }
            lines.forEachIndexed { i, line ->
                if (isComment(i)) return@forEachIndexed
                val code = line.substringBefore("//")
                if (!topLevelValue.containsMatchIn(code)) {
                    inlineSize.findAll(code).filter { it.groupValues[1].toDouble() != 0.0 }.forEach {
                        report("inline-size", i, "${it.value}: use Spacing, Radius or Sizes, or name the constant")
                    }
                }
                if (colorLiteral.containsMatchIn(code)) report("color-literal", i, "Color(0x...): use the theme colours")
                if (roundedCorner.containsMatchIn(code)) report("rounded-corner-literal", i, "RoundedCornerShape(n): use a Radius token")
                koinInject.findAll(code).forEach {
                    if (!it.groupValues[1].endsWith("UseCase")) {
                        report("koin-inject", i, "koinInject<${it.groupValues[1]}>: the UI only gets use cases, from the root")
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "checkUiLiterals found ${violations.size} hardcoded UI value(s):\n" + violations.joinToString("\n") +
                    "\nUse a token or a string resource, or justify an exception in config/ui-literals/allowlist.txt.",
            )
        }
    }
}

tasks.matching { it.name == "check" }.configureEach { dependsOn(checkUiLiterals) }
