// 类型定义
export interface PoemEntry {
  title: string
  author: string
  poem: string
}

export interface DynastyInfo {
  id: string
  name: string
  count: number
  file: string
}

export interface PoemIndex {
  version: string
  totalCount: number
  dynasties: DynastyInfo[]
}

// 加载器类
export class PoemLoader {
  private indexCache: PoemIndex | null = null
  private dynastyCache: Map<string, PoemEntry[]> = new Map()
  private allPoemsCache: PoemEntry[] | null = null
  private baseUrl = '/poems/'

  async loadIndex(): Promise<DynastyInfo[]> {
    if (this.indexCache) {
      return this.indexCache.dynasties
    }

    const response = await fetch(`${this.baseUrl}index.json`)
    if (!response.ok) {
      throw new Error(`Failed to load poem index: ${response.statusText}`)
    }

    this.indexCache = await response.json()
    return this.indexCache!.dynasties
  }

  async loadDynasty(dynastyId: string): Promise<PoemEntry[]> {
    if (this.dynastyCache.has(dynastyId)) {
      return this.dynastyCache.get(dynastyId)!
    }

    const dynasties = await this.loadIndex()
    const dynasty = dynasties.find(d => d.id === dynastyId)

    if (!dynasty) {
      throw new Error(`Dynasty not found: ${dynastyId}`)
    }

    const response = await fetch(`${this.baseUrl}${dynasty.file}`)
    if (!response.ok) {
      throw new Error(`Failed to load dynasty ${dynastyId}: ${response.statusText}`)
    }

    const data = await response.json()
    const poems = (data.poems as PoemEntry[]).map(p => ({
      ...p,
      poem: p.poem.replace(/\\n/g, '\n')
    }))

    this.dynastyCache.set(dynastyId, poems)
    return poems
  }

  async loadRandomDynasty(): Promise<PoemEntry[]> {
    const dynasties = await this.loadIndex()
    const availableDynasties = dynasties.filter(d => d.count > 0)
    const randomDynasty = availableDynasties[Math.floor(Math.random() * availableDynasties.length)]
    return this.loadDynasty(randomDynasty.id)
  }

  async loadAllPoems(): Promise<PoemEntry[]> {
    if (this.allPoemsCache) {
      return this.allPoemsCache
    }

    const dynasties = await this.loadIndex()
    const availableDynasties = dynasties.filter(d => d.count > 0)
    const poemGroups = await Promise.all(
      availableDynasties.map(dynasty => this.loadDynasty(dynasty.id))
    )

    this.allPoemsCache = poemGroups.flat()
    return this.allPoemsCache
  }

  async loadRandomPoems(count: number): Promise<PoemEntry[]> {
    const poems = await this.loadRandomDynasty()

    if (poems.length <= count) {
      return poems
    }

    const selected: PoemEntry[] = []
    const indices = new Set<number>()

    while (indices.size < count) {
      indices.add(Math.floor(Math.random() * poems.length))
    }

    indices.forEach(i => selected.push(poems[i]))
    return selected
  }

  clearCache(): void {
    this.indexCache = null
    this.allPoemsCache = null
    this.dynastyCache.clear()
  }
}

// 导出单例
export const poemLoader = new PoemLoader()
