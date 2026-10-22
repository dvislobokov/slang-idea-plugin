package org.zell.idea.lsp

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.zell.idea.toolchain.ZellToolchain
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The client-side commands behind zell-lsp's code lenses (upstream
 * docs/TOOLING.md "Code lens", editors/vscode/src/extension.ts):
 *
 *  - `zell.runTest [uri, name]`: `zell test <package or file> --filter ^name$ -v`;
 *  - `zell.debugTest [uri, name]`: `zell test` has no debug mode yet, so the
 *    test runs verbosely, like in VS Code;
 *  - `zell.run [uri]`: `zell run <package or file>`;
 *  - `zell.showReferences [uri, position, locations]`: a list of the
 *    reference locations to navigate to.
 *
 * Arguments come from lsp4j's Gson as [JsonElement]s (or plain values).
 */
object ZellLspCommands {
    private val LOG = logger<ZellLspCommands>()
    private val gson = Gson()

    /** Runs [command]; false if it is not one of ours (left to the platform). */
    fun execute(project: Project, command: String, args: List<Any?>): Boolean {
        when (command) {
            "zell.runTest" -> runTest(project, string(args, 0) ?: return true, string(args, 1) ?: return true)
            "zell.debugTest" -> {
                val uri = string(args, 0) ?: return true
                val name = string(args, 1) ?: return true
                notify(project, "Debugging a single test is not supported by `zell test` yet; running $name with -v instead.")
                runTest(project, uri, name)
            }
            "zell.run" -> runMain(project, string(args, 0) ?: return true)
            "zell.showReferences" -> showReferences(project, args.getOrNull(2))
            else -> return false
        }
        return true
    }

    private fun runTest(project: Project, uri: String, name: String) {
        val file = pathOf(uri) ?: return
        runZell(project, file, "test $name", listOf("test", target(file), "--filter", "^$name$", "-v"))
    }

    private fun runMain(project: Project, uri: String) {
        val file = pathOf(uri) ?: return
        runZell(project, file, "run", listOf("run", target(file)))
    }

    /** `zell <args>` in a Run tool window tab, from the project root (or the file's folder). */
    private fun runZell(project: Project, file: Path, title: String, args: List<String>) {
        val zell = ZellToolchain.zellExecutable()
        if (zell == null) {
            notify(project, "The zell compiler was not found (Settings | Languages &amp; Frameworks | Zell).")
            return
        }
        val cwd = project.basePath ?: file.parent.toString()
        val commandLine = GeneralCommandLine(listOf(zell.toString()) + args)
            .withWorkDirectory(cwd)
            .withCharset(Charsets.UTF_8)
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            try {
                val handler = OSProcessHandler(commandLine)
                RunContentExecutor(project, handler).withTitle("Zell: $title").withActivateToolWindow(true).run()
            } catch (e: Exception) {
                LOG.warn("cannot start ${commandLine.commandLineString}", e)
                notify(project, "Cannot start `${commandLine.commandLineString}`: ${e.message}")
            }
        }
    }

    /** The package directory of [file] (nearest folder with a zell manifest), else the file itself. */
    private fun target(file: Path): String {
        var dir: Path? = file.parent
        while (dir != null) {
            val d: Path = dir
            if (MANIFESTS.any { Files.isRegularFile(d.resolve(it)) }) return d.toString()
            dir = d.parent
        }
        return file.toString()
    }

    private fun showReferences(project: Project, locations: Any?) {
        val targets = jsonOf(locations)?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let(::location) }
            .orEmpty()
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            val descriptors = targets.mapNotNull { it.descriptor(project) }
            when (descriptors.size) {
                0 -> notify(project, "No references.")
                1 -> descriptors[0].navigate(true)
                else -> JBPopupFactory.getInstance()
                    .createListPopup(object : BaseListPopupStep<OpenFileDescriptor>("References (${descriptors.size})", descriptors) {
                        override fun getTextFor(value: OpenFileDescriptor): String =
                            "${value.file.presentableUrl}:${value.line + 1}:${value.column + 1}"

                        override fun onChosen(selectedValue: OpenFileDescriptor, finalChoice: Boolean): PopupStep<*>? {
                            selectedValue.navigate(true)
                            return FINAL_CHOICE
                        }
                    })
                    .showCenteredInCurrentWindow(project)
            }
        }
    }

    /** An LSP `Location` (`uri`, `range.start.line/character`, UTF-16 columns as in IntelliJ documents). */
    private class Target(val uri: String, val line: Int, val character: Int) {
        fun descriptor(project: Project): OpenFileDescriptor? {
            val file: VirtualFile = pathOf(uri)?.let { VfsUtil.findFile(it, true) } ?: return null
            return OpenFileDescriptor(project, file, line, character)
        }
    }

    private fun location(o: JsonObject): Target? {
        val uri = o.get("uri")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        val start = o.getAsJsonObject("range")?.getAsJsonObject("start") ?: return null
        return Target(uri, start.get("line")?.asInt ?: 0, start.get("character")?.asInt ?: 0)
    }

    private fun jsonOf(value: Any?): JsonElement? = when (value) {
        null -> null
        is JsonElement -> value
        else -> gson.toJsonTree(value)
    }

    private fun string(args: List<Any?>, i: Int): String? {
        val v = jsonOf(args.getOrNull(i)) ?: return null
        return if (v.isJsonPrimitive) v.asString else null
    }

    private fun pathOf(uri: String): Path? = try {
        Paths.get(URI(uri))
    } catch (e: Exception) {
        LOG.info("not a file URI: $uri", e)
        null
    }

    private fun notify(project: Project, text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Zell")
            .createNotification(text, NotificationType.INFORMATION)
            .notify(project)
    }

    /** Package manifests, as in upstream editors/vscode `packageDir`. */
    private val MANIFESTS = listOf("zell.toml", "zell.json", "zell.yaml", "zell.yml")
}
