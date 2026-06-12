<template>
  <div class="mod-bas-membership">
    <el-form :inline="true">
      <el-form-item>
        <el-button type="primary" @click="getList()">刷新</el-button>
        <el-button type="primary" @click="openAdd()">新增会员卡</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="loading" style="width: 100%;">
      <el-table-column type="index" width="50" />
      <el-table-column prop="cardKind" label="类型编码" width="110" header-align="center" align="center" />
      <el-table-column label="权益" width="88" header-align="center" align="center">
        <template slot-scope="scope">期限畅约</template>
      </el-table-column>
      <el-table-column prop="cardName" label="名称" min-width="120" header-align="center" align="center" />
      <el-table-column label="价格(元)" width="110" header-align="center" align="center">
        <template slot-scope="scope">{{ formatPrice(scope.row.priceYuan) }}</template>
      </el-table-column>
      <el-table-column prop="validityDays" label="有效天数" width="100" header-align="center" align="center" />
      <el-table-column prop="benefitDesc" label="权益说明" min-width="200" show-overflow-tooltip header-align="center" />
      <el-table-column prop="sortOrder" label="排序" width="70" header-align="center" align="center" />
      <el-table-column label="上架" width="90" header-align="center" align="center" fixed="right">
        <template slot-scope="scope">
          <el-switch
            :value="scope.row.onShelf === 1"
            @change="(v) => setShelf(scope.row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" header-align="center" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" class="danger-text" @click="removeOne(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" :close-on-click-modal="false" @close="resetForm">
      <el-form :model="dataForm" :rules="rules" ref="dataFormRef" label-width="110px">
        <el-form-item label="类型" prop="cardKind">
          <el-select v-model="dataForm.cardKind" placeholder="选择" style="width: 100%;">
            <el-option label="月卡 MONTH" value="MONTH" />
            <el-option label="季卡 QUARTER" value="QUARTER" />
            <el-option label="年卡 YEAR" value="YEAR" />
            <el-option label="自定义 OTHER" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="展示名称" prop="cardName">
          <el-input v-model="dataForm.cardName" placeholder="如：畅学月卡" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="价格(元)" prop="priceYuan">
          <el-input-number v-model="dataForm.priceYuan" :min="0" :precision="2" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="有效天数" prop="validityDays">
          <el-input-number v-model="dataForm.validityDays" :min="1" :max="3650" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="权益说明" prop="benefitDesc">
          <el-input v-model="dataForm.benefitDesc" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：有效期内无限次预约免费" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="dataForm.sortOrder" :min="0" :max="9999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="dataForm.onShelf" :active-value="1" :inactive-value="0" />
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
  name: 'BasMembershipCard',
  data () {
    return {
      dataList: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '新增会员卡',
      dataForm: {
        id: null,
        cardKind: 'MONTH',
        cardName: '',
        priceYuan: 0,
        validityDays: 30,
        benefitDesc: '',
        sortOrder: 0,
        onShelf: 1
      },
      rules: {
        cardKind: [{ required: true, message: '必选', trigger: 'change' }],
        cardName: [{ required: true, message: '必填', trigger: 'blur' }],
        priceYuan: [{ required: true, message: '必填', trigger: 'blur' }],
        validityDays: [{ required: true, message: '必填', trigger: 'blur' }]
      }
    }
  },
  created () {
    this.getList()
  },
  methods: {
    formatPrice (v) {
      if (v == null || v === '') return '-'
      const n = Number(v)
      return Number.isFinite(n) ? n.toFixed(2) : String(v)
    },
    getList () {
      this.loading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/membership-card/list'),
        method: 'get',
        params: this.$http.adornParams()
      })
        .then(({ data }) => {
          this.loading = false
          if (data && data.code === 0) {
            this.dataList = data.data || []
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
      this.dialogTitle = '新增会员卡'
      this.dataForm = {
        id: null,
        cardKind: 'MONTH',
        cardName: '',
        priceYuan: 0,
        validityDays: 30,
        benefitDesc: '',
        sortOrder: 0,
        onShelf: 1
      }
      this.dialogVisible = true
    },
    openEdit (row) {
      this.dialogTitle = '编辑会员卡'
      this.dataForm = {
        id: row.id,
        cardKind: row.cardKind || 'OTHER',
        cardName: row.cardName || '',
        priceYuan: row.priceYuan != null ? Number(row.priceYuan) : 0,
        validityDays: row.validityDays != null ? row.validityDays : 30,
        benefitDesc: row.benefitDesc || '',
        sortOrder: row.sortOrder != null ? row.sortOrder : 0,
        onShelf: row.onShelf === 1 ? 1 : 0
      }
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
        const url = isEdit ? '/sys/bas/membership-card/update' : '/sys/bas/membership-card/save'
        this.$http({
          url: this.$http.adornUrl(url),
          method: 'post',
          data: this.$http.adornData({ ...this.dataForm })
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
          .catch(() => this.$message.error('请求失败'))
      })
    },
    setShelf (row, on) {
      this.$http({
        url: this.$http.adornUrl('/sys/bas/membership-card/shelf'),
        method: 'post',
        data: this.$http.adornData({ id: row.id, onShelf: on ? 1 : 0 })
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.$message.success(on ? '已上架' : '已下架')
            row.onShelf = on ? 1 : 0
          } else {
            this.$message.error((data && data.msg) || '操作失败')
          }
        })
        .catch(() => this.$message.error('请求失败'))
    },
    removeOne (row) {
      this.$confirm('确定删除该会员卡类型？', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/membership-card/delete'),
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
.mod-bas-membership {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
</style>
