package cn.chahuyun.doudizhu.util

import net.mamoe.mirai.message.data.ForwardMessage
import net.mamoe.mirai.message.data.RawForwardMessage

/**
 * @param titleGenerator 显示标题
 * @param briefGenerator 消息栏显示简介
 * @param previewGenerator 消息栏预览消息，默认两条
 * @param summarySize 查看消息数量
 * @param summaryGenerator 消息栏总结，查看消息
 * @param sourceGenerator 未知显示
 */
class CustomForwardDisplayStrategy(
    private val titleGenerator:  String =  "群聊的聊天记录" ,
    private val briefGenerator:  String =  "[聊天记录]" ,
    private val previewGenerator:  List<String> = mutableListOf("1:1","2:2"),
    private val summarySize: Int = 10,
    private val summaryGenerator:String = "查看${summarySize}条转发消息",
    private val sourceGenerator:   String =  "聊天记录" ,
) : ForwardMessage.DisplayStrategy {

    override fun generateTitle(forward: RawForwardMessage): String = titleGenerator
    override fun generateBrief(forward: RawForwardMessage): String = briefGenerator
    override fun generateSource(forward: RawForwardMessage): String = sourceGenerator
    override fun generatePreview(forward: RawForwardMessage): List<String> = previewGenerator
    override fun generateSummary(forward: RawForwardMessage): String = summaryGenerator
}