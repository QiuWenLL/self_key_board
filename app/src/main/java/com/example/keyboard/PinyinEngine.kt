package com.example.keyboard

object PinyinEngine {
    // 这是一个稍微扩充的本地词库引擎演示
    // 在真正的输入法中，这里通常会读取一个 SQLite 数据库或者解析一个几 MB 大小的 Trie 树文件。
    private val dict = mapOf(
        // 单字母简拼
        "n" to listOf("你", "那", "男", "能"),
        "h" to listOf("好", "和", "很", "还"),
        "w" to listOf("我", "万", "为", "无"),
        "m" to listOf("们", "没", "吗", "买"),
        "z" to listOf("在", "这", "中", "组"),
        "c" to listOf("词", "才", "错", "吃"),
        
        // 全拼
        "ni" to listOf("你", "泥", "拟"),
        "hao" to listOf("好", "号", "毫"),
        "wo" to listOf("我", "握", "窝"),
        "men" to listOf("们", "门", "闷"),
        "zu" to listOf("组", "租", "足"),
        "ci" to listOf("词", "次", "此"),

        // 组词：全拼组词
        "nihao" to listOf("你好", "泥好"),
        "women" to listOf("我们", "我门"),
        "zuoci" to listOf("组词", "做次"),

        // 组词：首字母简拼 (输入法非常重要的高频特性)
        "nh" to listOf("你好", "南航", "内含"),
        "wm" to listOf("我们", "外卖", "完美", "网民"),
        "zc" to listOf("组词", "正常", "支持", "总裁")
    )

    fun getCandidates(pinyin: String): List<String> {
        val normalized = pinyin.lowercase()
        // 1. 尝试直接获取词库的精准匹配 (简拼或全拼)
        val exactMatches = dict[normalized]
        if (exactMatches != null) {
            return exactMatches
        }

        // 2. 真正的输入法在这里会进行【拼音拆分】算法
        // 例如用户输入了 "niha" 词库没有，算法会拆解成 "ni" + "ha" 或者 "n" + "i" + "h" + "a" 
        // 并在字库中利用隐马尔可夫模型(HMM)计算概率最高的组词。
        // 这里作为演示，一旦超纲，我们给出提示。
        return listOf("词库暂无: $pinyin")
    }
}