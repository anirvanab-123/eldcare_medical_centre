<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'

const router = useRouter()
const store = useStore()
const user = computed(() => store.state.user || {})

async function logout() {
    await store.dispatch('logout')
    router.push('/login')
}
</script>

<template>
    <el-container class="shell">
        <el-aside width="238px">
            <div class="logo">
                <span class="logo-mark">颐</span>
                <div>
                    颐养中心
                    <small>智慧照护平台</small>
                </div>
            </div>
            <el-menu
                router
                default-active="/dashboard"
            >
                <el-menu-item index="/dashboard">
                    <span class="menu-icon">⌂</span>
                    工作台
                </el-menu-item>
                <template v-if="user.role === 'ADMIN'">
                    <el-sub-menu index="customer">
                        <template #title>
                            <span class="menu-icon">♙</span>
                            客户管理
                        </template>
                        <el-menu-item index="/customers">入住登记</el-menu-item>
                        <el-menu-item index="/outing">外出登记</el-menu-item>
                        <el-menu-item index="/checkout">退住登记</el-menu-item>
                    </el-sub-menu>
                    <el-sub-menu index="bed">
                        <template #title>
                            <span class="menu-icon">▰</span>
                            床位管理
                        </template>
                        <el-menu-item index="/beds">床位示意图</el-menu-item>
                        <el-menu-item index="/bed-usage">床位使用记录</el-menu-item>
                    </el-sub-menu>
                    <el-sub-menu index="care">
                        <template #title>
                            <span class="menu-icon">✚</span>
                            护理管理
                        </template>
                        <el-menu-item index="/care-items">护理项目</el-menu-item>
                        <el-menu-item index="/care-levels">护理级别</el-menu-item>
                        <el-menu-item index="/services">客户护理设置</el-menu-item>
                        <el-menu-item index="/records">护理记录</el-menu-item>
                    </el-sub-menu>
                    <el-menu-item index="/managers">
                        <span class="menu-icon">♧</span>
                        服务对象设置
                    </el-menu-item>
                    <el-menu-item index="/users">
                        <span class="menu-icon">⚙</span>
                        用户管理
                    </el-menu-item>
                </template>
                <template v-else>
                    <el-menu-item index="/my-customers">
                        <span class="menu-icon">♧</span>
                        我的服务对象
                    </el-menu-item>
                    <el-menu-item index="/records">
                        <span class="menu-icon">✚</span>
                        日常护理
                    </el-menu-item>
                    <el-menu-item index="/outing">
                        <span class="menu-icon">➜</span>
                        外出申请
                    </el-menu-item>
                    <el-menu-item index="/checkout">
                        <span class="menu-icon">↗</span>
                        退住申请
                    </el-menu-item>
                </template>
            </el-menu>
        </el-aside>
        <el-container>
            <el-header>
                <span class="header-title">606号楼 · 智慧照护管理平台</span>
                <span class="user-box">
                    <i>{{ (user.realName || '用户').slice(0, 1) }}</i>
                    {{ user.realName }}
                    <el-tag size="small">
                        {{ user.role === 'ADMIN' ? '管理员' : '健康管家' }}
                    </el-tag>
                    <el-button
                        size="small"
                        type="danger"
                        plain
                        @click="logout"
                    >
                        退出登录
                    </el-button>
                </span>
            </el-header>
            <el-main>
                <router-view />
            </el-main>
        </el-container>
    </el-container>
</template>

<style scoped>
.shell {
    min-height: 100vh;
}

.el-aside {
    background: linear-gradient(180deg, #123f39, #18544a 58%, #1d6355);
    box-shadow: 5px 0 20px rgba(24, 63, 57, .14);
}

.logo {
    height: 82px;
    display: flex;
    align-items: center;
    gap: 11px;
    padding-left: 21px;
    font-size: 21px;
    font-weight: 700;
    color: #fff;
}

.logo small {
    display: block;
    font-size: 11px;
    font-weight: 400;
    letter-spacing: 2px;
    color: #a9d6ca;
    margin-top: 3px;
}

.logo-mark {
    width: 39px;
    height: 39px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    background: linear-gradient(135deg, #39c98e, #4b8eed);
    box-shadow: 0 5px 15px #0b3029;
}

.el-menu {
    border: 0;
    background: transparent;
    --el-menu-text-color: #d9ebe6;
    --el-menu-hover-bg-color: rgba(255, 255, 255, .1);
    --el-menu-active-color: #fff;
}

:deep(.el-sub-menu .el-menu) {
    background: rgba(5, 43, 38, .38) !important;
}

:deep(.el-sub-menu .el-menu-item) {
    color: #c7e3db;
    background: transparent !important;
    padding-left: 54px !important;
    border-left: 3px solid transparent;
}

:deep(.el-sub-menu .el-menu-item:hover) {
    color: #fff;
    background: rgba(79, 193, 151, .16) !important;
}

:deep(.el-sub-menu .el-menu-item.is-active) {
    color: #fff;
    background: linear-gradient(90deg, rgba(55, 207, 151, .42), rgba(66, 141, 232, .32)) !important;
    border-left-color: #65e0ad;
    border-right: 4px solid #65e0ad;
}

.el-menu-item.is-active {
    background: linear-gradient(90deg, rgba(51, 205, 145, .38), rgba(63, 137, 230, .25));
    border-right: 4px solid #6ee4b1;
}

.menu-icon {
    display: inline-block;
    width: 27px;
    font-size: 18px;
    color: #7de4b8;
}

.el-header {
    height: 68px;
    background: rgba(255, 255, 255, .94);
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #e1e9ef;
    box-shadow: 0 3px 12px rgba(43, 71, 88, .05);
    padding: 0 27px;
}

.header-title {
    font-size: 16px;
    color: #547080;
    font-weight: 600;
}

.user-box {
    display: flex;
    align-items: center;
    gap: 9px;
    color: #425967;
}

.user-box i {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    background: linear-gradient(135deg, #22ad78, #4787e8);
    color: #fff;
    font-style: normal;
    font-weight: 700;
}

.el-main {
    padding: 0;
}
</style>
