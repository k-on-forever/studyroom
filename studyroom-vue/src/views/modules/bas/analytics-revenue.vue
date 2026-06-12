<template>
  <div class="mod-page">
    <div class="mod-page-toolbar">
      <span class="mod-page-title">营收统计</span>
      <el-button type="primary" size="small" icon="el-icon-download" @click="exportCsv">导出CSV</el-button>
    </div>
    <el-tabs v-model="tab" @tab-click="reload">
      <el-tab-pane label="按日" name="day">
        <el-form :inline="true">
          <el-form-item label="回溯天数">
            <el-input-number v-model="pastDays" :min="7" :max="90" @change="reload" />
          </el-form-item>
        </el-form>
        <div ref="chartDay" style="width:100%;height:280px;margin-bottom:16px;"></div>
        <el-table :data="rows" border stripe v-loading="loading" size="small">
          <el-table-column prop="date" label="日期" width="120" align="center" />
          <el-table-column label="会员卡(元)" align="center">
            <template slot-scope="s">{{ formatN(s.row.membershipYuan) }}</template>
          </el-table-column>
          <el-table-column label="按次预约(元)" align="center">
            <template slot-scope="s">{{ formatN(s.row.appointmentYuan) }}</template>
          </el-table-column>
          <el-table-column label="合计(元)" align="center">
            <template slot-scope="s">{{ formatN(s.row.totalYuan) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单笔数" width="100" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="按周" name="week">
        <el-form :inline="true">
          <el-form-item label="周数">
            <el-input-number v-model="pastWeeks" :min="4" :max="52" @change="reload" />
          </el-form-item>
        </el-form>
        <div ref="chartWeek" style="width:100%;height:280px;margin-bottom:16px;"></div>
        <el-table :data="rows" border stripe v-loading="loading" size="small">
          <el-table-column prop="weekStart" label="周开始" width="120" align="center" />
          <el-table-column prop="weekEnd" label="周结束" width="120" align="center" />
          <el-table-column label="会员卡(元)" align="center"><template slot-scope="s">{{ formatN(s.row.membershipYuan) }}</template></el-table-column>
          <el-table-column label="按次(元)" align="center"><template slot-scope="s">{{ formatN(s.row.appointmentYuan) }}</template></el-table-column>
          <el-table-column label="合计(元)" align="center"><template slot-scope="s">{{ formatN(s.row.totalYuan) }}</template></el-table-column>
          <el-table-column prop="orderCount" label="订单笔数" width="100" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="按月" name="month">
        <el-form :inline="true">
          <el-form-item label="月数">
            <el-input-number v-model="pastMonths" :min="3" :max="24" @change="reload" />
          </el-form-item>
        </el-form>
        <div ref="chartMonth" style="width:100%;height:280px;margin-bottom:16px;"></div>
        <el-table :data="rows" border stripe v-loading="loading" size="small">
          <el-table-column prop="month" label="月份" width="120" align="center" />
          <el-table-column label="会员卡(元)" align="center"><template slot-scope="s">{{ formatN(s.row.membershipYuan) }}</template></el-table-column>
          <el-table-column label="按次(元)" align="center"><template slot-scope="s">{{ formatN(s.row.appointmentYuan) }}</template></el-table-column>
          <el-table-column label="合计(元)" align="center"><template slot-scope="s">{{ formatN(s.row.totalYuan) }}</template></el-table-column>
          <el-table-column prop="orderCount" label="订单笔数" width="100" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { httpConnErrorMessage } from '@/utils'

export default {
  name: 'BasAnalyticsRevenue',
  data () {
    return {
      tab: 'day',
      loading: false,
      rows: [],
      pastDays: 30,
      pastWeeks: 8,
      pastMonths: 12,
      charts: {}
    }
  },
  mounted () {
    this.$nextTick(() => {
      this.charts.day = echarts.init(this.$refs.chartDay)
      this.charts.week = echarts.init(this.$refs.chartWeek)
      this.charts.month = echarts.init(this.$refs.chartMonth)
      window.addEventListener('resize', this.handleResize)
    })
    this.reload()
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    handleResize () {
      Object.values(this.charts).forEach(c => c && c.resize())
    },
    formatN (v) {
      const n = Number(v)
      return Number.isFinite(n) ? n.toFixed(2) : (v == null ? '0.00' : String(v))
    },
    renderChart (key, rows, xField) {
      const chart = this.charts[key]
      if (!chart) return
      const xData = rows.map(r => {
        const v = r[xField] || ''
        return v.length > 7 ? v.slice(5) : v
      })
      const membership = rows.map(r => Number(r.membershipYuan) || 0)
      const appointment = rows.map(r => Number(r.appointmentYuan) || 0)
      const total = rows.map(r => Number(r.totalYuan) || 0)
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: function (params) {
            var d = params[0].axisValue
            var html = '<strong>' + d + '</strong><br/>'
            params.forEach(function (p) {
              html += p.marker + ' ' + p.seriesName + '：¥' + Number(p.value).toFixed(2) + '<br/>'
            })
            return html
          }
        },
        legend: { data: ['会员卡', '按次预约', '合计'], bottom: 0 },
        grid: { left: 60, right: 20, bottom: 50, top: 20 },
        xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11 } },
        yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
        series: [
          { name: '会员卡', type: 'bar', stack: 'revenue', data: membership, itemStyle: { color: '#409eff' } },
          { name: '按次预约', type: 'bar', stack: 'revenue', data: appointment, itemStyle: { color: '#79bbff' } },
          { name: '合计', type: 'line', data: total, lineStyle: { color: '#e6a23c', width: 2 }, itemStyle: { color: '#e6a23c' }, symbol: 'circle', symbolSize: 6 }
        ]
      })
    },
    exportCsv () {
      const params = this.$http.adornParams({ past: this.pastMonths })
      const url = this.$http.adornUrl('/sys/analytics/revenue/export-summary') + '?' + new URLSearchParams(params).toString()
      window.open(url, '_blank')
    },
    reload () {
      var urlMap = { day: '/sys/analytics/revenue/days', week: '/sys/analytics/revenue/weeks', month: '/sys/analytics/revenue/months' }
      var pastMap = { day: this.pastDays, week: this.pastWeeks, month: this.pastMonths }
      var xFieldMap = { day: 'date', week: 'weekStart', month: 'month' }
      var url = urlMap[this.tab]
      var params = { past: pastMap[this.tab] }
      this.loading = true
      this.$http({
        url: this.$http.adornUrl(url),
        method: 'get',
        params: this.$http.adornParams(params)
      })
        .then(({ data }) => {
          this.loading = false
          if (data && data.code === 0) {
            this.rows = data.data || []
            this.$nextTick(() => this.renderChart(this.tab, this.rows, xFieldMap[this.tab]))
          } else {
            this.$message.error((data && data.msg) || '加载失败')
          }
        })
        .catch((e) => {
          this.loading = false
          this.$message.error(httpConnErrorMessage(e))
        })
    }
  }
}
</script>

<style scoped>
.mod-page { padding: 12px 16px; }
</style>
