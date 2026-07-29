<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import api from '../api'

const r = useRouter()
const store = useStore()
const loading = ref(false)
const f = reactive({
    username: '',
    password: ''
})

async function login() {
    if (!f.username || !f.password) {
        ElMessage.warning('请输入登录账号和密码')
        return
    }
    try {
        loading.value = true
        const x = await api.post('/auth/login', f)
        await store.dispatch('saveSession', x)
        r.push('/dashboard')
    } catch (e) {
        ElMessage.error(e.message)
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <main class="login">
        <section>
            <div class="brand">NEU · CARE</div>
            <h1>东软颐养中心</h1>
            <p>让照护服务更清晰、更及时、更安心</p>
        </section>

        <el-card class="box">
            <h2>欢迎登录</h2>
            <p class="muted">使用管理员或健康管家账号进入系统</p>
            <el-form
                :model="f"
                autocomplete="off"
                @keyup.enter="login"
            >
                <el-form-item>
                    <el-input
                        v-model="f.username"
                        size="large"
                        name="eldercare_account"
                        autocomplete="off"
                        placeholder="请输入登录账号"
                    />
                </el-form-item>
                <el-form-item>
                    <el-input
                        v-model="f.password"
                        size="large"
                        name="eldercare_password"
                        autocomplete="new-password"
                        type="password"
                        show-password
                        placeholder="请输入登录密码"
                    />
                </el-form-item>
                <el-button
                    type="primary"
                    size="large"
                    :loading="loading"
                    style="width: 100%"
                    @click="login"
                >
                    登录系统
                </el-button>
            </el-form>
        </el-card>
    </main>
</template>

<style scoped>
.login {
    min-height: 100vh;
    display: grid;
    grid-template-columns: 1.2fr .8fr;
    align-items: center;
    padding: 8vw;
    background: linear-gradient(
            125deg,
            #0d493b,
            #2a8a68 56%,
            #edf7f2 56%
    );
    color: white;
}

.brand {
    letter-spacing: 5px;
    color: #b8e2d2;
}

.login h1 {
    font-size: 48px;
    margin: 18px 0 8px;
}

.box {
    width: 390px;
    justify-self: end;
    color: #25343d;
    border: 0;
    border-radius: 16px;
    padding: 14px;
}

.box h2 {
    margin-bottom: 4px;
}

@media (max-width: 800px) {
    .login {
        display: flex;
        justify-content: center;
        background: #edf7f2;
    }
    .login > section {
        display: none;
    }
    .box {
        width: min(100%, 390px);
    }
}
</style>
