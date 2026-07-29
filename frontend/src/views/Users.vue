<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const rows = ref([])
const show = ref(false)
const f = reactive({})
const confirmPwd = ref('')

async function load() {
    rows.value = await api.get('/management/users')
}

function open(x = {}) {
    Object.assign(f, {
        id: x.id,
        username: x.username || '',
        realName: x.real_name || '',
        phone: x.phone || '',
        role: x.role || 'HEALTH_MANAGER',
        status: x.status ?? 1,
        password: ''
    })
    confirmPwd.value = ''
    show.value = true
}

async function save() {
    if (!f.id) {
        if (!f.password || f.password.length < 5) {
            ElMessage.error('密码长度不能少于5位')
            return
        }
        if (f.password !== confirmPwd.value) {
            ElMessage.error('两次输入的密码不一致')
            return
        }
    }
    try {
        if (f.id) {
            await api.put(`/management/users/${f.id}`, f)
        } else {
            await api.post('/management/users', f)
        }
        show.value = false
        ElMessage.success('保存成功')
        load()
    } catch (e) {
        ElMessage.error(e.message)
    }
}

async function reset(id) {
    await ElMessageBox.confirm('密码将重置为手机号后六位，确认吗？')
    await api.put(`/management/users/${id}/reset-password`)
    ElMessage.success('密码已重置')
}

async function del(id) {
    await ElMessageBox.confirm('确认删除该员工账号吗？')
    try {
        await api.delete(`/management/users/${id}`)
        load()
    } catch (e) {
        ElMessage.error(e.message)
    }
}

onMounted(load)
</script>

<template>
    <div class="page">
        <h2>用户管理</h2>
        <div class="toolbar">
            <el-button
                type="primary"
                @click="open()"
            >
                新增员工
            </el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column
                    prop="username"
                    label="登录账号"
                />
                <el-table-column
                    prop="real_name"
                    label="姓名"
                />
                <el-table-column
                    prop="phone"
                    label="手机号"
                />
                <el-table-column
                    prop="role"
                    label="角色"
                >
                    <template #default="s">
                        {{ s.row.role === 'ADMIN' ? '管理员' : '健康管家' }}
                    </template>
                </el-table-column>
                <el-table-column
                    prop="status"
                    label="状态"
                >
                    <template #default="s">
                        {{ s.row.status ? '启用' : '停用' }}
                    </template>
                </el-table-column>
                <el-table-column
                    label="操作"
                    width="250"
                >
                    <template #default="s">
                        <el-button
                            link
                            @click="open(s.row)"
                        >
                            编辑
                        </el-button>
                        <el-button
                            link
                            @click="reset(s.row.id)"
                        >
                            重置密码
                        </el-button>
                        <el-button
                            link
                            type="danger"
                            @click="del(s.row.id)"
                        >
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <el-dialog
            v-model="show"
            title="员工信息"
        >
            <el-form label-width="100px">
                <el-form-item label="登录账号">
                    <el-input v-model="f.username" />
                </el-form-item>
                <el-form-item label="姓名">
                    <el-input v-model="f.realName" />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input v-model="f.phone" />
                </el-form-item>
                <el-form-item label="角色">
                    <el-select v-model="f.role">
                        <el-option
                            label="系统管理员"
                            value="ADMIN"
                        />
                        <el-option
                            label="健康管家"
                            value="HEALTH_MANAGER"
                        />
                    </el-select>
                </el-form-item>
                <template v-if="!f.id">
                    <el-form-item label="设置密码">
                        <el-input
                            v-model="f.password"
                            type="password"
                            show-password
                            placeholder="至少5位字符"
                        />
                    </el-form-item>
                    <el-form-item label="确认密码">
                        <el-input
                            v-model="confirmPwd"
                            type="password"
                            show-password
                            placeholder="再次输入密码"
                        />
                    </el-form-item>
                </template>
                <el-form-item label="状态">
                    <el-switch
                        v-model="f.status"
                        :active-value="1"
                        :inactive-value="0"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="show = false">
                    取消
                </el-button>
                <el-button
                    type="primary"
                    @click="save"
                >
                    保存
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>
