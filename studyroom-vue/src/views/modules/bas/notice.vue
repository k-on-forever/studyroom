<template>
  <div class="mod-bas-notice">
    <el-form :inline="true">
      <el-form-item label="关键词">
        <el-input v-model="q.keyword" clearable placeholder="标题/正文" style="width: 200px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="pageIndex = 1; fetchList()">查询</el-button>
        <el-button type="primary" @click="openAdd()">新增公告</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="id" label="ID" width="70" header-align="center" align="center" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip header-align="center" />
      <el-table-column prop="content" label="内容摘要" min-width="240" show-overflow-tooltip header-align="center" />
      <el-table-column prop="createTime" label="发布时间" width="165" header-align="center" align="center" />
      <el-table-column label="操作" width="140" fixed="right" header-align="center" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" :close-on-click-modal="false" @close="resetForm">
      <el-form :model="dataForm" :rules="rules" ref="dataFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="dataForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="dataForm.content" type="textarea" :rows="8" maxlength="8000" show-word-limit placeholder="支持较长正文" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm()">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'
export default {
  name: 'BasNotice',
  data () {
    return {
      loading: false,
      dataList: [],
      totalCount: 0,
      pageIndex: 1,
      pageSize: 10,
      q: { keyword: '' },
      dialogVisible: false,
      dialogTitle: '新增公告',
      dataForm: { id: null, title: '', content: '' },
      rules: {
        title: [{ required: true, message: '必填', trigger: 'blur' }]
      }
    }
  },
  created () {
    this.fetchList()
  },
  methods: {
    fetchList () {
      this.loading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/notice/page'),
        method: 'get',
        params: this.$http.adornParams({
          page: this.pageIndex,
          limit: this.pageSize,
          keyword: this.q.keyword || undefined
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
    openAdd () {
      this.dialogTitle = '新增公告'
      this.dataForm = { id: null, title: '', content: '' }
      this.dialogVisible = true
    },
    openEdit (row) {
      this.dialogTitle = '编辑公告'
      this.dataForm = { id: row.id, title: row.title || '', content: row.content || '' }
      this.dialogVisible = true
    },
    resetForm () {
      if (this.$refs.dataFormRef) this.$refs.dataFormRef.resetFields()
    },
    submitForm () {
      this.$refs.dataFormRef.validate((ok) => {
        if (!ok) return
        const isEdit = this.dataForm.id != null
        const url = isEdit ? '/sys/bas/notice/update' : '/sys/bas/notice/save'
        this.$http({
          url: this.$http.adornUrl(url),
          method: 'post',
          data: this.$http.adornData({ ...this.dataForm })
        })
          .then(({ data }) => {
            if (data && data.code === 0) {
              this.$message.success('保存成功')
              this.dialogVisible = false
              this.fetchList()
            } else {
              this.$message.error((data && data.msg) || '失败')
            }
          })
          .catch(() => this.$message.error('请求失败'))
      })
    },
    removeOne (row) {
      this.$confirm('确定删除该公告？', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/notice/delete'),
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
.mod-bas-notice {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
</style>
