<template>
  <div class="mod-page">
    <div class="mod-page-toolbar">
      <span class="mod-page-title">操作日志</span>
    </div>
    <el-form :inline="true" @submit.native.prevent="loadData">
      <el-form-item label="操作人">
        <el-input v-model="q.username" placeholder="用户名" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="操作描述">
        <el-input v-model="q.operation" placeholder="关键词" clearable style="width:160px" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker v-model="q.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="yyyy-MM-dd" style="width:260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="rows" border stripe v-loading="loading" size="small" style="width:100%">
      <el-table-column prop="username" label="操作人" width="110" align="center" />
      <el-table-column prop="operation" label="操作内容" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作详情" min-width="280">
        <template slot-scope="s">
          <span class="log-detail">{{ formatParams(s.row.operation, s.row.params) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="130" align="center" />
      <el-table-column prop="createDate" label="操作时间" width="170" align="center">
        <template slot-scope="s">{{ formatTime(s.row.createDate) }}</template>
      </el-table-column>
      <el-table-column label="详情" width="70" align="center">
        <template slot-scope="s">
          <el-button type="text" size="mini" @click="showDetail(s.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 0"
      background
      layout="total, sizes, prev, pager, next"
      :total="total"
      :page-size.sync="pageSize"
      :current-page.sync="page"
      :page-sizes="[20, 50, 100]"
      style="margin-top:12px;text-align:right"
      @current-change="loadData"
      @size-change="loadData"
    />
    <el-dialog title="操作详情" :visible.sync="detailVisible" width="600px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="操作人">{{ detailRow.username }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">{{ detailRow.operation }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTime(detailRow.createDate) }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detailRow.ip }}</el-descriptions-item>
        <el-descriptions-item label="执行方法">{{ detailRow.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="log-pre">{{ formatJson(detailRow.params) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'

export default {
  name: 'SysLogList',
  data () {
    return {
      q: { username: '', operation: '', dateRange: null },
      rows: [],
      loading: false,
      total: 0,
      page: 1,
      pageSize: 20,
      detailVisible: false,
      detailRow: {}
    }
  },
  mounted () {
    this.loadData()
  },
  methods: {
    formatTime (v) {
      if (!v) return '—'
      var d = new Date(v)
      var pad = function (n) { return n < 10 ? '0' + n : '' + n }
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
    },
    formatParams (operation, params) {
      if (!params || params === '[]' || params === '{}') return '—'
      try {
        var arr = JSON.parse(params)
        if (!Array.isArray(arr) || arr.length === 0) return '—'
        var obj = arr[0]
        if (!obj || typeof obj !== 'object') return '—'
        var keys = Object.keys(obj)
        if (keys.length === 0) return '—'
        return keys.map(function (k) { return k + ': ' + (obj[k] != null ? obj[k] : '空') }).join('，')
      } catch (e) {
        return params
      }
    },
    formatJson (v) {
      if (!v) return ''
      try {
        return JSON.stringify(JSON.parse(v), null, 2)
      } catch (e) {
        return v
      }
    },
    resetQuery () {
      this.q = { username: '', operation: '', dateRange: null }
      this.page = 1
      this.loadData()
    },
    showDetail (row) {
      this.detailRow = row
      this.detailVisible = true
    },
    loadData () {
      this.loading = true
      var params = {
        page: this.page,
        limit: this.pageSize,
        username: this.q.username || undefined,
        operation: this.q.operation || undefined
      }
      if (this.q.dateRange && this.q.dateRange.length === 2) {
        params.startDate = this.q.dateRange[0]
        params.endDate = this.q.dateRange[1]
      }
      this.$http({
        url: this.$http.adornUrl('/sys/log/list'),
        method: 'get',
        params: this.$http.adornParams(params)
      })
        .then(({ data }) => {
          this.loading = false
          if (data && data.code === 0) {
            this.rows = data.list || []
            this.total = data.total || 0
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
.log-detail {
  font-size: 12px;
  color: #606266;
}
.log-pre {
  margin: 0;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
