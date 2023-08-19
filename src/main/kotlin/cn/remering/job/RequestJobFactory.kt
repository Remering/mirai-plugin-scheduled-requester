package cn.remering.job

import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.MiraiLogger
import okhttp3.OkHttpClient
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.simpl.SimpleJobFactory
import org.quartz.spi.TriggerFiredBundle

class RequestJobFactory(
    private val client: OkHttpClient,
    private val botAndGroupIdList: Map<Bot, List<Long>>,
    private val logger: MiraiLogger
): SimpleJobFactory() {
    override fun newJob(bundle: TriggerFiredBundle?, scheduler: Scheduler?): Job {
        val requestJob = super.newJob(bundle, scheduler) as RequestJob
        requestJob.client = client
        requestJob.botAndGroupIdList = botAndGroupIdList
        requestJob.loggger = logger
        return requestJob
    }
}