import { ref } from 'vue'
import { statisticsApi, type DashboardData } from '../api/statisticsApi'
import { sanitizeApiMessage } from '../utils/apiMessage'
import { logger } from '../utils/logger'

export function useDashboardCharts() {
  let echarts: any = null
  const chartTextColor = '#233226'
  const chartMutedColor = 'rgba(53, 68, 55, 0.66)'
  const chartGridColor = 'rgba(113, 128, 108, 0.16)'
  const chartPanelColor = 'rgba(248, 244, 235, 0.96)'
  const chartBorderColor = 'rgba(120, 132, 109, 0.18)'
  const chartColors = ['#b88d57', '#6f8f70', '#8a6f61', '#8f7f62', '#97a37c']

  const loading = ref(true)
  const dashboardData = ref<DashboardData | null>(null)
  const errorMessage = ref('')

  const chartRefs = {
    borrowTrend: ref<HTMLElement>(),
    popularBooks: ref<HTMLElement>(),
    category: ref<HTMLElement>(),
    categoryRate: ref<HTMLElement>()
  }
  const chartInstances = new Map<string, any>()

  async function initEcharts() {
    const echartsModule = await import('echarts')
    echarts = echartsModule
  }

  let isDisposed = false

  async function loadData() {
    loading.value = true
    isDisposed = false
    errorMessage.value = ''
    try {
      const response = await statisticsApi.getDashboardData()
      if (response.data.success) {
        dashboardData.value = response.data.data
        setTimeout(() => {
          if (!isDisposed) initCharts()
        }, 100)
      } else {
        dashboardData.value = null
        errorMessage.value = sanitizeApiMessage(response.data.message, '加载仪表盘数据失败。')
      }
    } catch (error) {
      dashboardData.value = null
      errorMessage.value = '加载仪表盘数据失败。'
      logger.error('Failed to load dashboard data:', error)
    } finally {
      loading.value = false
    }
  }

  function initCharts() {
    if (!dashboardData.value) return

    if (dashboardData.value.borrowTrends?.length > 0) {
      initChart('borrowTrend', (data) => {
        const trends = data.borrowTrends!
        return {
          color: chartColors,
          animationDuration: 700,
          textStyle: { color: chartMutedColor, fontFamily: 'PingFang SC, Source Han Sans CN, Microsoft YaHei, sans-serif' },
          tooltip: {
            trigger: 'axis',
            backgroundColor: chartPanelColor,
            borderColor: chartBorderColor,
            textStyle: { color: chartTextColor },
          },
          legend: { data: ['借阅', '归还'], textStyle: { color: chartMutedColor } },
          grid: { left: 40, right: 16, top: 48, bottom: 52, containLabel: true },
          xAxis: {
            type: 'category',
            data: trends.map(t => t.date),
            axisLabel: { rotate: 45, color: chartMutedColor },
            axisLine: { lineStyle: { color: chartGridColor } },
            axisTick: { show: false },
          },
          yAxis: {
            type: 'value',
            axisLabel: { color: chartMutedColor },
            splitLine: { lineStyle: { color: chartGridColor } },
          },
          series: [
            {
              name: '借阅',
              type: 'line',
              data: trends.map(t => t.borrowCount),
              smooth: true,
              symbolSize: 6,
              lineStyle: { width: 3, color: '#b88d57' },
              itemStyle: { color: '#b88d57', borderColor: '#ecdbbc', borderWidth: 1 },
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [
                    { offset: 0, color: 'rgba(184, 141, 87, 0.24)' },
                    { offset: 1, color: 'rgba(184, 141, 87, 0)' },
                  ],
                },
              },
            },
            {
              name: '归还',
              type: 'line',
              data: trends.map(t => t.returnCount),
              smooth: true,
              symbolSize: 6,
              lineStyle: { width: 3, color: '#6f8f70' },
              itemStyle: { color: '#6f8f70', borderColor: '#d9e3d7', borderWidth: 1 },
              areaStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [
                    { offset: 0, color: 'rgba(111, 143, 112, 0.2)' },
                    { offset: 1, color: 'rgba(111, 143, 112, 0)' },
                  ],
                },
              },
            },
          ]
        }
      })
    }

    if (dashboardData.value.popularBooks?.length > 0) {
      initChart('popularBooks', (data) => {
        const books = data.popularBooks!.slice(0, 10)
        return {
          color: chartColors,
          animationDuration: 700,
          textStyle: { color: chartMutedColor, fontFamily: 'PingFang SC, Source Han Sans CN, Microsoft YaHei, sans-serif' },
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            backgroundColor: chartPanelColor,
            borderColor: chartBorderColor,
            textStyle: { color: chartTextColor },
          },
          grid: { left: 28, right: 22, top: 24, bottom: 16, containLabel: true },
          xAxis: {
            type: 'value',
            axisLabel: { color: chartMutedColor },
            splitLine: { lineStyle: { color: chartGridColor } },
          },
          yAxis: {
            type: 'category',
            data: books.map(b => b.title.length > 12 ? `${b.title.substring(0, 12)}...` : b.title),
            axisLabel: { interval: 0, color: chartMutedColor }
          },
          series: [
            {
              name: '借阅量',
              type: 'bar',
              data: books.map(b => b.borrowCount),
              barWidth: 18,
              itemStyle: {
                borderRadius: [0, 999, 999, 0],
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 1,
                  y2: 0,
                  colorStops: [
                    { offset: 0, color: '#8f7f62' },
                    { offset: 1, color: '#d8c39d' },
                  ],
                },
              },
            }
          ]
        }
      })
    }

    if (dashboardData.value.categoryStatistics?.length > 0) {
      initChart('category', (data) => {
        const stats = data.categoryStatistics!
        return {
          color: ['#7f95df', '#d96a5f'],
          animationDuration: 700,
          textStyle: { color: chartMutedColor, fontFamily: 'PingFang SC, Source Han Sans CN, Microsoft YaHei, sans-serif' },
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            backgroundColor: chartPanelColor,
            borderColor: chartBorderColor,
            textStyle: { color: chartTextColor },
          },
          legend: { data: ['馆藏总量', '借出数量'], textStyle: { color: chartMutedColor } },
          grid: { left: 32, right: 18, top: 48, bottom: 24, containLabel: true },
          xAxis: {
            type: 'category',
            data: stats.map(c => c.category),
            axisLabel: { color: chartMutedColor },
            axisLine: { lineStyle: { color: chartGridColor } },
          },
          yAxis: {
            type: 'value',
            axisLabel: { color: chartMutedColor },
            splitLine: { lineStyle: { color: chartGridColor } },
          },
          series: [
            {
              name: '馆藏总量',
              type: 'bar',
              data: stats.map(c => c.totalBooks),
              barWidth: 20,
              itemStyle: { color: '#6f8f70', borderRadius: [999, 999, 0, 0] },
            },
            {
              name: '借出数量',
              type: 'bar',
              data: stats.map(c => c.borrowedBooks),
              barWidth: 20,
              itemStyle: { color: '#8a6f61', borderRadius: [999, 999, 0, 0] },
            },
          ]
        }
      })

      initChart('categoryRate', (data) => ({
        color: ['#b88d57', '#6f8f70', '#8a6f61', '#8f7f62', '#97a37c'],
        animationDuration: 700,
        textStyle: { color: chartMutedColor, fontFamily: 'PingFang SC, Source Han Sans CN, Microsoft YaHei, sans-serif' },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}%',
          backgroundColor: chartPanelColor,
          borderColor: chartBorderColor,
          textStyle: { color: chartTextColor },
        },
        legend: {
          bottom: 0,
          textStyle: { color: chartMutedColor },
        },
        series: [{
          name: '借阅率',
          type: 'pie',
          radius: '60%',
          data: data.categoryStatistics!.map(c => ({ name: c.category, value: c.borrowRate })),
          label: { color: chartTextColor },
          labelLine: { lineStyle: { color: 'rgba(143, 127, 98, 0.32)' } },
          emphasis: {
            itemStyle: {
              shadowBlur: 24,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.55)'
            }
          }
        }]
      }))
    }
  }

  function initChart(key: keyof typeof chartRefs, optionsBuilder: (data: DashboardData) => any) {
    const chartRef = chartRefs[key]
    if (!chartRef.value || !dashboardData.value) return
    const instance = chartInstances.get(key)
    if (instance) instance.dispose()
    const newInstance = echarts.init(chartRef.value)
    newInstance.setOption(optionsBuilder(dashboardData.value))
    chartInstances.set(key, newInstance)
  }

  function handleResize() {
    chartInstances.forEach(instance => instance?.resize())
  }

  function dispose() {
    isDisposed = true
    chartInstances.forEach(instance => instance?.dispose())
    chartInstances.clear()
  }

  return {
    loading,
    dashboardData,
    errorMessage,
    borrowTrendChart: chartRefs.borrowTrend,
    popularBooksChart: chartRefs.popularBooks,
    categoryChart: chartRefs.category,
    categoryRateChart: chartRefs.categoryRate,
    initEcharts,
    loadData,
    handleResize,
    dispose,
  }
}
