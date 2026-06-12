<template>
  <div class="mod-bas-appointment">
    <el-form :inline="true">
      <el-form-item label="业务日">
        <el-date-picker v-model="q.bizDate" type="date" value-format="yyyy-MM-dd" placeholder="全部" clearable style="width: 160px;" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="q.seatState" clearable placeholder="全部" style="width: 130px;">
          <el-option label="待签到" :value="0" />
          <el-option label="使用中" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="未签到取消" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="q.keyword" clearable placeholder="手机/姓名/时段" style="width: 200px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="pageIndex = 1; fetchList()">查询</el-button>
        <el-button @click="exportCsv">导出 CSV</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="90" header-align="center" align="center" />
      <el-table-column prop="floor" label="楼层" width="90" header-align="center" align="center" show-overflow-tooltip />
      <el-table-column prop="roomName" label="自习室" min-width="110" header-align="center" align="center" show-overflow-tooltip />
      <el-table-column prop="seatLabel" label="座位" width="90" header-align="center" align="center" />
      <el-table-column prop="seatDay" label="预约时段" min-width="180" show-overflow-tooltip header-align="center" />
      <el-table-column prop="bizDate" label="业务日" width="110" header-align="center" align="center" />
      <el-table-column prop="seatName" label="预约人" width="100" header-align="center" align="center" show-overflow-tooltip />
      <el-table-column prop="seatPhone" label="手机" width="120" header-align="center" align="center" />
      <el-table-column prop="userId" label="用户ID" width="100" header-align="center" align="center" />
      <el-table-column label="状态" width="90" header-align="center" align="center">
        <template slot-scope="scope">{{ stateLabel(scope.row.seatState) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="165" header-align="center" align="center" />
      <el-table-column prop="checkInAt" label="签到时间" width="165" header-align="center" align="center" />
      <el-table-column prop="studyEndAt" label="结束学习" width="165" header-align="center" align="center" />
      <el-table-column label="操作" width="120" fixed="right" header-align="center" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.seatState === 0 || scope.row.seatState === 1"
            type="text"
            size="small"
            class="danger-text"
            @click="forceCancel(scope.row)"
          >强制取消</el-button>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top: 12px; text-align: right;"
      background
      layout="total, prev, pager, next, sizes"
      :page-sizes="[10, 20, 50]"
      :current-page.sync="pageIndex"
      :page-size.sync="pageSize"
      :total="totalCount"
      @current-change="fetchList"
      @size-change="() => { pageIndex = 1; fetchList() }"
    />
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'
export default {
  name: 'BasAppointment',
  data () {
    return {
      loading: false,
      dataList: [],
      totalCount: 0,
      pageIndex: 1,
      pageSize: 10,
      q: { bizDate: null, seatState: null, keyword: '' }
    }
  },
  created () {
    this.fetchList()
  },
  methods: {
    stateLabel (s) {
      const m = { 0: '待签到', 1: '使用中', 2: '已取消', 3: '已完成', 4: '未签到取消' }
      return m[s] != null ? m[s] : (s == null ? '—' : String(s))
    },
    fetchList () {
      this.loading = true
      const params = this.$http.adornParams({
        page: this.pageIndex,
        limit: this.pageSize,
        bizDate: this.q.bizDate || undefined,
        seatState: this.q.seatState != null && this.q.seatState !== '' ? this.q.seatState : undefined,
        keyword: this.q.keyword || undefined
      })
      this.$http({
        url: this.$http.adornUrl('/sys/bas/appointment/page'),
        method: 'get',
        params
      })
        .then(({ data }) => {
          this.loading = false
          if (data && data.code === 0) {
            this.dataList = data.list || []
            this.totalCount = data.totalCount != null ? data.totalCount : 0
          } else {
            this.$message.error((data && data.msg) || '加载失败')
          }
        })
        .catch((e) => {
          this.loading = false
          this.$message.error(httpConnErrorMessage(e))
        })
    },
    exportCsv () {
      const params = this.$http.adornParams({
        bizDate: this.q.bizDate || undefined,
        seatState: this.q.seatState != null && this.q.seatState !== '' ? this.q.seatState : undefined,
        keyword: this.q.keyword || undefined
      })
      this.$http({
        url: this.$http.adornUrl('/sys/bas/appointment/export'),
        method: 'get',
        params,
        responseType: 'blob'
      }).then((response) => {
        let filename = 'appointments.csv'
        const cd = response.headers && response.headers['content-disposition']
        if (cd && cd.indexOf('filename=') >= 0) {
          const q = cd.match(/filename="([^"]+)"/)
          const star = cd.match(/filename\*=UTF-8''(.+)/)
          if (star && star[1]) {
            filename = decodeURIComponent(star[1].replace(/(^"|"$)/g, ''))
          } else if (q && q[1]) {
            filename = q[1]
          }
        }
        const blob = new Blob([response.data])
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        a.click()
        window.URL.revokeObjectURL(url)
      }).catch((e) => {
        this.$message.error(httpConnErrorMessage(e))
      })
    },
    forceCancel (row) {
      this.$confirm('确定强制取消该预约？将释放座位占用。', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/appointment/admin-cancel'),
            method: 'post',
            data: this.$http.adornData({ id: row.id })
          })
            .then(({ data }) => {
              if (data && data.code === 0) {
                this.$message.success('已取消')
                this.fetchList()
              } else {
                this.$message.error((data && data.msg) || '失败')
              }
            })
            .catch(() => this.$message.error('请求失败'))
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.mod-bas-appointment {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
.text-muted {
  color: #999;
  font-size: 12px;
}
</style>
