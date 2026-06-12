<template>
  <div class="mod-bas-studyroom">
    <el-form :inline="true">
      <el-form-item label="楼层">
        <el-select v-model="qFloorId" clearable placeholder="全部" @change="getList()" style="min-width: 180px">
          <el-option v-for="f in floorOptions" :key="f.id" :label="(f.floorName || f.floor)" :value="f.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList()">查询</el-button>
        <el-button type="primary" @click="openAdd()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="dataListLoading" style="width: 100%;">
      <el-table-column type="index" width="50" />
      <el-table-column prop="floorName" label="所属楼层" header-align="center" align="center" min-width="100" />
      <el-table-column prop="roomName" label="自习室" header-align="center" align="center" min-width="120" />
      <el-table-column prop="roomLocation" label="地址/位置" header-align="center" show-overflow-tooltip min-width="120" />
      <el-table-column label="排座" width="100" header-align="center" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.seatRows > 0 && scope.row.seatCols > 0">
            {{ scope.row.seatRows }}×{{ scope.row.seatCols }}
          </span>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="openingTime" label="开放开始" width="100" header-align="center" align="center" />
      <el-table-column prop="closeTime" label="开放结束" width="100" header-align="center" align="center" />
      <el-table-column label="时段粒度" width="90" header-align="center" align="center">
        <template slot-scope="scope">{{ (scope.row.slotStepMinutes != null ? scope.row.slotStepMinutes : 10) }} 分钟</template>
      </el-table-column>
      <el-table-column label="操作" width="200" header-align="center" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" class="danger-text" @click="removeOne(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" :close-on-click-modal="false" width="520px" @close="resetForm()">
      <el-form :model="dataForm" :rules="rules" ref="dataFormRef" label-width="100px">
        <el-form-item label="所属楼层" prop="floorId">
          <el-select v-model="dataForm.floorId" placeholder="选择楼层" style="width: 100%;">
            <el-option v-for="f in floorOptions" :key="f.id" :label="(f.floorName || f.floor)" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="roomName">
          <el-input v-model="dataForm.roomName" placeholder="自习室名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="地址" prop="roomLocation">
          <el-input v-model="dataForm.roomLocation" type="textarea" :rows="2" placeholder="详细地址或区域说明（可选）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="开放开始" prop="openingTime">
          <el-input v-model="dataForm.openingTime" placeholder="如 08:00，须在全局营业时间内；留空则不单独限制" />
        </el-form-item>
        <el-form-item label="开放结束" prop="closeTime">
          <el-input v-model="dataForm.closeTime" placeholder="如 22:30，须与「开放开始」同时填写才生效" />
        </el-form-item>
        <el-form-item label="时段粒度" prop="slotStepMinutes">
          <el-select v-model="dataForm.slotStepMinutes" placeholder="选择" style="width: 100%;">
            <el-option label="10 分钟/槽" :value="10" />
            <el-option label="30 分钟/槽" :value="30" />
            <el-option label="60 分钟/槽" :value="60" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位行列" prop="seatRows">
          <el-col :span="10">
            <el-input-number v-model="dataForm.seatRows" :min="0" :max="200" size="small" style="width: 100%;" />
          </el-col>
          <el-col :span="2" style="text-align:center">×</el-col>
          <el-col :span="10">
            <el-input-number v-model="dataForm.seatCols" :min="0" :max="200" size="small" style="width: 100%;" />
          </el-col>
        </el-form-item>
        <p class="hint">行列用于记录规划；实际座位请在「座位管理」中按行列批量生成或手工维护。</p>
        <p class="hint">时段粒度须与后端 <code>study.reservation.slot-minutes</code> 一致（仓库默认 10 分钟/槽）；须与本页「时段粒度」相同。</p>
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
  name: 'BasStudyroom',
  data () {
    return {
      dataList: [],
      dataListLoading: false,
      floorOptions: [],
      qFloorId: null,
      floorNameMap: {},
      dialogVisible: false,
      dialogTitle: '新增自习室',
      dataForm: {
        id: null,
        floorId: null,
        roomName: '',
        roomLocation: '',
        openingTime: '',
        closeTime: '',
        seatRows: 0,
        seatCols: 0,
        slotStepMinutes: 10
      },
      rules: {
        floorId: [
          { required: true, message: '请选择楼层', trigger: 'change' }
        ],
        roomName: [
          { required: true, message: '名称不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  created () {
    this.loadFloors()
      .finally(() => { this.getList() })
  },
  methods: {
    loadFloors () {
      return this.$http({
        url: this.$http.adornUrl('/sys/bas/floor/list'),
        method: 'get',
        params: this.$http.adornParams()
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.floorOptions = data.data || []
            this.floorNameMap = {}
            this.floorOptions.forEach((f) => {
              this.floorNameMap[f.id] = f.floorName || f.floor
            })
          }
        })
        .catch((e) => {
          this.$message.error(httpConnErrorMessage(e))
        })
    },
    getList () {
      this.dataListLoading = true
      const params = this.$http.adornParams()
      if (this.qFloorId) {
        params.floorId = this.qFloorId
      }
      this.$http({
        url: this.$http.adornUrl('/sys/bas/studyroom/list'),
        method: 'get',
        params: params
      })
        .then(({ data }) => {
          this.dataListLoading = false
          if (data && data.code === 0) {
            const rows = data.data || []
            this.dataList = rows.map((r) => ({
              ...r,
              floorName: this.floorNameMap[r.floorId] || '-'
            }))
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
      this.dialogTitle = '新增自习室'
      this.dataForm = {
        id: null,
        floorId: this.qFloorId || null,
        roomName: '',
        roomLocation: '',
        openingTime: '',
        closeTime: '',
        seatRows: 0,
        seatCols: 0,
        slotStepMinutes: 10
      }
      this.dialogVisible = true
    },
    openEdit (row) {
      this.dialogTitle = '编辑自习室'
      this.dataForm = {
        id: row.id,
        floorId: row.floorId,
        roomName: row.roomName,
        roomLocation: row.roomLocation || '',
        openingTime: row.openingTime || '',
        closeTime: row.closeTime || '',
        seatRows: row.seatRows != null ? row.seatRows : 0,
        seatCols: row.seatCols != null ? row.seatCols : 0,
        slotStepMinutes: row.slotStepMinutes != null ? row.slotStepMinutes : 10
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
        const url = isEdit ? '/sys/bas/studyroom/update' : '/sys/bas/studyroom/save'
        this.$http({
          url: this.$http.adornUrl(url),
          method: 'post',
          data: this.$http.adornData(
            { ...this.dataForm,
              openingTime: this.dataForm.openingTime || null,
              closeTime: this.dataForm.closeTime || null,
              roomLocation: this.dataForm.roomLocation || null
            }
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
      this.$confirm('确定删除该自习室？若该室下仍有座位将无法删除。', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/studyroom/delete'),
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
.mod-bas-studyroom {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
.text-muted {
  color: #999;
  font-size: 12px;
}
.hint {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #999;
  padding-left: 100px;
  line-height: 1.4;
}
</style>
