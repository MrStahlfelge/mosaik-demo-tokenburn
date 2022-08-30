package org.ergoplatform.mosaik.example.mosaik

import io.swagger.v3.oas.annotations.Hidden
import org.ergoplatform.appkit.Address
import org.ergoplatform.mosaik.backendResponse
import org.ergoplatform.mosaik.example.service.MosaikUserService
import org.ergoplatform.mosaik.jackson.MosaikSerializer
import org.ergoplatform.mosaik.model.FetchActionResponse
import org.ergoplatform.mosaik.model.actions.ReloadAction
import org.ergoplatform.mosaik.showDialog
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

const val URL_CONNECT_WALLET = "/mosaik/connect"
const val RELOAD_ACTION_ID = "reload_action"
const val ID_SELECT_WALLET = "ergoAddress"

@RestController
@CrossOrigin
@Hidden
class MosaikCommonController(val mosaikUserService: MosaikUserService) {

    /**
     * connects a wallet and reloads the app from where it was called.
     */
    @PostMapping(URL_CONNECT_WALLET)
    fun connectWallet(
        @RequestBody values: Map<String, *>,
        @RequestHeader headers: Map<String, String>,
        request: HttpServletRequest,
    ): FetchActionResponse {
        val p2PKAddress = values[ID_SELECT_WALLET] as? String
        val context = MosaikSerializer.fromContextHeadersMap(headers)

        val error = p2PKAddress?.let {
            try {
                Address.create(p2PKAddress)
                false
            } catch (t: Throwable) {
                true
            }
        } ?: false

        return if (error) {
            backendResponse(
                1,
                showDialog("Given ergo address $p2PKAddress is invalid.")
            )
        } else {
            mosaikUserService.setP2PKAddress(context.guid, p2PKAddress)

            backendResponse(
                1,
                ReloadAction().apply { id = RELOAD_ACTION_ID })
        }
    }
}


fun isMainNetAddress(address: String): Boolean {
    return try {
        Address.create(address).isMainnet
    } catch (t: Throwable) {
        throw IllegalArgumentException("Invalid address: $address")
    }
}