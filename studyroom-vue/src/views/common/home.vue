<template>
  <div class="mod-home">
    <header class="home-title">自习室运营看板</header>
    <p class="home-sub">以下指标可按业务日查询；营收图为近 7 日汇总（会员订单 + 按次预约）。</p>

    <el-form :inline="true" :model="dataForm" class="home-toolbar">
      <el-form-item label="业务日">
        <el-date-picker
          v-model="dataForm.bizDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="选择日期"
          clearable
          style="width: 160px;"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadOverview()">刷新总览</el-button>
        <el-button @click="loadRoomTable()">刷新各室预约次数</el-button>
        <el-button @click="loadRevenue7d()">刷新营收图</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="16" class="home-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">已预约座位 / 可约座位</div>
          <div class="metric-value">{{ overview.bookedSeatCount }} / {{ overview.totalBookableSeats }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">预约率（非取消）</div>
          <div class="metric-value">{{ overview.bookingRatePercent }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card metric-card--accent">
          <div class="metric-label">当前使用中（已签到）</div>
          <div class="metric-value">{{ overview.inUseSeatCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card metric-card--revenue">
          <div class="metric-label">今日营收</div>
          <div class="metric-value metric-value--revenue">
            <template v-if="todayRevenue">¥{{ formatYuan(todayRevenue.totalYuan) }}</template>
            <template v-else>加载中…</template>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">所选业务日</div>
          <div class="metric-value metric-value--sm">{{ overview.bizDate || '—' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card class="home-chart-card" shadow="never">
          <div slot="header" class="clearfix">
            <span>近 7 日营收趋势（元）</span>
          </div>
          <div ref="revenueChart" style="width:100%;height:280px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="home-chart-card" shadow="never">
          <div slot="header" class="clearfix">
            <span>今日营收构成</span>
          </div>
          <div ref="pieChart" style="width:100%;height:280px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="home-table-card" shadow="never">
      <div slot="header">各自习室预约次数（所选业务日）</div>
      <el-table :data="dataList" border stripe v-loading="dataListLoading" style="width: 100%;">
        <template slot="empty">
          <el-empty v-if="!dataListLoading" description="所选业务日暂无各室预约记录" :image-size="72" />
        </template>
        <el-table-column type="index" width="50" />
        <el-table-column prop="floor" label="楼层" header-align="center" align="center" show-overflow-tooltip />
        <el-table-column prop="roomName" label="自习室" header-align="center" align="center" show-overflow-tooltip />
        <el-table-column prop="count" label="预约次数" header-align="center" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { httpConnErrorMessage } from '@/utils'

function todayYmd () {
  const d = new Date()
  const m = d.getMonth() + 1
  const day = d.getDate()
  return d.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (day < 10 ? '0' : '') + day
}

export default {
  data () {
    return {
      dataForm: { bizDate: todayYmd() },
      overview: {
        bizDate: '',
        totalBookableSeats: 0,
        bookedSeatCount: 0,
        bookingRatePercent: 0,
        inUseSeatCount: 0
      },
      revenue7d: [],
      todayRevenue: null,
      dataList: [],
      dataListLoading: false,
      revenueChart: null,
      pieChart: null
    }
  },
  mounted () {
    this.$nextTick(() => {
      this.revenueChart = echarts.init(this.$refs.revenueChart)
      this.pieChart = echarts.init(this.$refs.pieChart)
      // 图表就绪后才请求数据（避免数据回来但 chart 还没初始化）
      this.loadOverview()
      this.loadRevenue7d()
      this.loadTodayRevenue()
      this.loadRoomTable()
    })
  },
  methods: {
    formatYuan (v) {
      if (v == null) return '0'
      const n = Number(v)
      return Number.isFinite(n) ? n.toFixed(2) : String(v)
    },
    renderRevenueChart (rows) {
      if (!this.revenueChart) return
      const dates = rows.map(r => r.date.slice(5))
      const total = rows.map(r => Number(r.totalYuan) || 0)
      const membership = rows.map(r => Number(r.membershipYuan) || 0)
      const appointment = rows.map(r => Number(r.appointmentYuan) || 0)
      this.revenueChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: function (params) {
            const d = params[0].axisValue
            let html = '<strong>' + d + '</strong><br/>'
            params.forEach(p => {
              html += p.marker + ' ' + p.seriesName + '：¥' + Number(p.value).toFixed(2) + '<br/>'
            })
            return html
          }
        },
        legend: { data: ['会员卡', '按次预约', '合计'], bottom: 0 },
        grid: { left: 60, right: 20, bottom: 50, top: 20 },
        xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
        yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
        series: [
          {
            name: '会员卡',
            type: 'bar',
            stack: 'revenue',
            data: membership,
            itemStyle: { color: '#409eff' }
          },
          {
            name: '按次预约',
            type: 'bar',
            stack: 'revenue',
            data: appointment,
            itemStyle: { color: '#79bbff' }
          },
          {
            name: '合计',
            type: 'line',
            data: total,
            lineStyle: { color: '#e6a23c', width: 2 },
            itemStyle: { color: '#e6a23c' },
            symbol: 'circle',
            symbolSize: 6
          }
        ]
      })
    },
    renderPieChart (data) {
      if (!this.pieChart) return
      const rows = [
        { name: '会员卡收入', value: Number(data.membershipYuan) || 0 },
        { name: '按次预约收入', value: Number(data.appointmentYuan) || 0 }
      ]
      const hasData = rows.some(r => r.value > 0)
      this.pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}：¥{c} ({d}%)' },
        series: [{
          type: 'pie',
          radius: ['30%', '60%'],
          center: ['50%', '50%'],
          data: hasData ? rows.filter(r => r.value > 0) : [],
          label: hasData ? { show: true, formatter: '{b}\n¥{c}', fontSize: 12 } : undefined,
          itemStyle: {
            color: hasData ? function (p) {
              return p.dataIndex === 0 ? '#409eff' : '#79bbff'
            } : undefined
          }
        }],
        graphic: hasData ? [{
          type: 'text',
          left: 'center',
          top: '42%',
          style: {
            text: '¥' + this.formatYuan(data.totalYuan),
            fill: '#303133',
            fontSize: 18,
            fontWeight: 700
          }
        }] : [{
          type: 'text',
          left: 'center',
          top: '45%',
          style: {
            text: '暂无数据',
            fill: '#c0c4cc',
            fontSize: 16
          }
        }]
      })
    },
    loadOverview () {
      const bizDate = this.dataForm.bizDate || todayYmd()
      this.$http({
        url: this.$http.adornUrl('/sys/dashboard/overview'),
        method: 'get',
        params: this.$http.adornParams({ bizDate })
      })
        .then(({ data }) => {
          if (data && data.code === 0 && data.data) {
            this.overview = data.data
          } else {
            this.$message.error((data && data.msg) || '总览加载失败')
          }
        })
        .catch((e) => this.$message.error(httpConnErrorMessage(e)))
    },
    loadTodayRevenue () {
      this.$http({
        url: this.$http.adornUrl('/sys/dashboard/today-revenue'),
        method: 'get',
        params: this.$http.adornParams()
      }).then(({ data }) => {
        if (data && data.code === 0 && data.data) {
          this.todayRevenue = data.data
          this.renderPieChart(data.data)
        }
      }).catch(() => {})
    },
    loadRevenue7d () {
      this.$http({
        url: this.$http.adornUrl('/sys/dashboard/revenue7d'),
        method: 'get',
        params: this.$http.adornParams()
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            const rows = data.data || []
            this.revenue7d = rows
            this.renderRevenueChart(rows)
          }
        })
        .catch(() => {})
    },
    loadRoomTable () {
      const date = this.dataForm.bizDate || todayYmd()
      this.dataListLoading = true
      this.$http({
        url: this.$http.adornUrl('/basappointment/count'),
        method: 'get',
        params: this.$http.adornParams({ date })
      })
        .then(({ data }) => {
          this.dataListLoading = false
          if (data && data.code === 0) {
            this.dataList = data.data || []
          } else {
            this.$message.error((data && data.msg) || '加载失败')
          }
        })
        .catch((e) => {
          this.dataListLoading = false
          this.$message.error(httpConnErrorMessage(e))
        })
    }
  }
}
</script>

<style scoped>
.mod-home {
  padding: 16px 20px 40px;
  line-height: 1.5;
}
.home-title {
  text-align: center;
  color: #17b3a3;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}
.home-sub {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}
.home-toolbar {
  margin-bottom: 16px;
}
.home-cards {
  margin-bottom: 16px;
}
.metric-card {
  margin-bottom: 12px;
}
.metric-card--accent {
  border-top: 3px solid #409eff;
}
.metric-label {
  font-size: 13px;
  color: #909399;
}
.metric-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-top: 8px;
}
.metric-value--sm {
  font-size: 18px;
}
.home-chart-card {
  margin-bottom: 16px;
}
.metric-card--revenue {
  border-top: 3px solid #ffd700;
}
.metric-value--revenue {
  font-size: 22px;
  font-weight: 700;
  color: #e6a23c;
  margin-top: 8px;
}
</style>


