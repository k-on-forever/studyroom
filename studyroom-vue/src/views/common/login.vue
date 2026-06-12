<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true" />
    <div class="bg-orb bg-orb--1" aria-hidden="true"></div>
    <div class="bg-orb bg-orb--2" aria-hidden="true"></div>
    <div class="login-card">
      <div class="login-card__brand">
        <div class="login-card__logo" aria-hidden="true">SR</div>
        <h1 class="login-card__title">自习室预约管理</h1>
        <p class="login-card__subtitle">Study Room Admin Console</p>
      </div>

      <el-form
        ref="dataForm"
        class="login-form"
        :model="dataForm"
        :rules="dataRule"
        label-position="top"
        status-icon
        @keyup.enter.native="dataFormSubmit()"
      >
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="dataForm.userName"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            clearable
            size="medium"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="dataForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            show-password
            clearable
            size="medium"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="captcha">
          <div class="login-captcha-row">
            <el-input
              v-model="dataForm.captcha"
              placeholder="请输入验证码"
              prefix-icon="el-icon-key"
              maxlength="8"
              clearable
              size="medium"
              class="login-captcha-row__input"
            />
            <div class="login-captcha-row__img-wrap" title="点击刷新验证码">
              <img
                :src="captchaPath"
                alt="验证码"
                class="login-captcha-row__img"
                @click="getCaptcha()"
                @error="onCaptchaError"
              >
            </div>
          </div>
        </el-form-item>
        <el-form-item class="login-form__submit">
          <el-button
            type="primary"
            class="login-submit-btn"
            :loading="submitLoading"
            @click="dataFormSubmit()"
          >登 录</el-button>
        </el-form-item>
      </el-form>
    </div>
    <p class="login-footer">自习室预约管理系统</p>
  </div>
</template>

<script>
import { getUUID } from "@/utils";
export default {
  data() {
    return {
      submitLoading: false,
      captchaLoadAttempts: 0,
      dataForm: {
        userName: "",
        password: "",
        uuid: "",
        captcha: "",
      },
      dataRule: {
        userName: [{ required: true, message: "请输入用户名", trigger: "blur" }],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
        captcha: [
          { required: true, message: "请输入验证码", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              const v = (value || "").trim();
              if (!v) {
                callback(new Error("请输入验证码"));
                return;
              }
              if (!this.dataForm.uuid) {
                callback(new Error("请先点击验证码图片获取图形"));
                return;
              }
              this.$http({
                url: this.$http.adornUrl("/captcha/check"),
                method: "get",
                params: this.$http.adornParams({
                  uuid: this.dataForm.uuid,
                  code: v,
                }),
              })
                .then(({ data }) => {
                  if (data && data.code === 0) {
                    callback();
                  } else {
                    callback(new Error((data && data.msg) || "验证码不正确"));
                  }
                })
                .catch(() => callback(new Error("校验失败，请稍后重试")));
            },
            trigger: "blur",
          },
        ],
      },
      captchaPath: "",
    };
  },
  created() {
    this.getCaptcha();
  },
  methods: {
    dataFormSubmit() {
      this.$refs.dataForm.validate((valid) => {
        if (!valid) return;
        this.submitLoading = true;
        this.$http({
          url: this.$http.adornUrl("/sys/login"),
          method: "post",
          data: this.$http.adornData({
            username: this.dataForm.userName,
            password: this.dataForm.password,
            uuid: this.dataForm.uuid,
            captcha: this.dataForm.captcha,
          }),
        })
          .then(({ data }) => {
            this.submitLoading = false;
            if (data && data.code === 0) {
              this.$cookie.set("token", data.token);
              this.$router.replace({ name: "home" });
            } else {
              this.getCaptcha();
              this.$message.error((data && data.msg) || "登录失败");
            }
          })
          .catch(() => {
            this.submitLoading = false;
            this.getCaptcha();
            this.$message.error("无法连接服务器，请确认后端已启动");
          });
      });
    },
    getCaptcha() {
      this.dataForm.uuid = getUUID();
      this.dataForm.captcha = "";
      const base = this.$http.adornUrl("/captcha.jpg");
      const sep = base.indexOf("?") >= 0 ? "&" : "?";
      this.captchaPath = `${base}${sep}uuid=${encodeURIComponent(this.dataForm.uuid)}&t=${Date.now()}`;
      this.captchaLoadAttempts = 0;
      this.$nextTick(() => {
        if (this.$refs.dataForm) {
          this.$refs.dataForm.clearValidate("captcha");
        }
      });
    },
    onCaptchaError() {
      this.captchaLoadAttempts += 1;
      if (this.captchaLoadAttempts <= 2) {
        window.setTimeout(() => {
          const base = this.$http.adornUrl("/captcha.jpg");
          const sep = base.indexOf("?") >= 0 ? "&" : "?";
          this.captchaPath = `${base}${sep}uuid=${encodeURIComponent(this.dataForm.uuid)}&t=${Date.now()}`;
        }, 350);
        return;
      }
      this.$message.warning("验证码加载失败：请确认后端已启动（端口与配置一致），或稍后点击图片刷新");
    },
  },
};
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 16px 48px;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: linear-gradient(135deg, #0f766e 0%, #115e59 38%, #134e4a 72%, #042f2e 100%);
}

.login-bg::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 20% 10%, rgba(255, 255, 255, 0.12), transparent 55%),
    radial-gradient(ellipse 60% 40% at 85% 75%, rgba(45, 212, 191, 0.15), transparent 50%);
  pointer-events: none;
}

.bg-orb {
  position: fixed;
  border-radius: 50%;
  z-index: 0;
  pointer-events: none;
  animation: orbFloat 12s ease-in-out infinite;
}

.bg-orb--1 {
  width: 400px;
  height: 400px;
  top: -120px;
  right: -80px;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.12), transparent 70%);
  animation-delay: 0s;
}

.bg-orb--2 {
  width: 350px;
  height: 350px;
  bottom: -100px;
  left: -60px;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.08), transparent 70%);
  animation-delay: -6s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 40px 40px 36px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.08),
    0 24px 48px -12px rgba(15, 118, 110, 0.35);
  backdrop-filter: blur(8px);
  animation: cardEnter 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(36px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-card__brand {
  text-align: center;
  margin-bottom: 32px;
}

.login-card__logo {
  width: 60px;
  height: 60px;
  margin: 0 auto 12px;
  border-radius: 16px;
  background: linear-gradient(145deg, #17b3a3, #0f766e);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: -0.04em;
  box-shadow: 0 8px 24px rgba(15, 118, 110, 0.35);
  transition: transform 0.3s ease;
}

.login-card__logo:hover {
  transform: scale(1.05) rotate(-3deg);
}

.login-card__title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #134e4a;
  letter-spacing: 0.02em;
}

.login-card__subtitle {
  margin: 8px 0 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-form {
  ::v-deep .el-input__inner {
    border-radius: 10px;
    height: 44px;
    transition: all 0.3s ease;
  }
  ::v-deep .el-input__inner:focus {
    border-color: #17b3a3;
    box-shadow: 0 0 0 3px rgba(23, 179, 163, 0.12);
  }
  ::v-deep .el-form-item {
    margin-bottom: 22px;
  }
  ::v-deep .el-form-item__label {
    padding: 0 0 6px;
    line-height: 1.4;
    font-size: 13px;
    font-weight: 500;
    color: #475569;
  }
}

.login-captcha-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
}

.login-captcha-row__input {
  flex: 1;
}

.login-captcha-row__img-wrap {
  flex-shrink: 0;
  width: 118px;
  height: 44px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.login-captcha-row__img-wrap:hover {
  border-color: #17b3a3;
  box-shadow: 0 0 0 2px rgba(23, 179, 163, 0.15);
}

.login-captcha-row__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-form__submit {
  margin-bottom: 0;
  margin-top: 12px;
}

.login-submit-btn {
  width: 100%;
  border-radius: 10px;
  padding: 12px 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.3em;
  background: linear-gradient(135deg, #17b3a3, #0f766e) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(15, 118, 110, 0.3);
  transition: all 0.35s ease;
}

.login-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(15, 118, 110, 0.4) !important;
  background: linear-gradient(135deg, #1ccaba, #17b3a3) !important;
}

.login-submit-btn:active {
  transform: translateY(0) !important;
}

.login-footer {
  position: relative;
  z-index: 1;
  margin-top: 20px;
  max-width: 420px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.5;
}


</style>

<style>
/* 全局修复 Element UI 2.8.2 status-icon 验证图标显示 */
/* Element UI 2.8.2 的 input.js 渲染图标在 el-input__suffix 内部：
   class: "el-input__icon el-input__validateIcon el-icon-circle-check" (成功)
   class: "el-input__icon el-input__validateIcon el-icon-circle-close" (失败)
   el-input__suffix 有 pointer-events:none，需要覆盖 */

/* 确保图标可见 */
.el-form-item--feedback .el-input__suffix .el-input__validateIcon,
.el-input .el-input__validateIcon {
  display: inline-block !important;
  visibility: visible !important;
  opacity: 1 !important;
  pointer-events: auto !important;
}

/* 成功：绿色圆圈勾 */
.el-form-item.is-success .el-input__suffix .el-icon-circle-check {
  color: #67c23a !important;
}

/* 错误：红色圆圈叉 */
.el-form-item.is-error .el-input__suffix .el-icon-circle-close {
  color: #f56c6c !important;
}
</style>
