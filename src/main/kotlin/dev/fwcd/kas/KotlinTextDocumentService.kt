package dev.fwcd.kas

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType
import org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getFileOrScriptDeclarations
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * The implementation of text document-related requests, e.g. code completion etc.
 */
class KotlinTextDocumentService : TextDocumentService {
    /** The Kotlin analysis API session. */
    lateinit var session: StandaloneAnalysisAPISession
    var client: LanguageClient? = null

    /** Looks up a KtFile (the AST) for a URI via PsiManager. */
    private fun URI.findKtFile(): KtFile? {
        val fs = StandardFileSystems.local()
        val psiManager = PsiManager.getInstance(session.project)
        val path = Path.of(this)
        val vFile = fs.findFileByPath(path.toString())
        val psiFile = vFile?.let(psiManager::findFile)
        return psiFile as? KtFile
    }

    /** Fetch code completions. */
    override fun completion(params: CompletionParams?): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        client?.logMessage(MessageParams(MessageType.Info, "Publishing completions..."))
        val items = params
            ?.let { URI(it.textDocument.uri).findKtFile() }
            ?.let { ktFile ->
                // TODO: Proper completions, also figure out how the analysis API might be useful here (analyze { ... })
                ktFile.getFileOrScriptDeclarations()
                    .map { CompletionItem(it.name) }
            } ?: listOf()

        val list = CompletionList(items)
        return CompletableFuture.completedFuture(Either.forRight(list))
    }

    private fun KaSeverity.toLspSeverity(): DiagnosticSeverity = when (this) {
        KaSeverity.INFO -> DiagnosticSeverity.Information
        KaSeverity.WARNING -> DiagnosticSeverity.Warning
        KaSeverity.ERROR -> DiagnosticSeverity.Error
    }

    private fun PsiElement.toLspPosition(offset: Int): Position {
        val text = containingFile.text
        val lc = StringUtil.offsetToLineColumn(text, offset)
        return Position(lc.line, lc.column)
    }

    private fun PsiElement.toLspRange(textRange: TextRange): Range = Range(
        toLspPosition(textRange.startOffset),
        toLspPosition(textRange.endOffset)
    )

    private fun KaDiagnosticWithPsi<*>.toLspDiagnostic(): Diagnostic = Diagnostic().also {
        it.range = psi.toLspRange(textRanges.first())
        it.message = defaultMessage
        it.severity = severity.toLspSeverity()
    }

    /** Fetch diagnostics using the LSP 3.17 pull model. Uses the new analysis session. */
    override fun diagnostic(params: DocumentDiagnosticParams?): CompletableFuture<DocumentDiagnosticReport> {
        client?.logMessage(MessageParams(MessageType.Info, "Publishing diagnostics..."))
        val items = params
            ?.let { URI(it.textDocument.uri).findKtFile() }
            ?.let { ktFile ->
                analyze(ktFile) {
                    ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                        .map { it.toLspDiagnostic() }
                }
            }
            ?: listOf()
        val fullReport = RelatedFullDocumentDiagnosticReport(items)
        val report = DocumentDiagnosticReport(fullReport)
        return CompletableFuture.completedFuture(report)
    }

    override fun didOpen(params: DidOpenTextDocumentParams?) {
        // TODO
    }

    override fun didChange(params: DidChangeTextDocumentParams?) {
        params
            ?.let { URI(it.textDocument.uri).findKtFile() }
            ?.let { ktFile ->
                client?.logMessage(MessageParams(MessageType.Info, "Handling modifications..."))
                KaSourceModificationService.getInstance(session.project)
                    .handleElementModification(ktFile, KaElementModificationType.Unknown)
            }
    }

    override fun didClose(params: DidCloseTextDocumentParams?) {
        // TODO
    }

    override fun didSave(params: DidSaveTextDocumentParams?) {
        // TODO
    }
}
