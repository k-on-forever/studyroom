<template>
  <div class="mod-bas-seat">
    <el-form :inline="true">
      <el-form-item label="自习室">
        <el-select v-model="qRoomId" clearable filterable placeholder="全部" @change="getList()" style="min-width: 220px">
          <el-option v-for="o in roomOptions" :key="o.id" :label="o.label" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList()">查询</el-button>
        <el-button type="primary" @click="openAdd()">新增</el-button>
        <el-button type="success" @click="openBatch()">按行列生成</el-button>
        <el-button @click="openPreview()" :disabled="!qRoomId">预览座位图</el-button>
        <el-button type="warning" plain @click="goEditor()" :disabled="!qRoomId">可视化编辑器</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" border stripe v-loading="dataListLoading" style="width: 100%;">
      <el-table-column type="index" width="50" />
      <el-table-column prop="placeLabel" label="所在自习室" header-align="center" align="center" min-width="200" />
      <el-table-column prop="seatName" label="座位名" header-align="center" align="center" min-width="100" />
      <el-table-column label="行" width="60" header-align="center" align="center" prop="gridRow" />
      <el-table-column label="列" width="60" header-align="center" align="center" prop="gridCol" />
      <el-table-column label="类型" width="90" header-align="center" align="center">
        <template slot-scope="scope">{{ seatTypeLabel(scope.row.seatType) }}</template>
      </el-table-column>
      <el-table-column label="时段策略" width="100" header-align="center" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openRules(scope.row)">配置</el-button>
        </template>
      </el-table-column>
      <el-table-column label="禁用(维修)" width="110" header-align="center" align="center" fixed="right">
        <template slot-scope="scope">
          <el-switch
            :value="scope.row.locked === 1"
            active-color="#f56c6c"
            @change="(v) => setLock(scope.row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" header-align="center" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" class="danger-text" @click="removeOne(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" :close-on-click-modal="false" width="500px" @close="resetForm()">
      <el-form :model="dataForm" :rules="rules" ref="dataFormRef" label-width="100px">
        <el-form-item label="所属自习室" prop="roomId">
          <el-select v-model="dataForm.roomId" filterable placeholder="选择自习室" style="width: 100%;">
            <el-option v-for="o in roomOptions" :key="o.id" :label="o.label" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位名称" prop="seatName">
          <el-input v-model="dataForm.seatName" placeholder="如：A-01" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="行/列">
          <el-col :span="10">
            <el-input-number v-model="dataForm.gridRow" :min="0" :max="500" size="small" style="width: 100%;" />
          </el-col>
          <el-col :span="2" style="text-align:center">×</el-col>
          <el-col :span="10">
            <el-input-number v-model="dataForm.gridCol" :min="0" :max="500" size="small" style="width: 100%;" />
          </el-col>
        </el-form-item>
        <el-form-item label="座位类型">
          <el-select v-model="dataForm.seatType" style="width: 100%;">
            <el-option :value="0" label="单人座" />
            <el-option :value="1" label="双人座" />
            <el-option :value="2" label="包厢" />
          </el-select>
        </el-form-item>
        <el-form-item label="禁用座位">
          <el-switch v-model="dataForm.locked" :active-value="1" :inactive-value="0" active-color="#f56c6c" />
          <span class="hint-inline">开启后不可预约（如维修）</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm()">确定</el-button>
      </span>
    </el-dialog>
    <el-dialog title="按行列批量生成" :visible.sync="batchVisible" width="480px" :close-on-click-modal="false">
      <p class="batch-hint">会删除本自习室下<strong>尚无预约</strong>的旧座位后，按「名称前缀-行-列」重新生成。若任一座位已有预约，将拒绝操作。</p>
      <el-form :model="batchForm" ref="batchFormRef" label-width="100px">
        <el-form-item label="自习室" required>
          <el-select v-model="batchForm.roomId" filterable placeholder="选择" style="width: 100%;">
            <el-option v-for="o in roomOptions" :key="o.id" :label="o.label" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="行数" required>
          <el-input-number v-model="batchForm.rows" :min="1" :max="200" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="列数" required>
          <el-input-number v-model="batchForm.cols" :min="1" :max="200" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="名称前缀">
          <el-input v-model="batchForm.namePrefix" placeholder="默认 S" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="doBatch()">生成</el-button>
      </span>
    </el-dialog>
    <el-dialog :title="'时段策略 — ' + (rulesSeat && rulesSeat.seatName ? rulesSeat.seatName : '')" :visible.sync="rulesDialogVisible" width="900px" append-to-body @close="rulesSeat = null">
      <p class="rule-intro">在日期范围内，按<strong>每日相同时段</strong>生效（须落在营业 <strong>08:00–22:30</strong> 内）：<strong>全员不可订</strong>，或<strong>门店授权</strong>（仅名单内手机号可订）。白名单请填顾客<strong>注册手机号</strong>（与小程序登录账号一致，系统比对时只保留数字，如 13800138000）；顾客未绑手机或号码不一致将无法预约。与座位「禁用(维修)」可同时使用。</p>
      <div style="margin-bottom:10px">
        <el-button type="primary" size="small" :disabled="!rulesSeat" @click="openRuleEdit(null)">新增策略</el-button>
        <el-button size="small" @click="loadRulesList" :loading="rulesLoading">刷新</el-button>
      </div>
      <el-table :data="rulesList" border stripe size="small" v-loading="rulesLoading" max-height="360">
        <el-table-column prop="dateFrom" label="起止日期" min-width="170" align="center">
          <template slot-scope="s">{{ s.row.dateFrom }} ~ {{ s.row.dateTo }}</template>
        </el-table-column>
        <el-table-column label="每日时段" width="130" align="center">
          <template slot-scope="s">{{ (s.row.timeFrom || '').toString().slice(0,5) }} — {{ (s.row.timeTo || '').toString().slice(0,5) }}</template>
        </el-table-column>
        <el-table-column label="模式" width="120" align="center">
          <template slot-scope="s">{{ (s.row.lockMode === 1) ? '门店授权' : '全员不可订' }}</template>
        </el-table-column>
        <el-table-column prop="whitelistUserIds" label="手机号白名单" min-width="200" show-overflow-tooltip />
        <el-table-column label="启用" width="80" align="center">
          <template slot-scope="s">
            <el-switch :value="s.row.enabled === 1" @change="(v) => toggleRuleEnabled(s.row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template slot-scope="s">
            <el-button type="text" size="small" @click="openRuleEdit(s.row)">编辑</el-button>
            <el-button type="text" size="small" class="danger-text" @click="removeRule(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <el-dialog :title="ruleEditTitle" :visible.sync="ruleEditVisible" width="520px" append-to-body @close="resetRuleForm">
      <el-form :model="ruleForm" label-width="110px" size="small">
        <el-form-item label="日期范围" required>
          <el-date-picker v-model="ruleForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="每日时段" required>
          <el-col :span="11">
            <el-time-select v-model="ruleForm.timeFrom" :picker-options="ruleBizTimeFromOpts" placeholder="开始" style="width:100%" @change="onRuleTimeFromChange" />
          </el-col>
          <el-col :span="2" style="text-align:center">—</el-col>
          <el-col :span="11">
            <el-time-select v-model="ruleForm.timeTo" :picker-options="ruleBizTimeToOpts" placeholder="结束" style="width:100%" />
          </el-col>
        </el-form-item>
        <el-form-item label="模式" required>
          <el-radio-group v-model="ruleForm.lockMode">
            <el-radio :label="0">该时段全员不可订</el-radio>
            <el-radio :label="1">仅以下手机号可订</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleForm.lockMode === 1" label="手机号" required>
          <el-input v-model="ruleForm.whitelistText" type="textarea" :rows="2" placeholder='逗号分隔或 JSON，如 13800138000,13900139000 或 ["13800138000"]（与注册手机号一致，比对时只取数字）' />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ruleForm.remark" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="ruleEditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRule">保存</el-button>
      </span>
    </el-dialog>
    <el-dialog title="座位平面图预览" :visible.sync="previewVisible" width="640px" append-to-body>
      <div class="preview-grid" :style="{ gridTemplateColumns: 'repeat(' + previewCols + ', 1fr)' }">
        <div
          v-for="cell in previewCells"
          :key="cell.key"
          class="preview-cell"
          :class="{ 'preview-cell--seat': cell.seat, 't1': cell.seatType === 1, 't2': cell.seatType === 2 }"
        >
          {{ cell.label }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'
export default {
  name: 'BasSeat',
  data () {
    return {
      dataList: [],
      dataListLoading: false,
      roomOptions: [],
      qRoomId: null,
      roomLabelById: {},
      dialogVisible: false,
      batchVisible: false,
      dialogTitle: '新增座位',
      dataForm: { id: null, roomId: null, seatName: '', gridRow: 0, gridCol: 0, seatType: 0, locked: 0 },
      previewVisible: false,
      previewCols: 6,
      previewCells: [],
      batchForm: { roomId: null, rows: 3, cols: 4, namePrefix: 'S' },
      rulesDialogVisible: false,
      rulesSeat: null,
      rulesList: [],
      rulesLoading: false,
      ruleEditVisible: false,
      ruleEditTitle: '新增时段策略',
      ruleForm: {
        id: null,
        dateRange: [],
        timeFrom: '09:00',
        timeTo: '12:00',
        lockMode: 0,
        whitelistText: '',
        remark: '',
        enabled: 1
      },
      rules: {
        roomId: [{ required: true, message: '请选择自习室', trigger: 'change' }],
        seatName: [{ required: true, message: '不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    /** 与后端 study.reservation day-start / day-end（08:00–22:30）一致 */
    ruleBizTimeFromOpts () {
      return { start: '08:00', step: '00:30', end: '22:30' }
    },
    ruleBizTimeToOpts () {
      const min = this.ruleForm.timeFrom || '08:00'
      return { start: '08:00', step: '00:30', end: '22:30', minTime: min }
    }
  },
  created () {
    this.loadRoomOptions()
      .finally(() => { this.getList() })
  },
  methods: {
    /** 开始时间推后时，若结束早于开始则清空结束，避免落在 minTime 外 */
    onRuleTimeFromChange () {
      const f = this.ruleForm.timeFrom
      const t = this.ruleForm.timeTo
      if (!f || !t) return
      if (String(t) <= String(f)) {
        this.ruleForm.timeTo = ''
      }
    },
    seatTypeLabel (t) {
      if (t === 1) return '双人'
      if (t === 2) return '包厢'
      return '单人'
    },
    goEditor () {
      this.$router.push({ name: 'bas-seat-editor', query: { roomId: String(this.qRoomId) } })
    },
    openPreview () {
      if (!this.qRoomId) return
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat/list'),
        method: 'get',
        params: this.$http.adornParams({ roomId: this.qRoomId })
      })
        .then(({ data }) => {
          if (!(data && data.code === 0)) {
            this.$message.error('加载失败')
            return
          }
          const list = data.data || []
          let maxR = 1; let maxC = 1
          list.forEach((s) => {
            const r = s.gridRow || 0; const c = s.gridCol || 0
            if (r > maxR) maxR = r
            if (c > maxC) maxC = c
          })
          maxR = Math.max(maxR, 6)
          maxC = Math.max(maxC, 6)
          this.previewCols = maxC
          const cells = []
          for (let r = 1; r <= maxR; r++) {
            for (let c = 1; c <= maxC; c++) {
              const seat = list.find((s) => (s.gridRow || 0) === r && (s.gridCol || 0) === c)
              cells.push({
                key: r + '-' + c,
                seat: !!seat,
                label: seat ? (seat.seatName || '') : '·',
                seatType: seat ? (seat.seatType != null ? seat.seatType : 0) : 0
              })
            }
          }
          this.previewCells = cells
          this.previewVisible = true
        })
    },
    loadRoomOptions () {
      const p1 = this.$http({
        url: this.$http.adornUrl('/sys/bas/floor/list'),
        method: 'get',
        params: this.$http.adornParams()
      })
      const p2 = this.$http({
        url: this.$http.adornUrl('/sys/bas/studyroom/list'),
        method: 'get',
        params: this.$http.adornParams()
      })
      return Promise.all([p1, p2])
        .then(([a, b]) => {
          if (!(a && a.data && a.data.code === 0) || !(b && b.data && b.data.code === 0)) {
            return
          }
          const floors = a.data.data || []
          const fmap = {}
          floors.forEach((f) => { fmap[f.id] = f.floorName || f.floor || '' })
          const rooms = b.data.data || []
          this.roomOptions = rooms.map((r) => ({
            id: r.id,
            label: (fmap[r.floorId] || '?') + ' - ' + (r.roomName || '未命名')
          }))
          this.roomLabelById = {}
          this.roomOptions.forEach((o) => { this.roomLabelById[o.id] = o.label })
        })
        .catch((e) => {
          this.$message.error(httpConnErrorMessage(e))
        })
    },
    getList () {
      this.dataListLoading = true
      const params = this.$http.adornParams()
      if (this.qRoomId) {
        params.roomId = this.qRoomId
      }
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat/list'),
        method: 'get',
        params: params
      })
        .then(({ data }) => {
          this.dataListLoading = false
          if (data && data.code === 0) {
            const rows = data.data || []
            this.dataList = rows.map((r) => ({
              ...r,
              placeLabel: this.roomLabelById[r.roomId] || '自习室#' + r.roomId,
              locked: r.locked === 1 || r.locked === '1' ? 1 : 0
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
      this.dialogTitle = '新增座位'
      this.dataForm = { id: null, roomId: this.qRoomId || null, seatName: '', gridRow: 0, gridCol: 0, seatType: 0, locked: 0 }
      this.dialogVisible = true
    },
    openEdit (row) {
      this.dialogTitle = '编辑座位'
      this.dataForm = {
        id: row.id,
        roomId: row.roomId,
        seatName: row.seatName,
        gridRow: row.gridRow != null ? row.gridRow : 0,
        gridCol: row.gridCol != null ? row.gridCol : 0,
        seatType: row.seatType != null ? row.seatType : 0,
        locked: row.locked === 1 ? 1 : 0
      }
      this.dialogVisible = true
    },
    openBatch () {
      this.batchForm = {
        roomId: this.qRoomId,
        rows: 3,
        cols: 4,
        namePrefix: 'S'
      }
      this.batchVisible = true
    },
    doBatch () {
      if (!this.batchForm.roomId) {
        this.$message.error('请选择自习室')
        return
      }
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat/batch-generate'),
        method: 'post',
        data: this.$http.adornData({
          roomId: this.batchForm.roomId,
          rows: this.batchForm.rows,
          cols: this.batchForm.cols,
          namePrefix: this.batchForm.namePrefix || 'S'
        })
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.$message.success('已按行列重排本室座位')
            this.batchVisible = false
            this.getList()
          } else {
            this.$message.error((data && data.msg) || '失败')
          }
        })
        .catch(() => this.$message.error('请求失败'))
    },
    setLock (row, on) {
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat/lock'),
        method: 'post',
        data: this.$http.adornData({ id: row.id, locked: on ? 1 : 0 })
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.$message.success(on ? '已禁用' : '已启用')
            row.locked = on ? 1 : 0
          } else {
            this.$message.error((data && data.msg) || '操作失败')
          }
        })
        .catch(() => this.$message.error('请求失败'))
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
        const url = isEdit ? '/sys/bas/seat/update' : '/sys/bas/seat/save'
        const body = { ...this.dataForm }
        this.$http({
          url: this.$http.adornUrl(url),
          method: 'post',
          data: this.$http.adornData(body)
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
    openRules (row) {
      this.rulesSeat = row
      this.rulesDialogVisible = true
      this.loadRulesList()
    },
    loadRulesList () {
      if (!this.rulesSeat || !this.rulesSeat.id) return
      this.rulesLoading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat-access-rule/list'),
        method: 'get',
        params: this.$http.adornParams({ seatId: this.rulesSeat.id })
      })
        .then(({ data }) => {
          this.rulesLoading = false
          if (data && data.code === 0) {
            this.rulesList = data.data || []
          } else {
            this.$message.error((data && data.msg) || '加载策略失败（请先在库执行 bas_seat_access_rule 相关 migration）')
          }
        })
        .catch((e) => {
          this.rulesLoading = false
          this.$message.error(httpConnErrorMessage(e))
        })
    },
    openRuleEdit (row) {
      if (!this.rulesSeat) return
      if (row) {
        this.ruleEditTitle = '编辑时段策略'
        this.ruleForm = {
          id: row.id,
          dateRange: [row.dateFrom, row.dateTo],
          timeFrom: (row.timeFrom && String(row.timeFrom).slice(0, 5)) || '09:00',
          timeTo: (row.timeTo && String(row.timeTo).slice(0, 5)) || '12:00',
          lockMode: row.lockMode === 1 ? 1 : 0,
          whitelistText: row.whitelistUserIds != null ? String(row.whitelistUserIds) : '',
          remark: row.remark || '',
          enabled: row.enabled === 1 ? 1 : 0
        }
      } else {
        this.ruleEditTitle = '新增时段策略'
        const t = new Date()
        const y = t.getFullYear()
        const m = String(t.getMonth() + 1).padStart(2, '0')
        const d = String(t.getDate()).padStart(2, '0')
        const today = y + '-' + m + '-' + d
        this.ruleForm = {
          id: null,
          dateRange: [today, today],
          timeFrom: '09:00',
          timeTo: '12:00',
          lockMode: 0,
          whitelistText: '',
          remark: '',
          enabled: 1
        }
      }
      this.ruleEditVisible = true
    },
    resetRuleForm () {
      this.ruleForm = {
        id: null,
        dateRange: [],
        timeFrom: '09:00',
        timeTo: '12:00',
        lockMode: 0,
        whitelistText: '',
        remark: ''
      }
    },
    submitRule () {
      if (!this.rulesSeat) return
      const dr = this.ruleForm.dateRange
      if (!dr || dr.length !== 2) {
        this.$message.error('请选择日期范围')
        return
      }
      if (!this.ruleForm.timeFrom || !this.ruleForm.timeTo) {
        this.$message.error('请填写每日时段')
        return
      }
      if (String(this.ruleForm.timeTo) <= String(this.ruleForm.timeFrom)) {
        this.$message.error('结束时间须晚于开始时间')
        return
      }
      const body = {
        id: this.ruleForm.id,
        seatId: this.rulesSeat.id,
        dateFrom: dr[0],
        dateTo: dr[1],
        timeFrom: this.ruleForm.timeFrom,
        timeTo: this.ruleForm.timeTo,
        lockMode: this.ruleForm.lockMode,
        whitelistUserIds: this.ruleForm.lockMode === 1 ? this.ruleForm.whitelistText : null,
        enabled: this.ruleForm.enabled === 0 ? 0 : 1,
        remark: this.ruleForm.remark || null
      }
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat-access-rule/save'),
        method: 'post',
        data: this.$http.adornData(body)
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.$message.success('已保存')
            this.ruleEditVisible = false
            this.loadRulesList()
          } else {
            this.$message.error((data && data.msg) || '保存失败')
          }
        })
        .catch((e) => this.$message.error(httpConnErrorMessage(e)))
    },
    removeRule (row) {
      this.$confirm('确定删除该条时段策略？', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/seat-access-rule/delete'),
            method: 'post',
            data: this.$http.adornData({ id: row.id })
          })
            .then(({ data }) => {
              if (data && data.code === 0) {
                this.$message.success('已删除')
                this.loadRulesList()
              } else {
                this.$message.error((data && data.msg) || '删除失败')
              }
            })
        })
        .catch(() => {})
    },
    toggleRuleEnabled (row, on) {
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat-access-rule/toggle-enabled'),
        method: 'post',
        data: this.$http.adornData({ id: row.id, enabled: on ? 1 : 0 })
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            row.enabled = on ? 1 : 0
          } else {
            this.$message.error((data && data.msg) || '更新失败')
            this.loadRulesList()
          }
        })
        .catch(() => this.loadRulesList())
    },
    removeOne (row) {
      this.$confirm('确定删除该座位？若已有预约将无法删除。', '提示', { type: 'warning' })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl('/sys/bas/seat/delete'),
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
.mod-bas-seat {
  padding: 12px 16px;
}
.danger-text {
  color: #f56c6c;
}
.batch-hint {
  font-size: 13px;
  color: #e6a23c;
  margin: 0 0 12px 0;
  line-height: 1.5;
}
.hint-inline {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
.preview-grid {
  display: grid;
  gap: 6px;
}
.preview-cell {
  min-height: 40px;
  border: 1px dashed #e4e7ed;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  text-align: center;
  padding: 4px;
  color: #c0c4cc;
}
.preview-cell--seat {
  border-style: solid;
  border-color: #409eff;
  color: #303133;
  font-weight: 600;
  background: #ecf5ff;
}
.preview-cell.t1 { border-color: #67c23a; background: #f0f9eb; }
.preview-cell.t2 { border-color: #e6a23c; background: #fdf6ec; }
.rule-intro {
  font-size: 13px;
  color: #606266;
  line-height: 1.55;
  margin: 0 0 12px 0;
}
</style>
