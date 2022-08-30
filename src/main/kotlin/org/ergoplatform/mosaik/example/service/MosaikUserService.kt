package org.ergoplatform.mosaik.example.service

import org.ergoplatform.explorer.client.model.Balance
import org.ergoplatform.mosaik.example.db.MosaikUser
import org.ergoplatform.mosaik.example.db.MosaikUserRepo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MosaikUserService(
    val mosaikUserRepo: MosaikUserRepo,
    val explorerApiService: ExplorerApiService,
) {
    private val cachedBalances = HashMap<String, Balance>()
    private val TOKEN_SEPARATOR = ","

    fun getMosaikUser(guid: String): MosaikUser? =
        mosaikUserRepo.findByMosaikUuid(guid).orElse(null)

    @Transactional
    fun setP2PKAddress(guid: String, p2PKAddress: String?) {
        val user = getMosaikUser(guid)
        mosaikUserRepo.save(MosaikUser(user?.id ?: 0, guid, p2PKAddress, user?.tokensList))
    }

    @Transactional
    fun setUserTokenList(guid: String, tokens: List<String>) {
        val user = getMosaikUser(guid)
        mosaikUserRepo.save(
            MosaikUser(
                user?.id ?: 0,
                guid,
                user?.p2PKAddress,
                tokens.joinToString(TOKEN_SEPARATOR)
            )
        )
    }

    fun getUserTokenList(mosaikUser: MosaikUser): List<String> =
        mosaikUser.tokensList?.split(TOKEN_SEPARATOR) ?: emptyList()

    fun getBalanceForUser(guid: String): Balance? {
        return getMosaikUser(guid)?.p2PKAddress?.let { p2PKAddress ->
            val cachedBalance = synchronized(cachedBalances) {
                cachedBalances[p2PKAddress]
            }

            if (cachedBalance == null) {
                val freshBalance = explorerApiService.getBalance(
                    !p2PKAddress.startsWith('3'),
                    p2PKAddress,
                )?.confirmed

                freshBalance?.let {
                    synchronized(cachedBalances) {
                        cachedBalances[p2PKAddress] = freshBalance
                    }
                }

                freshBalance
            } else cachedBalance
        }
    }

    @Scheduled(fixedRate = 60 * 1000L)
    fun removeCache() {
        synchronized(cachedBalances) {
            cachedBalances.clear()
        }
    }

}