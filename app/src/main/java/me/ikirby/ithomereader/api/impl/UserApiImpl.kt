package me.ikirby.ithomereader.api.impl

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import me.ikirby.ithomereader.api.UserApi
import me.ikirby.ithomereader.network.ITHomeApi
import java.io.IOException

object UserApiImpl : UserApi {
    override fun login(username: String, password: String): Deferred<String?> = GlobalScope.async {
        return@async try {
            ITHomeApi.login(username, password)
        } catch (e: IOException) {
            null
        }
    }

}