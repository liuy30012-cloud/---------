#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

// 文件路径配置
const POEM_LIBRARY_PATH = path.join(__dirname, '../data/poemLibrary.ts');
const OUTPUT_DIR = path.join(__dirname, '../../public/poems');
const REPORT_PATH = path.join(__dirname, 'classification-report.txt');
const UNKNOWN_AUTHORS_PATH = path.join(__dirname, 'unknown-authors.json');

// 朝代配置
const DYNASTIES = [
  { id: 'pre-tang', name: '先秦至隋', file: 'pre-tang.json' },
  { id: 'tang', name: '唐代', file: 'tang.json' },
  { id: 'song', name: '宋代', file: 'song.json' },
  { id: 'yuan', name: '元代', file: 'yuan.json' },
  { id: 'ming', name: '明代', file: 'ming.json' },
  { id: 'qing', name: '清代', file: 'qing.json' },
  { id: 'modern', name: '近现代', file: 'modern.json' }
];

// 作者-朝代精确映射表
const authorDynastyMap = {
  // 先秦至隋 (先秦-618)
  '屈原': 'pre-tang', '宋玉': 'pre-tang', '贾谊': 'pre-tang',
  '司马相如': 'pre-tang', '扬雄': 'pre-tang', '班固': 'pre-tang',
  '张衡': 'pre-tang', '蔡邕': 'pre-tang', '曹操': 'pre-tang',
  '曹丕': 'pre-tang', '曹植': 'pre-tang', '诸葛亮': 'pre-tang',
  '陶渊明': 'pre-tang', '谢灵运': 'pre-tang', '鲍照': 'pre-tang',
  '谢朓': 'pre-tang', '庾信': 'pre-tang', '江淹': 'pre-tang',
  '刘邦': 'pre-tang', '项羽': 'pre-tang', '刘彻': 'pre-tang',
  '李延年': 'pre-tang', '苏武': 'pre-tang', '李陵': 'pre-tang',
  '王昭君': 'pre-tang', '蔡文姬': 'pre-tang', '左思': 'pre-tang',
  '陆机': 'pre-tang', '潘岳': 'pre-tang', '阮籍': 'pre-tang',
  '嵇康': 'pre-tang', '刘桢': 'pre-tang', '王粲': 'pre-tang',
  '徐干': 'pre-tang', '应玚': 'pre-tang', '刘勰': 'pre-tang',
  '沈约': 'pre-tang', '谢惠连': 'pre-tang', '鲍令晖': 'pre-tang',
  '何逊': 'pre-tang', '吴均': 'pre-tang', '萧统': 'pre-tang',
  '萧纲': 'pre-tang', '萧绎': 'pre-tang', '徐陵': 'pre-tang',
  '庾肩吾': 'pre-tang', '王褒': 'pre-tang', '薛道衡': 'pre-tang',
  '卢思道': 'pre-tang', '杨素': 'pre-tang',

  // 五代十国 (907-960)
  '南唐先主李昪': 'pre-tang', '嗣主璟': 'pre-tang', '后主煜': 'pre-tang',
  '后主衍': 'pre-tang', '李煜': 'pre-tang', '李璟': 'pre-tang',
  '冯延巳': 'pre-tang', '韦庄': 'tang', '花蕊夫人': 'pre-tang',

  // 唐代 (618-907)
  '李白': 'tang', '杜甫': 'tang', '白居易': 'tang', '王维': 'tang',
  '孟浩然': 'tang', '李商隐': 'tang', '杜牧': 'tang', '韩愈': 'tang',
  '柳宗元': 'tang', '刘禹锡': 'tang', '元稹': 'tang', '贾岛': 'tang',
  '温庭筠': 'tang', '李贺': 'tang', '韦应物': 'tang', '岑参': 'tang',
  '高适': 'tang', '王昌龄': 'tang', '王之涣': 'tang', '崔颢': 'tang',

  // 唐代皇帝和皇室
  '太宗皇帝': 'tang', '玄宗皇帝': 'tang', '武则天': 'tang',
  '高宗皇帝': 'tang', '中宗皇帝': 'tang', '睿宗皇帝': 'tang',
  '明皇帝': 'tang', '肃宗皇帝': 'tang', '德宗皇帝': 'tang',
  '文宗皇帝': 'tang', '宣宗皇帝': 'tang', '则天皇后': 'tang',
  '上官昭容': 'tang', '杨贵妃': 'tang', '徐贤妃': 'tang',
  '章怀太子': 'tang', '韩王元嘉': 'tang', '越王贞': 'tang',
  '李隆基': 'tang', '李世民': 'tang',

  // 唐代诗人补充
  '骆宾王': 'tang', '宋之问': 'tang', '沈佺期': 'tang', '陈子昂': 'tang',
  '张九龄': 'tang', '贺知章': 'tang', '张若虚': 'tang', '王勃': 'tang',
  '杨炯': 'tang', '卢照邻': 'tang', '李峤': 'tang', '崔融': 'tang',
  '许敬宗': 'tang', '上官仪': 'tang', '虞世南': 'tang', '褚亮': 'tang',
  '魏征': 'tang', '李百药': 'tang', '李德裕': 'tang', '令狐楚': 'tang',
  '李绅': 'tang', '许浑': 'tang', '李群玉': 'tang', '皮日休': 'tang',
  '陆龟蒙': 'tang', '司空图': 'tang', '罗隐': 'tang', '杜荀鹤': 'tang',
  '郑谷': 'tang', '齐己': 'tang', '张籍': 'tang', '王建': 'tang',
  '李益': 'tang', '卢纶': 'tang', '钱起': 'tang', '戴叔伦': 'tang',
  '韩翃': 'tang', '司空曙': 'tang', '刘长卿': 'tang', '李端': 'tang',
  '储光羲': 'tang', '常建': 'tang', '祖咏': 'tang', '綦毋潜': 'tang',
  '张旭': 'tang', '贺铸': 'tang', '薛涛': 'tang', '鱼玄机': 'tang',
  '李冶': 'tang', '刘采春': 'tang', '崔郊': 'tang', '崔护': 'tang',
  '元结': 'tang', '顾况': 'tang', '戎昱': 'tang', '李涉': 'tang',
  '张继': 'tang', '于鹄': 'tang', '朱庆馀': 'tang', '姚合': 'tang',
  '张祜': 'tang', '杜秋娘': 'tang', '李珣': 'tang', '李存勖': 'tang',
  '孟郊': 'tang', '张说': 'tang', '苏颋': 'tang', '李颀': 'tang',
  '王翰': 'tang', '崔国辅': 'tang',

  // 唐代皇室和后妃补充
  '文德皇后': 'tang', '江妃': 'tang', '萧妃': 'tang',
  '信安王祎': 'tang', '宜芬公主': 'tang', '韩王从善': 'tang',
  '吉王从谦': 'tang',

  // 唐代女诗人和宫廷诗人
  '女学士宋氏若华': 'tang', '尚宫宋氏若昭': 'tang', '尚宫宋氏若宪': 'tang',
  '鲍氏君徽': 'tang', '鲍氏君[徽]微': 'tang', '郎大家宋氏': 'tang',
  '张氏琰': 'tang', '梁氏琰': 'tang', '程氏长文': 'tang',
  '姚氏月华': 'tang', '刘氏云': 'tang', '刘氏媛': 'tang',
  '李季兰': 'tang',

  // 唐代大臣和文人大量补充
  '包佶': 'tang', '卢从愿': 'tang', '刘晃': 'tang', '韩休': 'tang',
  '王晙': 'tang', '崔玄童': 'tang', '贾曾': 'tang', '何鸾': 'tang',
  '蒋挺': 'tang', '源光裕': 'tang', '于邵': 'tang', '姚崇': 'tang',
  '蔡孚': 'tang', '卢怀慎': 'tang', '姜皎': 'tang', '崔日用': 'tang',
  '李乂': 'tang', '裴璀': 'tang', '郭子仪': 'tang', '刘晏': 'tang',
  '郑余庆': 'tang', '郑𬘡': 'tang', '段文昌': 'tang', '牛僧孺': 'tang',
  '李回': 'tang', '夏侯孜': 'tang', '萧倣': 'tang', '李舒': 'tang',
  '徐彦伯': 'tang', '丘说': 'tang', '张齐贤': 'tang', '郑善玉': 'tang',
  '薛稷': 'tang', '徐坚': 'tang', '胡雄': 'tang', '刘子玄': 'tang',
  '员半千': 'tang', '祝钦明': 'tang', '许孟容': 'tang', '陈京': 'tang',
  '冯伉': 'tang', '崔邠': 'tang', '张荐': 'tang', '归登': 'tang',
  '杜羔': 'tang', '李逢吉': 'tang', '孟简': 'tang', '裴度': 'tang',
  '王涯': 'tang', '赵光逢、张衮': 'tang', '崔居俭、卢文纪': 'tang',
  '张昭': 'tang', '刘驾': 'tang', '郑世翼': 'tang', '张循之': 'tang',
  '刘方平': 'tang', '皇甫冉': 'tang', '于𣸣': 'tang', '卢仝': 'tang',
  '庄南杰': 'tang', '翁绶': 'tang', '杨师道': 'tang', '鲍溶': 'tang',
  '贾驰': 'tang', '窦威': 'tang', '张易之': 'tang', '马戴': 'tang',
  '耿[𣲗]': 'tang', '刘济': 'tang', '刘希夷': 'tang', '沈彬': 'tang',
  '乔知之': 'tang', '刘宪': 'tang', '崔湜': 'tang', '韦承庆': 'tang',
  '欧阳瑾': 'tang', '余延寿': 'tang', '长孙佐辅': 'tang', '于武陵': 'tang',
  '郑渥': 'tang', '聂夷中': 'tang', '薛能': 'tang', '秦韬玉': 'tang',
  '纪唐天': 'tang', '王叡': 'tang', '刘眘虚': 'tang', '丁仙芝': 'tang',
  '赵微明': 'tang', '孟云卿': 'tang', '李彦𬀩': 'tang', '梁献': 'tang',
  '董思恭': 'tang', '顾朝阳': 'tang', '东方虬': 'tang', '郭元振': 'tang',
  '张仲素': 'tang', '王偃': 'tang', '张文琮': 'tang', '陈昭': 'tang',
  '王无竞': 'tang', '郑愔': 'tang', '贾至': 'tang', '王适': 'tang',
  '欧阳詹': 'tang', '袁晖': 'tang', '刘商': 'tang', '吴烛': 'tang',
  '朱光弼': 'tang', '朱放': 'tang', '陶翰': 'tang', '李约': 'tang',
  '厉玄': 'tang', '杜𬱟': 'tang', '袁朗': 'tang', '薛奇童': 'tang',
  '张汯': 'tang', '刘元济': 'tang', '李暇': 'tang', '刘义': 'tang',
  '吴少微': 'tang', '雍陶': 'tang', '张修之': 'tang', '裴交泰': 'tang',
  '刘皂': 'tang', '刘言史': 'tang', '李华': 'tang', '齐澣': 'tang',
  '高蟾': 'tang', '严识玄': 'tang', '张烜': 'tang', '王沈': 'tang',
  '王𬤇': 'tang', '柯崇': 'tang', '薛耀': 'tang', '张子容': 'tang',
  '杨巨源': 'tang', '施肩吾': 'tang', '张柬之': 'tang', '阎朝隐': 'tang',
  '杨衡': 'tang', '陈羽': 'tang', '邹绍先': 'tang', '郎士元': 'tang',
  '崔涂': 'tang', '杨凌': 'tang', '武平一': 'tang', '杜审言': 'tang',
  '刘元淑': 'tang', '卢弼': 'tang', '胡曾': 'tang', '王贞白': 'tang',
  '王毂': 'tang', '独孤及': 'tang', '虞羽客': 'tang', '李嶷': 'tang',
  '李廓': 'tang', '郑锡': 'tang', '张炽': 'tang', '辛弘智': 'tang',
  '李康成': 'tang', '雍裕之': 'tang', '张纮': 'tang', '贺兰进明': 'tang',
  '王缙': 'tang',

  // 唐代僧人诗人
  '僧贯休': 'tang', '僧齐己': 'tang', '僧皎然': 'tang', '僧子兰': 'tang',

  // 唐代诗人继续补充
  '吴融': 'tang', '常理': 'tang', '姚系': 'tang', '张彪': 'tang',
  '刘氏瑶': 'tang', '张潮': 'tang', '万楚': 'tang', '王训': 'tang',
  '田娥': 'tang', '李嘉祐': 'tang', '李章': 'tang', '权德舆': 'tang',
  '陆长源': 'tang', '赵嘏': 'tang', '李义府': 'tang', '薛逢': 'tang',
  '盖嘉运': 'tang', '杨敬述进': 'tang', '李景伯': 'tang', '卢贞': 'tang',
  '韩琮': 'tang', '滕潜': 'tang', '吉中孚妻张氏': 'tang',
  '崔液': 'tang', '谢偃': 'tang', '张志和': 'tang', '薛维翰': 'tang',
  '韦渠牟': 'tang', '高骈': 'tang', '陈陶': 'tang', '陈叔达': 'tang',
  '王融': 'tang', '长孙无忌': 'tang', '颜师古': 'tang', '杜淹': 'tang',
  '于志宁': 'tang', '令狐德棻': 'tang', '封行高': 'tang', '杜正伦': 'tang',
  '岑文本': 'tang', '刘洎': 'tang', '褚遂良': 'tang', '杨续': 'tang',
  '刘孝孙': 'tang', '陆敬': 'tang', '沈叔安': 'tang', '何仲宣': 'tang',
  '赵中虚': 'tang', '杨濬': 'tang', '王绩': 'tang', '萧德言': 'tang',
  '崔信明': 'tang', '孔绍安': 'tang', '蔡允恭': 'tang', '杜之松': 'tang',
  '崔善为': 'tang', '朱仲晦': 'tang', '王宏': 'tang', '朱子奢': 'tang',
  '张文收': 'tang', '毛明素': 'tang', '陈子良': 'tang', '庾抱': 'tang',
  '马周': 'tang', '来济': 'tang', '张文恭': 'tang', '薛元超': 'tang',
  '萧翼': 'tang', '欧阳询': 'tang', '阎立本': 'tang', '刘祎之': 'tang',
  '李敬玄': 'tang', '张大安': 'tang', '元万顷': 'tang', '郭正一': 'tang',
  '胡元范': 'tang', '任希古': 'tang', '裴守真': 'tang', '杨思玄': 'tang',
  '王德真': 'tang', '郑义真': 'tang', '萧楚材': 'tang', '薛克构': 'tang',
  '徐珩': 'tang', '贺遂亮': 'tang', '韩思彦': 'tang', '魏求己': 'tang',
  '刘怀一': 'tang', '杜易简': 'tang', '陈元光': 'tang', '许天正': 'tang',
  '许圉师': 'tang', '赵谦光': 'tang', '郑惟忠': 'tang', '张𬸦': 'tang',
  '李福业': 'tang', '薛眘惑': 'tang', '贺敱': 'tang', '狄仁杰': 'tang',
  '魏元忠': 'tang', '李怀远': 'tang', '宗楚客': 'tang', '苏瓌': 'tang',
  '崔涤': 'tang', '王勔': 'tang', '刘允济': 'tang', '邵大震': 'tang',
  '宋璟': 'tang', '苏味道': 'tang', '郭震': 'tang', '田游岩': 'tang',
  '李夔': 'tang', '韦元旦': 'tang', '邵升': 'tang', '唐远悊': 'tang',
  '李适': 'tang', '高正臣': 'tang', '崔知贤': 'tang', '席元明': 'tang',
  '韩仲宣': 'tang', '周彦昭': 'tang', '高球': 'tang', '弓嗣初': 'tang',
  '高瑾': 'tang', '王茂时': 'tang', '徐皓': 'tang', '长孙正隐': 'tang',
  '高绍': 'tang', '郎余令': 'tang', '陈嘉言': 'tang', '周彦晖': 'tang',
  '高峤': 'tang', '刘友贤': 'tang', '周思钧': 'tang', '姜晞': 'tang',
  '徐晶': 'tang', '张敬忠': 'tang', '史俊': 'tang', '武三思': 'tang',
  '张昌宗': 'tang', '薛曜': 'tang', '杨敬述': 'tang', '于季子': 'tang',
  '乔侃': 'tang', '乔备': 'tang', '张均': 'tang', '张垍': 'tang',
  '韦嗣立': 'tang', '魏奉古': 'tang', '崔日知': 'tang', '崔泰之': 'tang',
  '魏知古': 'tang', '卢藏用': 'tang', '岑羲': 'tang', '马怀素': 'tang',
  '富嘉谟': 'tang', '刘知几': 'tang', '丘悦': 'tang', '赵冬曦': 'tang',
  '尹懋': 'tang', '王琚': 'tang', '阴行先': 'tang', '王熊': 'tang',
  '梁知微': 'tang', '李伯鱼': 'tang', '杨重玄': 'tang', '朱使欣': 'tang',
  '袁恕己': 'tang', '刘幽求': 'tang', '章玄同': 'tang', '王易从': 'tang',
  '卢僎': 'tang', '牛凤及': 'tang', '司马逸客': 'tang', '王绍宗': 'tang',
  '郑遂初': 'tang', '李崇嗣': 'tang', '张楚金': 'tang', '房融': 'tang',
  '吕太一': 'tang', '郑蜀宾': 'tang', '宋务光': 'tang', '李行言': 'tang',
  '郭利贞': 'tang', '元希声': 'tang', '李澄之': 'tang', '李如璧': 'tang',
  '洪子舆': 'tang', '寇泚': 'tang', '吴兢': 'tang', '赵彦昭': 'tang',
  '萧至忠': 'tang', '李迥秀': 'tang', '杨廉': 'tang',

  // 五代十国补充
  '孙光宪': 'pre-tang', '皇甫松': 'pre-tang', '孙鲂': 'pre-tang',
  '牛峤': 'pre-tang', '和凝': 'pre-tang', '欧阳炯': 'pre-tang',
  '吴越王钱镠': 'pre-tang', '后王钱俶': 'pre-tang',
  '后蜀嗣主孟昶': 'pre-tang', '闽王王继鹏': 'pre-tang',
  '蜀太后徐氏': 'pre-tang', '蜀太妃徐氏': 'pre-tang',

  // 宋代 (960-1279)
  '苏轼': 'song', '李清照': 'song', '辛弃疾': 'song', '陆游': 'song',
  '欧阳修': 'song', '王安石': 'song', '苏辙': 'song', '苏洵': 'song',
  '黄庭坚': 'song', '秦观': 'song', '周邦彦': 'song', '柳永': 'song',
  '范仲淹': 'song', '晏殊': 'song', '晏几道': 'song', '姜夔': 'song',
  '杨万里': 'song', '范成大': 'song', '文天祥': 'song', '岳飞': 'song',
  '宋徽宗': 'song', '宋太宗': 'song',

  // 宋代诗人补充
  '梅尧臣': 'song', '曾巩': 'song', '张先': 'song', '宋祁': 'song',
  '司马光': 'song', '王令': 'song', '邵雍': 'song', '程颢': 'song',
  '程颐': 'song', '朱熹': 'song', '张载': 'song', '周敦颐': 'song',
  '吕本中': 'song', '曾几': 'song', '尤袤': 'song', '赵师秀': 'song',
  '翁卷': 'song', '徐玑': 'song', '刘克庄': 'song', '刘过': 'song',
  '刘辰翁': 'song', '谢枋得': 'song', '林逋': 'song', '寇准': 'song',
  '钱惟演': 'song', '石延年': 'song', '柳开': 'song', '穆修': 'song',
  '王禹偁': 'song', '潘阆': 'song', '魏野': 'song', '林和靖': 'song',
  '宋庠': 'song', '苏舜钦': 'song', '蔡襄': 'song', '韩琦': 'song',
  '富弼': 'song', '文彦博': 'song', '王珪': 'song', '王雱': 'song',
  '曾布': 'song', '吕惠卿': 'song', '章惇': 'song', '蔡京': 'song',
  '李纲': 'song', '宗泽': 'song', '韩世忠': 'song', '张浚': 'song',
  '张孝祥': 'song', '张元干': 'song', '朱敦儒': 'song', '陈与义': 'song',
  '李之仪': 'song', '贺铸': 'song', '周紫芝': 'song', '叶梦得': 'song',
  '吴潜': 'song', '刘克庄': 'song', '戴复古': 'song', '严羽': 'song',
  '方岳': 'song', '汪元量': 'song', '谢翱': 'song', '郑思肖': 'song',
  '林景熙': 'song', '文及翁': 'song',

  // 元代 (1271-1368)
  '关汉卿': 'yuan', '马致远': 'yuan', '白朴': 'yuan', '郑光祖': 'yuan',
  '王实甫': 'yuan', '张养浩': 'yuan', '睢景臣': 'yuan',
  '乔吉': 'yuan', '张可久': 'yuan', '卢挚': 'yuan', '贯云石': 'yuan',
  '徐再思': 'yuan', '刘致': 'yuan', '王冕': 'yuan', '倪瓒': 'yuan',
  '杨维桢': 'yuan', '萨都剌': 'yuan', '虞集': 'yuan', '揭傒斯': 'yuan',
  '柯九思': 'yuan', '赵孟頫': 'yuan',

  // 明代 (1368-1644)
  '唐寅': 'ming', '文征明': 'ming', '徐渭': 'ming', '杨慎': 'ming',
  '于谦': 'ming', '高启': 'ming', '刘基': 'ming', '宋濂': 'ming',
  '解缙': 'ming', '杨士奇': 'ming', '杨荣': 'ming', '杨溥': 'ming',
  '李东阳': 'ming', '王世贞': 'ming', '李攀龙': 'ming', '谢榛': 'ming',
  '宗臣': 'ming', '梁有誉': 'ming', '徐祯卿': 'ming', '边贡': 'ming',
  '何景明': 'ming', '康海': 'ming', '王九思': 'ming', '李梦阳': 'ming',
  '袁宏道': 'ming', '袁宗道': 'ming', '袁中道': 'ming', '钟惺': 'ming',
  '谭元春': 'ming', '陈子龙': 'ming', '夏完淳': 'ming', '吴伟业': 'ming',
  '钱谦益': 'ming', '冯梦龙': 'ming', '凌濛初': 'ming',

  // 清代 (1644-1912)
  '纳兰性德': 'qing', '曹雪芹': 'qing', '龚自珍': 'qing', '郑燮': 'qing',
  '袁枚': 'qing', '黄景仁': 'qing', '顾炎武': 'qing', '王士祯': 'qing',
  '朱彝尊': 'qing', '陈维崧': 'qing', '吴伟业': 'qing', '钱谦益': 'qing',
  '王夫之': 'qing', '屈大均': 'qing', '查慎行': 'qing', '赵翼': 'qing',
  '蒋士铨': 'qing', '沈德潜': 'qing', '翁方纲': 'qing', '姚鼐': 'qing',
  '方苞': 'qing', '刘大櫆': 'qing', '恽敬': 'qing', '张惠言': 'qing',
  '周济': 'qing', '谭献': 'qing', '况周颐': 'qing', '王国维': 'qing',
  '梁启超': 'qing', '康有为': 'qing', '谭嗣同': 'qing', '黄遵宪': 'qing',
  '丘逢甲': 'qing', '秋瑾': 'qing', '柳亚子': 'qing',

  // 近现代 (1912-)
  '鲁迅': 'modern', '毛泽东': 'modern', '郭沫若': 'modern',
  '徐志摩': 'modern', '闻一多': 'modern', '戴望舒': 'modern',
  '艾青': 'modern', '卞之琳': 'modern', '何其芳': 'modern',
  '冯至': 'modern', '臧克家': 'modern', '田间': 'modern'
};

// 根据作者名识别朝代
function identifyDynasty(author) {
  // 策略1: 精确匹配
  if (authorDynastyMap[author]) {
    return authorDynastyMap[author];
  }

  // 策略2: 模糊匹配 - 皇帝类
  if (author.includes('太宗') || author.includes('高祖')) return 'tang';
  if (author.includes('宋') && author.includes('宗')) return 'song';
  if (author.includes('明') && author.includes('宗')) return 'ming';
  if (author.includes('清') && author.includes('帝')) return 'qing';

  // 策略3: 无法识别
  return 'unknown';
}

// 解析 poemLibrary.ts 文件
function parsePoemLibrary() {
  const content = fs.readFileSync(POEM_LIBRARY_PATH, 'utf-8');
  const arrayMatch = content.match(/export const poemLibrary: PoemEntry\[\] = \[([\s\S]*)\]/);
  if (!arrayMatch) throw new Error('无法解析 poemLibrary.ts 文件');

  const arrayContent = arrayMatch[1];
  const poems = [];
  const poemRegex = /\{\s*title:\s*'([^']+)',\s*author:\s*'([^']+)',\s*poem:\s*'([^']+)'\s*\}/g;
  let match;

  while ((match = poemRegex.exec(arrayContent)) !== null) {
    poems.push({ title: match[1], author: match[2], poem: match[3] });
  }

  console.log(`✓ 成功解析 ${poems.length} 首诗词`);
  return poems;
}

// 将诗词按朝代分类
function classifyPoems(poems) {
  const classified = {};
  const unknown = [];

  DYNASTIES.forEach(d => { classified[d.id] = []; });

  poems.forEach(poem => {
    const dynasty = identifyDynasty(poem.author);
    if (dynasty === 'unknown') {
      unknown.push(poem);
    } else {
      classified[dynasty].push(poem);
    }
  });

  return { classified, unknown };
}

// 生成并保存JSON文件
function generateJSONFiles(classified) {
  const index = { version: '1.0.0', totalCount: 0, dynasties: [] };

  DYNASTIES.forEach(dynasty => {
    const poems = classified[dynasty.id] || [];
    const count = poems.length;

    if (count > 0) {
      const dynastyData = { dynasty: dynasty.id, poems: poems };
      const filePath = path.join(OUTPUT_DIR, dynasty.file);
      fs.writeFileSync(filePath, JSON.stringify(dynastyData, null, 2), 'utf-8');
      console.log(`✓ 生成 ${dynasty.file}: ${count} 首`);
    }

    index.dynasties.push({ id: dynasty.id, name: dynasty.name, count: count, file: dynasty.file });
    index.totalCount += count;
  });

  const indexPath = path.join(OUTPUT_DIR, 'index.json');
  fs.writeFileSync(indexPath, JSON.stringify(index, null, 2), 'utf-8');
  console.log(`✓ 生成 index.json`);

  return index;
}

// 生成分类报告
function generateReport(index, unknown) {
  const totalClassified = index.totalCount;
  const totalUnknown = unknown.length;
  const totalPoems = totalClassified + totalUnknown;
  const classifiedPercent = ((totalClassified / totalPoems) * 100).toFixed(1);

  let report = '诗词分类报告\n=============\n';
  report += `生成时间: ${new Date().toLocaleString('zh-CN')}\n`;
  report += `总计: ${totalPoems} 首\n\n`;
  report += `已分类: ${totalClassified} 首 (${classifiedPercent}%)\n`;

  index.dynasties.forEach(d => {
    if (d.count > 0) report += `  - ${d.name}: ${d.count} 首\n`;
  });

  report += `\n待审核: ${totalUnknown} 首 (${((totalUnknown / totalPoems) * 100).toFixed(1)}%)\n`;

  fs.writeFileSync(REPORT_PATH, report, 'utf-8');
  console.log(`\n${report}`);

  if (totalUnknown > 0) {
    const unknownAuthors = {};
    unknown.forEach(poem => {
      if (!unknownAuthors[poem.author]) unknownAuthors[poem.author] = [];
      unknownAuthors[poem.author].push(poem.title);
    });
    fs.writeFileSync(UNKNOWN_AUTHORS_PATH, JSON.stringify(unknownAuthors, null, 2), 'utf-8');
  }
}

// 主函数
function main() {
  console.log('开始分类诗词...\n');
  const poems = parsePoemLibrary();
  const { classified, unknown } = classifyPoems(poems);
  const index = generateJSONFiles(classified);
  generateReport(index, unknown);
  console.log('\n✓ 分类完成!');
}

main();
