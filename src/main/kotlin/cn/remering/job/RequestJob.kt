package cn.remering.job

import cn.remering.PluginConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.quartz.Job
import org.quartz.JobExecutionContext

class RequestJob: Job {

    lateinit var client: OkHttpClient
    lateinit var botAndGroupIdList: Map<Bot, List<Long>>
    lateinit var loggger: MiraiLogger

    override fun execute(context: JobExecutionContext?) {
        if (!::client.isInitialized) {
            loggger.info {
                "No request client initialized"
            }
            return
        }

        if (botAndGroupIdList.isEmpty()) {
            loggger.info {
                "No message will be sent because no online bot which id in ${PluginConfig.botIdAndGroupIdList.keys}"
            }
            return
        }

        var headerBuilder = Headers.Builder()
        for ((k, v) in PluginConfig.requestHeader) {
            headerBuilder = headerBuilder.add(k, v)
        }
        client.newCall(
            Request.Builder()
                .headers(headerBuilder.build())
                .url(PluginConfig.url)
                .post(PluginConfig.requestBody.toRequestBody())
                .build()
        ).execute().use {
            val text = it.body?.string()
            if (text == null) {
                loggger.info {
                    "No message will be sent because no response body received from ${PluginConfig.url}"
                }
                return
            }
            botAndGroupIdList
                .filter { (bot, _) -> bot.isOnline }
                .filter { (_, groupList) -> groupList.isNotEmpty() }
                .forEach { (bot, groupList) ->
                    groupList.mapNotNull { groupId -> bot.getGroup(groupId) }
                        .forEach {
                            GlobalScope.launch {
                                it.sendMessage(text)
                            }
                        }

                }
        }

    }
}