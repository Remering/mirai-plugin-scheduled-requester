package cn.remering

import cn.remering.job.RequestJob
import cn.remering.job.RequestJobFactory
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.utils.info
import okhttp3.OkHttpClient
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Files
import java.util.concurrent.TimeUnit

object ScheduledRequester : KotlinPlugin(
    JvmPluginDescription(
        id = "cn.remering",
        name = "ScheduledRequester",
        version = "0.1.0",
    ) {
        author("Remering")
    }
) {

    private val botAndGroupIdMap = mutableMapOf<Bot, List<Long>>()

    private lateinit var scheduler: Scheduler

    private lateinit var client: OkHttpClient

    private lateinit var botOnlineListener: Listener<BotOnlineEvent>
    private lateinit var botOfflineListener: Listener<BotOfflineEvent>

    private fun initScheduler() {
        val originQuartzPropertiesFile = System.getProperty(StdSchedulerFactory.PROPERTIES_FILE)
        val targetQuartzPropertiesPath = configFolder.toPath().resolve(PluginConfig.quartzProperties).toAbsolutePath()
        System.setProperty(StdSchedulerFactory.PROPERTIES_FILE, targetQuartzPropertiesPath.toString())
        if (!Files.exists(targetQuartzPropertiesPath)) {
            logger.info {
                "No quartz config properties found at $targetQuartzPropertiesPath, copy default config properties for you"
            }
            val classLoader = StdSchedulerFactory::class.java.classLoader

            Files.copy(
                classLoader.getResourceAsStream("quartz.properties")?:
                classLoader.getResourceAsStream("/quartz.properties")?:
                classLoader.getResourceAsStream("org/quartz/quartz.properties")?:
                throw RuntimeException(
                    "Default quartz.properties not found in class path"
                ),
                targetQuartzPropertiesPath
            )
        } else {
            logger.info {
                "Found quartz config properties at $targetQuartzPropertiesPath"
            }
        }

        scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.setJobFactory(RequestJobFactory(client, botAndGroupIdMap, logger))

        if (originQuartzPropertiesFile != null) {
            System.setProperty(StdSchedulerFactory.PROPERTIES_FILE, originQuartzPropertiesFile)
        } else {
            System.clearProperty(StdSchedulerFactory.PROPERTIES_FILE)
        }
    }

    private fun initClient() {
        client = OkHttpClient.Builder()
            .connectTimeout(PluginConfig.requestConnectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(PluginConfig.requestReadTimeout, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun schedule() {

        var trigger: Trigger? = null
        try {
            trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(PluginConfig.cron))
                .build()
        } catch (runtimeException: RuntimeException) {
            logger.info {
                runtimeException.message
            }
            onDisable()
        }
        if (trigger != null) {
            val job = JobBuilder.newJob(RequestJob::class.java)
                .build()
            scheduler.scheduleJob(job, trigger)
            scheduler.start()

        }
    }

    override fun onEnable() {
        PluginConfig.reload()
        initClient()
        initScheduler()
        schedule()

        botOnlineListener = GlobalEventChannel.subscribe<BotOnlineEvent> {
            if (bot.id in PluginConfig.botIdAndGroupIdList.keys) {
                botAndGroupIdMap += Pair(bot, PluginConfig.botIdAndGroupIdList[bot.id]!!)
            }
            ListeningStatus.LISTENING
        }

        botOfflineListener = GlobalEventChannel.subscribe<BotOfflineEvent> {
            if (bot in botAndGroupIdMap) {
                botAndGroupIdMap -= bot
            }
            ListeningStatus.LISTENING
        }
        logger.info { "Plugin enabled" }
    }

    override fun onDisable() {
        if (this::scheduler.isInitialized) {
            scheduler.shutdown()
        }
        if (::botOnlineListener.isInitialized) {
            botOnlineListener.complete()
        }
        if (::botOfflineListener.isInitialized) {
            botOfflineListener.complete()
        }
        logger.info("Plugin disabled")
    }
}


object PluginConfig: AutoSavePluginConfig("scheduled-requester") {
    val botIdAndGroupIdList by value<Map<Long, List<Long>>>(mapOf())
    val quartzProperties by value<String>("quartz.properties")
    val url by value<String>("http://localhost:8080/getLuckyPlayer")
    val requestConnectTimeout by value<Long>(1000L)
    val requestReadTimeout by value<Long>(1000L)
    val requestHeader by value<Map<String, String>>()
    val requestBody by value<String>()
    val cron by value<String>()
}