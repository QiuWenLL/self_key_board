import jieba
from pypinyin import pinyin, Style

print('Loading Jieba Dictionary...')
jieba.initialize()

words = jieba.dt.FREQ.items()
print(f'Total words in Jieba: {len(words)}')

# Filter out non-Chinese and single chars to avoid trash words mapped as phrases
valid_words = []
for word, freq in words:
    # only pure Chinese phrases length >= 2
    if len(word) >= 1 and all('\u4e00' <= c <= '\u9fff' for c in word):
        valid_words.append((word, freq))

# Sort by frequency descending
valid_words.sort(key=lambda x: x[1], reverse=True)

# Generate pinyin for all
print('Generating Pinyin...')
with open('app/src/main/assets/final_pinyin_dict.txt', 'w', encoding='utf-8') as f:
    count = 0
    for word, freq in valid_words:
        py = pinyin(word, style=Style.TONE) # gives e.g. [['xiàn'], ['zài']]
        py_str = ' '.join([item[0] for item in py])
        f.write(f'{word}:{py_str}:{freq}\n')
        count += 1

print(f'Done! Created dictionary with {count} Chinese words.')
