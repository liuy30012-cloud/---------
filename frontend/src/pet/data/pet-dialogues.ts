export interface DialogueEntry {
  zh: string
  en: string
}

export interface DynamicDialogue {
  zh: (data: any) => string
  en: (data: any) => string
}

// ============ Static dialogues ============

export const greetings: DialogueEntry[] = [
  { zh: '喵~ 欢迎来到图书馆！鸡蛋仔今天随时待命~', en: 'Meow~ Welcome to the library! Jidanzai is on duty~' },
  { zh: '今天想找什么书呢？鸡蛋仔帮你~', en: 'What book are you looking for? Let me help~' },
  { zh: '嗨！又见面了~ 📚', en: 'Hey! Good to see you again~ 📚' },
]

export const searchStart: DialogueEntry[] = [
  { zh: '让鸡蛋仔帮你找找~🔍', en: 'Let me search for you~🔍' },
  { zh: '等一下，我去书架看看！', en: 'Hold on, checking the shelves!' },
  { zh: '收到！鸡蛋仔出发找书~', en: 'Roger! Jidanzai searching~' },
]

export const searchEmpty: DialogueEntry[] = [
  { zh: '翻了个遍也没找到...换个关键词试试？', en: 'Searched everywhere... Try different keywords?' },
  { zh: '呜...这本书好像不在我们馆...', en: 'Hmm... This book might not be in our library...' },
  { zh: '没找到诶😿 试试用ISBN号搜？', en: "Couldn't find it😿 Try searching by ISBN?" },
]

export const idleChat: DialogueEntry[] = [
  { zh: '你知道吗？中图分类法把书分成22大类哦~', en: 'Fun fact: Chinese Library Classification has 22 main categories~' },
  { zh: '小贴士：用ISBN号搜索最精准！', en: 'Tip: Searching by ISBN is the most accurate!' },
  { zh: '鸡蛋仔最喜欢在书架之间溜达了~', en: 'Jidanzai loves strolling between bookshelves~' },
  { zh: '图书馆的味道...是知识的味道~📖', en: 'The smell of the library... is the scent of knowledge~📖' },
  { zh: '偷偷告诉你，三楼文学区的角落很舒服哦~', en: 'Secret: the corner in 3F Literature section is super cozy~' },
  { zh: '鸡蛋仔今天已经帮好多人找到书了！', en: 'Jidanzai helped so many people find books today!' },
  { zh: '好安静...大家都在认真看书呢~', en: "So quiet... everyone's reading carefully~" },
  { zh: '如果找不到书，可以问鸡蛋仔哦！', en: "Can't find a book? Just ask Jidanzai!" },
]

export const feedReactions: DialogueEntry[] = [
  { zh: '好好吃！谢谢主人！❤️', en: 'So delicious! Thank you! ❤️' },
  { zh: '啊呜~太美味了喵！', en: 'Nom~ So yummy meow!' },
  { zh: '吃饱了有力气帮你找书了！💪', en: 'Full belly = energy to find books! 💪' },
]

export const sleepyChat: DialogueEntry[] = [
  { zh: '呼...好困...要打个盹了...', en: 'Yawn... so sleepy... taking a nap...' },
  { zh: '鸡蛋仔要睡一会...有事叫我哦~', en: 'Jidanzai napping... wake me if needed~' },
]

export const dragReactions: DialogueEntry[] = [
  { zh: '喵呜！！放我下来~', en: 'Meow!! Put me down~' },
  { zh: '好高好高！怕怕！', en: "So high! I'm scared!" },
  { zh: '鸡蛋仔不是玩具啦！', en: "Jidanzai isn't a toy!" },
]

export const offlineChat: DialogueEntry[] = [
  { zh: '后端连不上了...鸡蛋仔先用记忆帮你找~', en: "Backend's down... Using cached data to help~" },
  { zh: '网络断了，但鸡蛋仔还在！本地数据可以看~', en: 'Network lost, but Jidanzai is here! Local data available~' },
]

export const onlineRestored: DialogueEntry[] = [
  { zh: '网络恢复了！搜索数据是最新的✨', en: 'Network restored! Search data is fresh✨' },
]

export const langSwitch: DialogueEntry[] = [
  { zh: 'Switching! 鸡蛋仔 can speak English too~', en: '切换到中文啦~ 鸡蛋仔也会说中文哦！' },
]

export const checkedOutReaction: DialogueEntry[] = [
  { zh: '这本被别人借走了...要不要设置到馆提醒？', en: 'This one is checked out... Want a return notification?' },
  { zh: '啊，已借出...鸡蛋仔帮你关注归还动态~', en: "Ah, checked out... I'll watch for its return~" },
]

export const actionChat: Record<string, DialogueEntry[]> = {
  gaming: [
    { zh: '通关啦！鸡蛋仔可是游戏高手~🎮', en: 'Level complete! Jidanzai is a pro~🎮' },
    { zh: '这个Boss怎么这么难打...再来一局！', en: 'This boss is so tough... one more game!' }
  ],
  dancing: [
    { zh: '左三圈，右三圈，脖子扭扭，屁股扭扭~🎵', en: 'Left, right, twist and shout~🎵' },
    { zh: '鸡蛋仔的舞姿是不是很可爱？✨', en: 'Is my dancing cute? ✨' }
  ],
  magic: [
    { zh: '巴啦啦能量！...变出你要的书！🔮', en: 'Balala energy!... Conjuring your book! 🔮' },
    { zh: '让我算一算...你要的书在东北方！', en: 'Let me predict... your book is to the northeast!' }
  ],
  angry: [
    { zh: '喵呜！鸡蛋仔生气了，哄不好的那种！💢', en: 'Meow! Jidanzai is angry, very angry! 💢' },
    { zh: '不要总是揪我的毛啦！', en: 'Stop pulling my fur!' }
  ]
}

// ============ Dynamic (template) dialogues ============

export const searchResultDynamic: DynamicDialogue = {
  zh: (data: { title: string; location: string; count: number }) =>
    data.count === 1
      ? `找到了！《${data.title}》在 ${data.location}！`
      : `找到了！一共${data.count}本~ 《${data.title}》在 ${data.location}！`,
  en: (data: { title: string; location: string; count: number }) =>
    data.count === 1
      ? `Found it! "${data.title}" is at ${data.location}!`
      : `Found ${data.count} books! "${data.title}" is at ${data.location}!`,
}

export const historyRecall: DynamicDialogue = {
  zh: (data: { keyword: string; count: number }) =>
    `上次你搜了「${data.keyword}」找到了${data.count}条，还需要继续找吗？`,
  en: (data: { keyword: string; count: number }) =>
    `You searched "${data.keyword}" last time (${data.count} results). Continue?`,
}

export const notifDynamic: DynamicDialogue = {
  zh: (data: { title: string }) => `📬 新通知：${data.title}`,
  en: (data: { title: string }) => `📬 New notice: ${data.title}`,
}

// Category comments
export const categoryComments: Record<string, DialogueEntry> = {
  Technology: { zh: '科技类的书！代码的世界真奇妙~', en: 'Tech books! The world of code is fascinating~' },
  Literature: { zh: '文学作品~ 这种书要慢慢品味哦~', en: 'Literature~ Take your time savoring it~' },
  Art: { zh: '艺术类！翻书的时候轻一点~', en: 'Art books! Turn pages gently~' },
}

// Language comments
export const langComments: Record<string, DialogueEntry> = {
  '中文 (Chinese)': { zh: '中文书，读起来最舒服了~', en: 'Chinese books feel most natural~' },
  English: { zh: '英文原版！Looks like a good read~📚', en: 'English original! Looks like a good read~📚' },
  Latin: { zh: '拉丁文...这本很古老呢', en: 'Latin... This is very ancient' },
  Deutsch: { zh: '德文书！Guten Tag~', en: 'German book! Guten Tag~' },
}

// ============ Time-based greetings (Dizhi) ============
export const dizhiGreetings: Record<string, DialogueEntry> = {
  zi:   { zh: '深夜了还在查资料？注意身体呀~', en: "Late night research? Take care of yourself~" },
  chou: { zh: '这个点图书馆自习室都锁了...休息吧', en: "Study rooms are locked now... Get some rest" },
  yin:  { zh: '💤 (呼噜声)', en: '💤 (snoring)' },
  mao:  { zh: '天亮了...图书馆还没开呢~', en: "Dawn... Library isn't open yet~" },
  chen: { zh: '早上好！图书馆要开门了~ 📖', en: "Good morning! Library's opening soon~ 📖" },
  si:   { zh: '上午是借阅高峰期，热门书抓紧哦~', en: 'Mornings are busy! Popular books go fast~' },
  wu:   { zh: '午休时间~ 借阅处可能暂停服务~', en: 'Lunch break~ Check-out may pause~' },
  wei:  { zh: '午后最适合安静阅读了~', en: 'Perfect afternoon for quiet reading~' },
  shen: { zh: '下午还有几小时闭馆~', en: 'Few hours before closing~' },
  you:  { zh: '快闭馆了！要借书赶紧哦~', en: 'Closing soon! Hurry if borrowing~' },
  xu:   { zh: '图书馆关门了...线上查阅随时可以~', en: "Library's closed... Online search anytime~" },
  hai:  { zh: '在线查找好书最惬意~ 不用排队🌙', en: 'Finding books online is cozy~ No queues🌙' },
}

// Helper to pick random from array
export function pickRandom<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}
