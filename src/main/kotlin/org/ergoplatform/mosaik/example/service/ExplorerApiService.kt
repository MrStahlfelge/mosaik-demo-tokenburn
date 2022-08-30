package org.ergoplatform.mosaik.example.service

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.explorer.client.DefaultApi
import org.ergoplatform.explorer.client.model.OutputInfo
import org.springframework.stereotype.Service
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

@Service
class ExplorerApiService(private val okHttpClient: OkHttpClient) {
    val timeout = 30L // 30 seconds since Explorer can be slooooow

    private val api by lazy {
        buildExplorerApi(RestApiErgoClient.defaultMainnetExplorerUrl)
    }

    private val testapi by lazy {
        buildExplorerApi(RestApiErgoClient.defaultTestnetExplorerUrl)
    }

    private fun buildExplorerApi(url: String) = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            okHttpClient.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS).build()
        )
        .build()
        .create(DefaultApi::class.java)

    fun getBoxesByTokenId(tokenId: String, offset: Int, limit: Int): List<OutputInfo> =
        wrapCall { api.getApiV1BoxesUnspentBytokenidP1(tokenId, offset, limit) }.items

    private fun <T> wrapCall(call: () -> Call<T>): T {
        val explorerCall = call().execute()

        if (!explorerCall.isSuccessful)
            throw IOException("Error calling Explorer: ${explorerCall.errorBody()}")

        return explorerCall.body()!!
    }

    fun getBoxesByAddress(mainnet: Boolean, address: String, offset: Int, limit: Int, ascending: Boolean) =
        wrapCall {
            (if (mainnet) api else testapi)
                .getApiV1BoxesUnspentByaddressP1(address, offset, limit, if (ascending) "asc" else "desc")
        }.items

    fun getBalance(mainnet: Boolean, address: String) =
        wrapCall {
            (if (mainnet) api else testapi)
                .getApiV1AddressesP1BalanceTotal(address)
        }

    fun getBoxInformation(mainnet: Boolean, boxId: String) =
        wrapCall {
            (if (mainnet) api else testapi).getApiV1BoxesP1(boxId)
        }

    fun getTransactionInfo(mainnet: Boolean, txId: String) =
            wrapCall {
                (if (mainnet) api else testapi).getApiV1TransactionsP1(txId)
            }

}
