package org.ergoplatform.mosaik.example

import org.ergoplatform.appkit.*
import org.ergoplatform.ergopay.ErgoPayResponse
import org.ergoplatform.mosaik.example.mosaik.TOKENBURN_COMPLETE_ACTION
import org.ergoplatform.mosaik.example.mosaik.isMainNetAddress
import org.ergoplatform.mosaik.example.mosaik.showMosaikVersionError
import org.ergoplatform.mosaik.example.mosaik.tokenBurnScreen
import org.ergoplatform.mosaik.example.service.ExplorerApiService
import org.ergoplatform.mosaik.example.service.MosaikUserService
import org.ergoplatform.mosaik.invokeErgoPay
import org.ergoplatform.mosaik.jackson.MosaikSerializer
import org.ergoplatform.mosaik.model.FetchActionResponse
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.mosaikApp
import org.ergoplatform.mosaik.showDialog
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
@CrossOrigin
class TokenBurnController(
    private val mosaikUserService: MosaikUserService,
    private val explorerApiService: ExplorerApiService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/")
    fun mainPage(): ModelAndView {
        // we always serve nobrowser error page for the main url. If the request came from a
        // Mosaik executor, it will pick up the <link rel="mosaik" ...> entry
        return ModelAndView("nobrowser.html")
    }

    private fun HttpServletRequest.getServerUrl() =
        requestURL.toString().substringBefore(TOKENBURN_MAIN_URI)

    @GetMapping("/$TOKENBURN_MAIN_URI")
    fun mainApp(
        @RequestHeader headers: Map<String, String>,
        httpServletRequest: HttpServletRequest
    ): MosaikApp {
        return mosaikApp(
            "Token Burner",
            1,
            "Burn unwanted tokens",
            targetMosaikVersion = MosaikContext.LIBRARY_MOSAIK_VERSION,
            targetCanvasDimension = MosaikManifest.CanvasDimension.COMPACT_WIDTH,
        ) {
            val context = MosaikSerializer.fromContextHeadersMap(headers)

            if (context.mosaikVersion < 1) {
                // we need at least version 1
                showMosaikVersionError(1)
            } else {
                tokenBurnScreen(
                    context,
                    mosaikUserService,
                    httpServletRequest.getServerUrl()
                )
            }
        }
    }

    @PostMapping("/$TOKENBURN_PREPARE_TX_URI")
    fun prepareTransaction(
        @RequestBody values: Map<String, Any?>,
        @RequestHeader headers: Map<String, String>,
        request: HttpServletRequest,
    ): FetchActionResponse {

        // collect all tokens that were marked to be burned
        val tokensToBurn =
            values.entries.filter { it.key.startsWith(TOKENBURN_CHECK_PREFIX) && (it.value as? Boolean) == true }
                .map { it.key.substringAfter(TOKENBURN_CHECK_PREFIX) }

        val context = MosaikSerializer.fromContextHeadersMap(headers)

        mosaikUserService.setUserTokenList(context.guid, tokensToBurn)

        return FetchActionResponse(
            1,
            if (tokensToBurn.isNotEmpty())
                invokeErgoPay(
                    "ergopay://" + request.getServerUrl().trimEnd('/').substringAfter("://") +
                            "/" + TOKENBURN_GET_TX_URI.replace("{uuid}", context.guid),
                    id = "BURN_TOKEN_ACTION",
                ) {
                    onFinished = TOKENBURN_COMPLETE_ACTION
                }
            else
                showDialog("Please select at least one token to burn.")
        )
    }

    @GetMapping("/$TOKENBURN_GET_TX_URI")
    fun getBurningTransaction(@PathVariable uuid: String): ErgoPayResponse {
        val response = ErgoPayResponse()

        val mosaikUser = mosaikUserService.getMosaikUser(uuid)

        // this is more or less taken from ergopay example repo
        try {
            val address = mosaikUser!!.p2PKAddress!!
            val isMainNet: Boolean = isMainNetAddress(address)
            val amountToSend = 1000L * 1000L
            val sender = Address.create(address)
            val recipient = Address.create(address)
            val tokenBalances = mosaikUserService.getBalanceForUser(uuid)?.tokens ?: emptyList()
            val tokensToBurn = mosaikUserService.getUserTokenList(mosaikUser)

            val tokensToBurnList = tokenBalances.filter { tokensToBurn.contains(it.tokenId) }.map {
                ErgoToken(it.tokenId, it.amount)
            }

            val reduced: ByteArray = getReducedTx(isMainNet, amountToSend, tokensToBurnList, sender,
                outputBuilder = { unsignedTxBuilder: UnsignedTransactionBuilder ->
                    val contract = recipient.toErgoContract()
                    val outBoxBuilder = unsignedTxBuilder.outBoxBuilder()
                        .value(amountToSend)
                        .contract(contract)
                    val newBox = outBoxBuilder.build()
                    unsignedTxBuilder.outputs(newBox).tokensToBurn(*tokensToBurnList.toTypedArray())
                    unsignedTxBuilder
                }
            ).toBytes()
            response.reducedTx = Base64.getUrlEncoder().encodeToString(reduced)
            response.address = address
            response.message = "Send this transaction to burn the selected tokens (see below)."
            response.messageSeverity = ErgoPayResponse.Severity.INFORMATION
        } catch (t: Throwable) {
            response.messageSeverity = ErgoPayResponse.Severity.ERROR
            response.message = t.message ?: "Unconditional error."
            logger.error("Error burn token", t)
        }

        return response
    }

    private fun getReducedTx(
        isMainNet: Boolean, amountToSpend: Long, tokensToSpend: List<ErgoToken>,
        sender: Address,
        outputBuilder: (UnsignedTransactionBuilder) -> UnsignedTransactionBuilder
    ): ReducedTransaction {
        val networkType = if (isMainNet) NetworkType.MAINNET else NetworkType.TESTNET
        return RestApiErgoClient.create(
            getDefaultNodeUrl(isMainNet),
            networkType,
            "",
            RestApiErgoClient.getDefaultExplorerUrl(networkType)
        ).execute { ctx: BlockchainContext ->
            val boxesToSpend =
                BoxOperations.createForSender(sender, ctx)
                    .withAmountToSpend(amountToSpend)
                    .withTokensToSpend(tokensToSpend)
                    .loadTop()
            val changeAddress = sender.asP2PK()
            val txB = ctx.newTxBuilder()
            val unsignedTransactionBuilder = txB.boxesToSpend(boxesToSpend)
                .fee(Parameters.MinFee)
                .sendChangeTo(changeAddress)
            val unsignedTransaction = outputBuilder(unsignedTransactionBuilder).build()
            ctx.newProverBuilder().build().reduce(unsignedTransaction, 0)
        }
    }

    private fun getDefaultNodeUrl(mainNet: Boolean): String {
        return if (mainNet) NODE_MAINNET else NODE_TESTNET
    }

}

const val TOKENBURN_MAIN_URI = "tokenburn"

const val TOKENBURN_PREPARE_TX_URI = "tokenburn/prepare"

const val TOKENBURN_GET_TX_URI = "tokenburn/get/{uuid}"

const val TOKENBURN_CHECK_PREFIX = "CHECK_"

// this class processes all requests from the an ErgoPay wallet application
const val NODE_MAINNET = "http://213.239.193.208:9053/"
const val NODE_TESTNET = "http://213.239.193.208:9052/"
