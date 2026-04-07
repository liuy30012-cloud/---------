export interface FoodItem {
  id: string
  icon: string
  nameZh: string
  nameEn: string
  hungerRestore: number
  moodRestore: number
  affinityBonus: number
  descZh: string
  descEn: string
}

export const FOODS: FoodItem[] = [
  {
    id: 'coffee',
    icon: '☕',
    nameZh: '阅览室咖啡',
    nameEn: 'Library Coffee',
    hungerRestore: 15,
    moodRestore: 5,
    affinityBonus: 1,
    descZh: '图书馆自习室的免费咖啡~',
    descEn: 'Free coffee from the library study room~',
  },
  {
    id: 'tea',
    icon: '🍵',
    nameZh: '馆长的茶',
    nameEn: "Librarian's Tea",
    hungerRestore: 20,
    moodRestore: 10,
    affinityBonus: 2,
    descZh: '馆长办公室里的好茶，偷偷喝一口~',
    descEn: "Fine tea from the head librarian's office~",
  },
  {
    id: 'fish',
    icon: '🐟',
    nameZh: '食堂小鱼干',
    nameEn: 'Cafeteria Fish',
    hungerRestore: 25,
    moodRestore: 15,
    affinityBonus: 2,
    descZh: '学院食堂带过来的~ 猫最爱！',
    descEn: 'From the school cafeteria~ Cat favorite!',
  },
  {
    id: 'cookie',
    icon: '🍪',
    nameZh: '书本饼干',
    nameEn: 'Book Cookie',
    hungerRestore: 20,
    moodRestore: 12,
    affinityBonus: 2,
    descZh: '书形饼干！知识的味道~',
    descEn: 'Book-shaped cookie! Tastes like knowledge~',
  },
  {
    id: 'cake',
    icon: '🎂',
    nameZh: '图书馆纪念蛋糕',
    nameEn: 'Library Anniversary Cake',
    hungerRestore: 35,
    moodRestore: 25,
    affinityBonus: 5,
    descZh: '图书馆建馆纪念日的蛋糕！限定款！',
    descEn: 'Library anniversary cake! Limited edition!',
  },
]
