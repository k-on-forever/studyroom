<template>
  <div class="mod-page">
    <el-form :inline="true">
      <el-form-item label="开始">
        <el-date-picker v-model="q.from" type="date" value-format="yyyy-MM-dd" placeholder="开始" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="结束">
        <el-date-picker v-model="q.to" type="date" value-format="yyyy-MM-dd" placeholder="结束" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="热门Top">
        <el-input-number v-model="q.top" :min="5" :max="50" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="热门座位">
          <el-table :data="popular" border stripe size="small" v-loading="loading">
            <el-table-column type="index" width="45" />
            <el-table-column prop="seatName" label="座位" />
            <el-table-column prop="cnt" label="预约次数" width="100" align="center" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="高峰时段（按预约起始时刻）">
          <el-table :data="peak" border stripe size="small" v-loading="loading">
            <el-table-column prop="timeRange" label="时段（起始→下一档）" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="cnt" label="预约次数" width="110" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'
export default {
  name: 'BasAnalyticsUtilization',
  data () {
    return {
      loading: false,
      q: { from: null, to: null, top: 15 },
      popular: [],
      peak: []
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      this.$http({
        url: this.$http.adornUrl('/sys/analytics/utilization'),
        method: 'get',
        params: this.$http.adornParams({
          from: this.q.from || undefined,
          to: this.q.to || undefined,
          top: this.q.top
        })
      })
        .then(({ data }) => {
          this.loading = false
          if (data && data.code === 0 && data.data) {
            this.popular = data.data.popularSeats || []
            this.peak = data.data.peakSlots || []
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
.text-muted { color: #909399; font-size: 12px; }
</style>
