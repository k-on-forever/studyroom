<template>
  <div class="mod-reservation-rule">
    <p class="intro">提前预约天数、取消提前量保存后由后端持久化。单次连续预约最长时长<strong>不设独立上限</strong>，与配置 <code>study.reservation.day-start</code> / <code>day-end</code> 的营业总跨度一致（同一天内时段，见小程序与 <code>TimeSlotCodec</code>）。若加载报错，请在 MySQL 业务库执行 <code>schema-studyroom-migration-sys-reservation-rule.sql</code>（或 <code>schema-studyroom-core.sql</code>）。</p>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="180px" v-loading="loading" style="max-width: 560px;">
      <el-form-item label="提前预约（最多几天内）" prop="advanceBookingDays">
        <el-input-number v-model="form.advanceBookingDays" :min="0" :max="365" :step="1" style="width: 220px;" />
        <span class="unit">预约日不得晚于「今天 + N 天」（含当天为 N=0；后端按自然日差校验，常见填 <strong>14</strong>）</span>
      </el-form-item>
      <el-form-item label="单次最长（营业窗）">
        <span v-if="operatingSpanMinutes != null"><strong>{{ operatingSpanMinutes }}</strong> 分钟（只读，由后端按营业起止计算）</span>
        <span v-else class="muted">加载后显示</span>
        <span class="unit">单次预约须落在同一日历日，且不超过当日营业时段。</span>
      </el-form-item>
      <el-form-item label="取消需提前" prop="cancelAdvanceMinutes">
        <el-input-number v-model="form.cancelAdvanceMinutes" :min="0" :max="7 * 24 * 60" :step="5" style="width: 220px;" />
        <span class="unit">距预约开始不足该分钟数则不可在小程序取消</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="load">重新加载</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { httpConnErrorMessage } from '@/utils'
export default {
  name: 'BasReservationRule',
  data () {
    return {
      loading: false,
      operatingSpanMinutes: null,
      form: {
        id: 1,
        advanceBookingDays: 7,
        cancelAdvanceMinutes: 30
      },
      rules: {
        advanceBookingDays: [{ required: true, message: '必填', trigger: 'blur' }],
        cancelAdvanceMinutes: [{ required: true, message: '必填', trigger: 'blur' }]
      }
    }
  },
  created () {
    this.load()
  },
  methods: {
    load () {
      this.loading = true
      this.$http({
        url: this.$http.adornUrl('/sys/bas/reservation-rule/info'),
        method: 'get',
        params: this.$http.adornParams()
      })
        .then(({ data }) => {
          this.loading = false
          if (data && data.code === 0 && data.data) {
            const d = data.data
            this.operatingSpanMinutes = data.operatingSpanMinutes != null ? data.operatingSpanMinutes : d.maxDurationMinutes
            this.form = {
              id: d.id != null ? d.id : 1,
              advanceBookingDays: d.advanceBookingDays != null ? d.advanceBookingDays : 7,
              cancelAdvanceMinutes: d.cancelAdvanceMinutes != null ? d.cancelAdvanceMinutes : 30
            }
          } else {
            this.$message.error((data && data.msg) || '加载失败')
          }
        })
        .catch((e) => {
          this.loading = false
          this.$message.error(httpConnErrorMessage(e))
        })
    },
    save () {
      this.$refs.formRef.validate((ok) => {
        if (!ok) return
        this.$http({
          url: this.$http.adornUrl('/sys/bas/reservation-rule/save'),
          method: 'post',
          data: this.$http.adornData(this.form)
        })
          .then(({ data }) => {
            if (data && data.code === 0) {
              this.$message.success('已保存')
              this.load()
            } else {
              this.$message.error((data && data.msg) || '保存失败')
            }
          })
          .catch((e) => {
            this.$message.error(httpConnErrorMessage(e))
          })
      })
    }
  }
}
</script>

<style scoped>
.mod-reservation-rule {
  padding: 12px 16px;
}
.intro {
  color: #666;
  font-size: 13px;
  max-width: 700px;
  line-height: 1.5;
  margin-bottom: 16px;
}
.unit {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}
.muted {
  color: #999;
  font-size: 13px;
}
</style>
