<template>
  <div class="mod-bas-mini-user">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="账号与登录说明"
      description="小程序用户登录账号为「手机号」（与账号列一致）。密码经 SHA256 单向加密入库，任何人（含管理员）均无法查看明文；如需帮用户重置，请另做「重置密码」功能或让用户使用忘记密码流程。"
      style="margin-bottom: 14px;"
    />
    <el-form :inline="true">
      <el-form-item label="状态">
        <el-select v-model="q.status" clearable placeholder="全部" style="width: 110px;">
          <el-option label="正常" :value="1" />
          <el-option label="封禁" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="q.keyword" clearable placeholder="手机/昵称/账号/openId" style="width: 220px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="pageIndex = 1; fetchList()">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="userId" label="用户ID" width="160" header-align="center" align="center" />
      <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip header-align="center" align="center" />
      <el-table-column prop="account" label="登录账号(手机)" width="130" show-overflow-tooltip header-align="center" align="center" />
      <el-table-column label="密码" width="120" header-align="center" align="center">
        <template slot-scope="scope">
          <span style="color:#909399;">不可查看</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" width="100" header-align="center" align="center" />
      <el-table-column prop="mobile" label="手机" width="120" header-align="center" align="center" />
      <el-table-column prop="openId" label="openId" min-width="160" show-overflow-tooltip header-align="center" />
      <el-table-column prop="createTime" label="注册时间" width="165" header-align="center" align="center" />
      <el-table-column label="封禁" width="90" fixed="right" header-align="center" align="center">
        <template slot-scope="scope">
          <el-switch
            :value="scope.row.status === 0"
            active-color="#f56c6c"
            @change="(v) => setBan(scope.row, v)"
          />
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
  name: 'BasMiniUser',
  data () {
    return {
      loading: false,
      dataList: [],
      totalCount: 0,
      pageIndex: 1,
      pageSize: 10,
      q: { keyword: '', status: null }
    }
  },
  created () {
    this.fetchList()
  },
  methods: {
    fetchList () {
      this.loading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/mini-user/page'),
        method: 'get',
        params: this.$http.adornParams({
          page: this.pageIndex,
          limit: this.pageSize,
          keyword: this.q.keyword || undefined,
          status: this.q.status != null && this.q.status !== '' ? this.q.status : undefined
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
    setBan (row, banned) {
      const status = banned ? 0 : 1
      this.$http({
        url: this.$http.adornUrl('/sys/bas/mini-user/status'),
        method: 'post',
        data: this.$http.adornData({ userId: row.userId, status })
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.$message.success(banned ? '已封禁' : '已解禁')
            row.status = status
          } else {
            this.$message.error((data && data.msg) || '操作失败')
            this.fetchList()
          }
        })
        .catch(() => {
          this.$message.error('请求失败')
          this.fetchList()
        })
    }
  }
}
</script>

<style scoped>
.mod-bas-mini-user {
  padding: 12px 16px;
}
</style>
