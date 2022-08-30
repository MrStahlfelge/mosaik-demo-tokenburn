package org.ergoplatform.mosaik.example.mosaik

import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.example.TOKENBURN_CHECK_PREFIX
import org.ergoplatform.mosaik.example.TOKENBURN_PREPARE_TX_URI
import org.ergoplatform.mosaik.example.service.MosaikUserService
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.ViewContent
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.model.ui.layout.VAlignment
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.mosaik.model.ui.text.TruncationType

fun MosaikApp.tokenBurnScreen(
    context: MosaikContext,
    mosaikUserService: MosaikUserService,
    serverRequestUrl: String
) {
    reloadApp { id = RELOAD_ACTION_ID }

    val p2PKAddress = mosaikUserService.getMosaikUser(context.guid)?.p2PKAddress

    val connectWalletRequest =
        backendRequest(serverRequestUrl.trimEnd('/') + URL_CONNECT_WALLET)

    column(Padding.DEFAULT) {
        layout(HAlignment.JUSTIFY) {
            card(Padding.HALF_DEFAULT) {
                layout(HAlignment.JUSTIFY, VAlignment.CENTER) {
                    row(Padding.HALF_DEFAULT, spacing = Padding.HALF_DEFAULT) {

                        layout(weight = 1) {
                            column {
                                label(
                                    "Burn tokens",
                                    style = LabelStyle.BODY1BOLD,
                                    textAlignment = HAlignment.CENTER
                                )
                                ergoAddressChooser(
                                    ID_SELECT_WALLET,

                                    ) {
                                    onValueChangedAction = connectWalletRequest.id
                                    value = p2PKAddress
                                }
                            }
                        }
                    }
                }
            }

            if (p2PKAddress == null) {
                box(Padding.DEFAULT) {
                    label(
                        "Connect your wallet to list available tokens to burn.",
                        style = LabelStyle.HEADLINE2,
                        textAlignment = HAlignment.CENTER
                    )
                }
            }

            p2PKAddress?.let {
                val tokens =
                    mosaikUserService.getBalanceForUser(context.guid)?.tokens ?: emptyList()

                if (tokens.isEmpty()) {
                    box(Padding.DEFAULT) {
                        label(
                            "No tokens on this address. Connect another address.",
                            style = LabelStyle.HEADLINE2,
                            textAlignment = HAlignment.CENTER
                        )
                    }
                }

                val showColumnsId = "SHOW_COLUMNS"

                column(Padding.DEFAULT, spacing = Padding.QUARTER_DEFAULT) {
                    id = showColumnsId

                    layout(HAlignment.JUSTIFY) {
                        tokens.forEach { token ->

                            card {

                                column(Padding.DEFAULT) {

                                    val tokenId = token.tokenId

                                    tokenLabel(
                                        tokenId,
                                        token.name,
                                        token.amount,
                                        token.decimals ?: 0,
                                        LabelStyle.HEADLINE2
                                    )

                                    label(tokenId, textColor = ForegroundColor.SECONDARY) {
                                        maxLines = 1
                                        truncationType = TruncationType.MIDDLE
                                    }

                                    checkboxLabel(
                                        "$TOKENBURN_CHECK_PREFIX$tokenId",
                                        "Yes, burn this token",
                                        style = LabelStyle.BODY1BOLD
                                    )

                                }

                            }

                        }

                        if (!tokens.isEmpty()) {
                            button("Prepare burn transaction") {
                                onClickAction(backendRequest(serverRequestUrl.trimEnd('/') + "/$TOKENBURN_PREPARE_TX_URI"))
                            }
                        }
                    }
                }

                changeView(ViewContent().apply {
                    box(Padding.DEFAULT) {
                        id = showColumnsId
                        label(
                            "Transaction submitted. Please be aware that the tokens are lost forever.",
                            LabelStyle.HEADLINE2,
                            HAlignment.CENTER
                        )
                    }
                }, TOKENBURN_COMPLETE_ACTION)
            }
        }

    }
}

const val TOKENBURN_COMPLETE_ACTION = "action_burn_finished"