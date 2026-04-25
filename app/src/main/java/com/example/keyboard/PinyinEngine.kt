package com.example.keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.LinkedHashMap

object PinyinEngine {
    
    // 使用 LinkedHashMap 从而能在后续追加的时候，一定程度上保持基于频率排序的插入顺序
    private val dictMap = LinkedHashMap<String, MutableList<String>>()
    private val singleCharDictMap = LinkedHashMap<String, MutableList<String>>()
    @Volatile
    var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        Thread {
            try {
                // 加载我们全新生成的基于 Jieba 分词的高频精准词库！包含几万个按使用频率排序的最常见的中国词汇
                loadHighQualityDictionary(context)
                // 加载基础单字库 (Luna Pinyin)，作为补底
                loadLunaPinyin(context)
                
                isInitialized = true
                Log.d("PinyinEngine", "Loaded! group=${dictMap.size}, single=${singleCharDictMap.size}")
                Log.d("PinyinEngine", "DEBUG sm: ${dictMap["sm"]?.take(5)}")
                Log.d("PinyinEngine", "DEBUG xianzai: ${dictMap["xianzai"]?.take(5)}")
                Log.d("PinyinEngine", "DEBUG shenme: ${dictMap["shenme"]?.take(5)}")
            } catch (e: Exception) {
                Log.e("PinyinEngine", "读取词库失败", e)
            }
        }.start()
    }

    private fun loadHighQualityDictionary(context: Context) {
        val inputStream = context.assets.open("final_pinyin_dict.txt")
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = reader.readLine()
        var lineCount = 0

        while (line != null && lineCount < 150000) {
            // 格式：北京:běi jīng:34488 (词语:声调拼音:使用频率)
            val parts = line.split(":")
            if (parts.size >= 2) {
                val hanzi = parts[0].trim()
                val pinyinWithTones = parts[1].trim()
                
                val cleanPinyin = stripTones(pinyinWithTones)
                val fullPinyinKey = cleanPinyin.replace(" ", "")
                
                val existing = dictMap.getOrPut(fullPinyinKey) { mutableListOf() }
                // 允许放多达 25 个候选词组（因为按照高频排序，前面的肯定最准）
                if (!existing.contains(hanzi) && existing.size < 25) {
                    existing.add(hanzi)
                }

                // 自动生成首字母缩写。如 "bei jing" -> "bj"
                val pinyinWords = cleanPinyin.split(" ")
                if (pinyinWords.size > 1 && pinyinWords.size <= 6) { // 限制超长组合的首拼
                    val initialsKey = pinyinWords.mapNotNull { it.firstOrNull() }.joinToString("")
                    if (initialsKey != fullPinyinKey) {
                        val initialsExisting = dictMap.getOrPut(initialsKey) { mutableListOf() }
                        // 对于简单的首拼（如 bj），我们只收入最高频的前 25 个！避免垃圾词汇覆盖好词
                        if (!initialsExisting.contains(hanzi) && initialsExisting.size < 25) {
                            initialsExisting.add(hanzi)
                        }
                    }
                }
            }
            line = reader.readLine()
            lineCount++
        }
        reader.close()
    }

    private fun loadLunaPinyin(context: Context) {
        val inputStream = context.assets.open("luna_pinyin.dict.yaml")
        val reader = BufferedReader(InputStreamReader(inputStream))
        var inDataSection = false
        var line: String? = reader.readLine()

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed == "...") {
                inDataSection = true
                line = reader.readLine()
                continue
            }
            
            if (inDataSection && trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split("\t")
                if (parts.size >= 2) {
                    val hanzi = parts[0]
                    if (hanzi.length == 1) { // 只提取单字补底
                        val pinyinKey = parts[1].replace(" ", "").lowercase()
                        val existing = singleCharDictMap.getOrPut(pinyinKey) { mutableListOf() }
                        if (!existing.contains(hanzi) && existing.size < 30) {
                            existing.add(hanzi)
                        }
                    }
                }
            }
            line = reader.readLine()
        }
        reader.close()
    }

    private fun stripTones(pinyin: String): String {
        val replacedV = pinyin.replace("ü", "v").replace("Ü", "v")
        val normalized = Normalizer.normalize(replacedV, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }

    suspend fun fetchCandidates(pinyin: String): List<String> = withContext(Dispatchers.Default) {
        val normalized = pinyin.lowercase().trim()
        if (normalized.isEmpty()) return@withContext emptyList()

        // 核心修复：如果是刚启动时用户就打字了，词典可能还在加载中
        // 我们不能直接返回 emptyList()，否则 UI 就死锁在暂无词库了
        // 应该用挂起函数等待它加载完成！
        var waitCount = 0
        while (!isInitialized && waitCount < 50) { // 最多等 5 秒
            kotlinx.coroutines.delay(100)
            waitCount++
        }

        if (!isInitialized) return@withContext listOf("词库加载中或失败...")

        val results = mutableListOf<String>()

        // 【新增神级优化】：前缀模糊匹配补充
        val exactWords = dictMap[normalized]
        if (exactWords != null && exactWords.isNotEmpty()) {
            results.addAll(exactWords)
        } else {
            // 没有精准匹配（比如用户刚敲了 xian，准备打 xianzai）
            // 我们去最高频的 key 里，收集最多 30 个能以此作为前缀的汉字推荐
            var addedCount = 0
            for (entry in dictMap) {
                if (entry.key.startsWith(normalized)) {
                    for (word in entry.value) {
                        if (!results.contains(word)) {
                            results.add(word)
                            addedCount++
                        }
                        if (addedCount >= 30) break
                    }
                }
                if (addedCount >= 30) break
            }
        }

        // 2. 长句子的智能断词匹配 (Greedy Split)
        if (normalized.length >= 2) {
            val smartSentence = greedySplit(normalized)
            if (smartSentence.isNotEmpty()) {
                val sentenceStr = smartSentence.joinToString("")
                if (!results.contains(sentenceStr) && sentenceStr != normalized) {
                    if (results.isEmpty()) {
                        results.add(sentenceStr)
                    } else {
                        // 如果有词，把长句排进前三（仅次于最热门的组词）
                        results.add(1.coerceAtMost(results.size), sentenceStr) 
                        results.add("【断词参考】" + smartSentence.joinToString("'"))
                    }
                }
            }
        }

        // 3. 补底加上单字的匹配 (如果只打了 n，把 n 的单字排在词语后面)
        val singleChars = singleCharDictMap[normalized]
        if (singleChars != null) {
            for (char in singleChars) {
                if (!results.contains(char)) {
                    results.add(char)
                }
            }
        }

        if (results.isNotEmpty()) {
            return@withContext results.take(40) // 最多展示 40 个候选字
        }

        return@withContext listOf("暂无词库: $pinyin")
    }

    private fun greedySplit(pinyin: String): List<String> {
        var remaining = pinyin
        val resultWords = mutableListOf<String>()

        while (remaining.isNotEmpty()) {
            var matchFound = false
            for (i in remaining.length downTo 1) {
                val prefix = remaining.substring(0, i)
                val candidates = dictMap[prefix]
                
                if (candidates != null && candidates.isNotEmpty()) {
                    resultWords.add(candidates.first()) 
                    remaining = remaining.substring(i) 
                    matchFound = true
                    break
                }
            }
            if (!matchFound) {
                // 作为回退，如果连单字都切不出来，强行切最后一个单字
                val singlechar = singleCharDictMap[remaining.substring(0, 1)]
                if(singlechar != null && singlechar.isNotEmpty()){
                    resultWords.add(singlechar.first())
                    remaining = remaining.substring(1)
                }else{
                    resultWords.add(remaining)
                    break
                }
            }
        }
        return resultWords
    }
}