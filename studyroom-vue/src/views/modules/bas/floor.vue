<template>
  <div class="mod-bas-floor">
    <el-form :inline="true">
      <el-form-item>
        <el-button type="primary" @click="getList()">刷新</el-button>
        <el-button type="primary" @click="openAdd()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="dataListLoading" style="width: 100%;">
      <el-table-column type="index" width="50" />
      <el-table-column prop="floorName" label="楼层名称" header-align="center" align="center" />
      <el-table-column label="操作" width="200" header-align="center" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" class="danger-text" @click="removeOne(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" :close-on-click-modal="false" width="400px" @close="resetForm()">
      <el-form :model="dataForm" :rules="rules" ref="dataFormRef" label-width="100px">
        <el-form-item label="楼层名称" prop="floorName">
          <el-input v-model="dataForm.floorName" placeholder="如：1F" maxlength="100" show-word-limit />
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
  name: 'BasFloor',
  data () {
    return {
      dataList: [],
      dataListLoading: false,
      dialogVisible: false,
      dialogTitle: '新增楼层',
      dataForm: { id: null, floorName: '' },
      rules: {
        floorName: [
          { required: true, message: '名称不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  created () {
    this.getList()
  },
  methods: {
    getList () {
      this.dataListLoading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/floor/list'),
        method: 'get',
        params: this.$http.adornParams()
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
    },
    openAdd () {
      this.dialogTitle = '新增楼层'
      this.dataForm = { id: null, floorName: '' }
      this.dialogVisible = true
    },
    openEdit (row) {
      this.dialogTitle = '编辑楼层'
      this.dataForm = { id: row.id, floorName: row.floorName || row.floor || '' }
      this.dialogVisible = true
    },
    resetForm () {
      if (this.$refs.dataFormRef) {
        this.$refs.dataFormRef.resetFields()
      }
    },
    submitForm () {
      this.$refs.dataFormRef.validate((valid) => {
        if (!valid) return
        const isEdit = this.dataForm.id != null
        const url = isEdit ? '/sys/bas/floor/update' : '/sys/bas/floor/save'
        this.$http({
          url: this.$http.adornUrl(url),
          method: 'post',
          data: this.$http.adornData(
            { id: this.dataForm.id, floorName: this.dataForm.floorName }
          )
        })
          .then(({ data }) => {
            if (data && data.code === 0) {
              this.$message.success('保存成功')
              this.dialogVisible = false
              this.getList()
            } else {
              this.$message.error((data && data.msg) || '失败')
            }
          })
          .catch(() => {
            this.$message.error('请求失败')
          })
      })
    },
    removeOne (row) {
      this.$confirm('确定删除该楼层？如楼下仍有自习室将无法删除。', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/floor/delete'),
            method: 'post',
            data: this.$http.adornData({ id: row.id })
          })
            .then(({ data }) => {
              if (data && data.code === 0) {
                this.$message.success('已删除')
                this.getList()
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
.mod-bas-floor {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
</style>
