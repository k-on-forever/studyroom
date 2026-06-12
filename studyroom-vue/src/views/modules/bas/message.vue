<template>
  <div class="mod-bas-message">
    <el-form :inline="true">
      <el-form-item label="类型">
        <el-select v-model="q.messageType" clearable placeholder="全部" style="width: 120px;">
          <el-option label="类型1" :value="1" />
          <el-option label="其它" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="q.keyword" clearable placeholder="用户名/内容" style="width: 200px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="pageIndex = 1; fetchList()">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="70" header-align="center" align="center" />
      <el-table-column prop="messageType" label="类型" width="80" header-align="center" align="center" />
      <el-table-column prop="username" label="用户" width="120" header-align="center" align="center" />
      <el-table-column prop="userId" label="用户ID" width="100" header-align="center" align="center" />
      <el-table-column prop="message" label="留言内容" min-width="260" show-overflow-tooltip header-align="center" />
      <el-table-column prop="createTime" label="时间" width="165" header-align="center" align="center" />
      <el-table-column label="操作" width="90" fixed="right" header-align="center" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" class="danger-text" @click="removeOne(scope.row)">删除</el-button>
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
  name: 'BasMessage',
  data () {
    return {
      loading: false,
      dataList: [],
      totalCount: 0,
      pageIndex: 1,
      pageSize: 10,
      q: { keyword: '', messageType: null }
    }
  },
  created () {
    this.fetchList()
  },
  methods: {
    fetchList () {
      this.loading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/message/page'),
        method: 'get',
        params: this.$http.adornParams({
          page: this.pageIndex,
          limit: this.pageSize,
          keyword: this.q.keyword || undefined,
          messageType: this.q.messageType != null && this.q.messageType !== '' ? this.q.messageType : undefined
        })
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
    removeOne (row) {
      this.$confirm('确定删除该留言？', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/message/delete'),
            method: 'post',
            data: this.$http.adornData({ id: row.id })
          })
            .then(({ data }) => {
              if (data && data.code === 0) {
                this.$message.success('已删除')
                this.fetchList()
              } else {
                this.$message.error((data && data.msg) || '失败')
              }
            })
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.mod-bas-message {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
</style>
