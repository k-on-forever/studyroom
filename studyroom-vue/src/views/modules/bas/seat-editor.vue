<template>
  <div class="mod-seat-editor">
    <el-alert type="info" show-icon :closable="false" title="在网格中点击空格添加座位，点击已有座位可改类型/删除。行列从 1 起，与数据库 grid_row/grid_col 一致。保存后与小程序坐标平面图联动。" style="margin-bottom:12px" />
    <el-form :inline="true">
      <el-form-item label="自习室">
        <el-select v-model="roomId" filterable placeholder="选择" style="min-width:280px" @change="loadSeats">
          <el-option v-for="o in roomOptions" :key="o.id" :label="o.label" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="画布行">
        <el-input-number v-model="canvasRows" :min="1" :max="24" />
      </el-form-item>
      <el-form-item label="画布列">
        <el-input-number v-model="canvasCols" :min="1" :max="24" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="applyCanvas">应用画布尺寸</el-button>
        <el-button type="success" @click="saveLayout">保存布局</el-button>
      </el-form-item>
    </el-form>
    <div class="seat-legend" v-if="roomId">
      <span class="legend-item"><span class="legend-dot legend-dot--empty"></span>空位</span>
      <span class="legend-item"><span class="legend-dot legend-dot--single"></span>单人座</span>
      <span class="legend-item"><span class="legend-dot legend-dot--double"></span>双人座</span>
      <span class="legend-item"><span class="legend-dot legend-dot--room"></span>包厢</span>
      <span class="legend-stats">共 {{ seats.length }} 个座位</span>
    </div>
    <div class="grid-wrap" v-if="roomId">
      <div class="grid" :style="gridStyle">
        <div
          v-for="cell in flatCells"
          :key="cell.key"
          class="cell"
          :class="cellClass(cell)"
          draggable="true"
          @click="onCellClick(cell)"
          @dragstart="onDragStart(cell, $event)"
          @dragover.prevent
          @drop="onDrop(cell, $event)"
        >
          <span class="cell-txt">{{ cell.label }}</span>
          <span class="cell-type-badge" v-if="cell.seat">{{ seatTypeBadge(cell.seatType) }}</span>
        </div>
      </div>
    </div>
    <el-dialog :title="dlgTitle" :visible.sync="dlg" width="420px" append-to-body>
      <el-form label-width="90px" v-if="editCell">
        <el-form-item label="名称">
          <el-input v-model="editName" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editType" style="width:100%">
            <el-option :value="0" label="单人座" />
            <el-option :value="1" label="双人座" />
            <el-option :value="2" label="包厢" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editCell.seat">
          <el-checkbox v-model="editDelete">删除该座位</el-checkbox>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="applyEdit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'
export default {
  name: 'BasSeatEditor',
  data () {
    return {
      roomOptions: [],
      roomId: null,
      canvasRows: 6,
      canvasCols: 6,
      seats: [],
      flatCells: [],
      dlg: false,
      dlgTitle: '',
      editCell: null,
      editName: '',
      editType: 0,
      editDelete: false,
      dragSource: null
    }
  },
  computed: {
    gridStyle () {
      return { gridTemplateColumns: `repeat(${this.canvasCols}, 1fr)` }
    }
  },
  mounted () {
    this.loadRoomOptions().then(() => {
      const q = this.$route.query.roomId
      if (q) {
        this.roomId = Number(q)
        this.loadSeats()
      }
    })
  },
  methods: {
    cellClass (cell) {
      return {
        'cell--seat': cell.seat,
        'cell--type0': cell.seat && cell.seatType === 0,
        'cell--type1': cell.seat && cell.seatType === 1,
        'cell--type2': cell.seat && cell.seatType === 2
      }
    },
    seatTypeBadge (t) {
      return t === 1 ? '双' : t === 2 ? '包' : ''
    },
    onDragStart (cell, e) {
      if (!cell.seat) return
      this.dragSource = cell
      e.dataTransfer.effectAllowed = 'move'
    },
    onDrop (target, e) {
      e.preventDefault()
      if (!this.dragSource || this.dragSource.key === target.key) return
      if (target.seat) {
        this.$message.warning('目标位置已有座位，请先移除')
        return
      }
      var src = this.seats.find(s => s.id === this.dragSource.id)
      if (src) {
        src.gridRow = target.r
        src.gridCol = target.c
        this.rebuildFlat()
      }
      this.dragSource = null
    },
    loadRoomOptions () {
      const p1 = this.$http({ url: this.$http.adornUrl('/sys/bas/floor/list'), method: 'get', params: this.$http.adornParams() })
      const p2 = this.$http({ url: this.$http.adornUrl('/sys/bas/studyroom/list'), method: 'get', params: this.$http.adornParams() })
      return Promise.all([p1, p2])
        .then(([a, b]) => {
          if (!(a && a.data && a.data.code === 0) || !(b && b.data && b.data.code === 0)) return
          const floors = a.data.data || []
          const fmap = {}
          floors.forEach((f) => { fmap[f.id] = f.floorName || '' })
          const rooms = b.data.data || []
          this.roomOptions = rooms.map((r) => ({
            id: r.id,
            label: (fmap[r.floorId] || '?') + ' - ' + (r.roomName || '')
          }))
        })
        .catch((e) => this.$message.error(httpConnErrorMessage(e)))
    },
    loadSeats () {
      if (!this.roomId) return
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat/list'),
        method: 'get',
        params: this.$http.adornParams({ roomId: this.roomId })
      })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.seats = (data.data || []).map((s) => ({
              id: s.id,
              gridRow: s.gridRow || 0,
              gridCol: s.gridCol || 0,
              seatName: s.seatName || '',
              seatType: s.seatType != null ? s.seatType : 0,
              locked: s.locked
            }))
            let maxR = 0; let maxC = 0
            this.seats.forEach((s) => {
              if (s.gridRow > maxR) maxR = s.gridRow
              if (s.gridCol > maxC) maxC = s.gridCol
            })
            this.canvasRows = Math.max(this.canvasRows, maxR || 6)
            this.canvasCols = Math.max(this.canvasCols, maxC || 6)
            this.rebuildFlat()
          }
        })
    },
    applyCanvas () {
      this.rebuildFlat()
    },
    rebuildFlat () {
      const list = []
      for (let r = 1; r <= this.canvasRows; r++) {
        for (let c = 1; c <= this.canvasCols; c++) {
          const seat = this.seats.find((s) => s.gridRow === r && s.gridCol === c)
          list.push({
            key: r + '-' + c,
            r,
            c,
            seat: !!seat,
            id: seat ? seat.id : null,
            label: seat ? (seat.seatName || '#' + seat.id) : '+',
            seatType: seat ? seat.seatType : 0
          })
        }
      }
      this.flatCells = list
    },
    onCellClick (cell) {
      this.editCell = cell
      this.dlgTitle = cell.seat ? '编辑座位' : '添加座位'
      this.editName = cell.seat ? (this.seats.find((s) => s.id === cell.id) || {}).seatName || '' : ('S-' + cell.r + '-' + cell.c)
      this.editType = cell.seatType != null ? cell.seatType : 0
      this.editDelete = false
      this.dlg = true
    },
    applyEdit () {
      const cell = this.editCell
      if (!cell) return
      if (cell.seat && this.editDelete) {
        this.seats = this.seats.filter((s) => s.id !== cell.id)
        this.dlg = false
        this.rebuildFlat()
        return
      }
      if (!cell.seat) {
        this.seats.push({
          id: null,
          gridRow: cell.r,
          gridCol: cell.c,
          seatName: this.editName || ('S-' + cell.r + '-' + cell.c),
          seatType: this.editType,
          locked: 0
        })
      } else {
        const s = this.seats.find((x) => x.id === cell.id)
        if (s) {
          s.seatName = this.editName
          s.seatType = this.editType
        }
      }
      this.dlg = false
      this.rebuildFlat()
    },
    saveLayout () {
      if (!this.roomId) {
        this.$message.warning('请选择自习室')
        return
      }
      this.$http({
        url: this.$http.adornUrl('/sys/bas/seat/list'),
        method: 'get',
        params: this.$http.adornParams({ roomId: this.roomId })
      }).then(({ data }) => {
        if (!(data && data.code === 0)) {
          this.$message.error('无法读取旧座位')
          return
        }
        const old = data.data || []
        const keepIds = new Set(this.seats.filter((s) => s.id).map((s) => s.id))
        const cells = []
        old.forEach((o) => {
          if (!keepIds.has(o.id)) {
            cells.push({ id: o.id, r: o.gridRow, c: o.gridCol, deleted: true })
          }
        })
        this.seats.forEach((s) => {
          cells.push({
            id: s.id || undefined,
            r: s.gridRow,
            c: s.gridCol,
            seatName: s.seatName,
            seatType: s.seatType != null ? s.seatType : 0
          })
        })
        this.$http({
          url: this.$http.adornUrl('/sys/bas/seat/layout-save'),
          method: 'post',
          data: this.$http.adornData({ roomId: this.roomId, cells })
        })
          .then(({ data }) => {
            if (data && data.code === 0) {
              this.$message.success('已保存')
              this.loadSeats()
            } else {
              this.$message.error((data && data.msg) || '保存失败')
            }
          })
          .catch((e) => this.$message.error(httpConnErrorMessage(e)))
      })
    }
  }
}
</script>

<style scoped>
.mod-seat-editor { padding: 12px 16px; }
.seat-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid #dcdfe6;
}
.legend-dot--empty { background: #fafafa; }
.legend-dot--single { background: #ecf5ff; border-color: #409eff; }
.legend-dot--double { background: #f0f9eb; border-color: #67c23a; }
.legend-dot--room { background: #fdf6ec; border-color: #e6a23c; }
.legend-stats { margin-left: auto; color: #909399; }
.grid-wrap { overflow: auto; max-width: 100%; }
.grid {
  display: grid;
  gap: 6px;
  max-width: 720px;
}
.cell {
  height: 52px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fafafa;
  font-size: 12px;
  text-align: center;
  padding: 4px;
  word-break: break-all;
  position: relative;
  transition: transform 0.15s, box-shadow 0.15s;
}
.cell:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.cell--seat {
  border-style: solid;
  border-color: #409eff;
  background: #ecf5ff;
  font-weight: 600;
  cursor: grab;
}
.cell--seat:active { cursor: grabbing; }
.cell--type0 { border-color: #409eff; background: #ecf5ff; }
.cell--type1 { border-color: #67c23a; background: #f0f9eb; }
.cell--type2 { border-color: #e6a23c; background: #fdf6ec; }
.cell-txt { line-height: 1.2; }
.cell-type-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 9px;
  background: rgba(0,0,0,0.06);
  border-radius: 2px;
  padding: 0 3px;
  line-height: 1.4;
}
</style>
