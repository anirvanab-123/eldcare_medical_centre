<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const managers = ref([])
const assigned = ref([])
const unassigned = ref([])
const managerId = ref()
const managerName = ref('')
const customerName = ref('')

async function loadManagers() {
    managers.value = await api.get('/management/managers', {
        params: {
            name: managerName.value
        }
    })
    if (!managers.value.some(m => m.id === managerId.value)) {
        managerId.value = managers.value[0]?.id
    }
    if (managerId.value) {
        await refresh()
    } else {
        assigned.value = []
        unassigned.value = []
    }
}

async function refresh() {
    ;[assigned.value, unassigned.value] = await Promise.all([
        api.get(`/management/managers/${managerId.value}/customers`),
        api.get('/management/unassigned-customers', {
            params: {
                name: customerName.value
            }
        })
    ])
}

async function choose(id) {
    managerId.value = id
    await refresh()
}

async function add(customer) {
    await api.put(`/management/customers/${customer.id}/manager`, {
        managerId: managerId.value
    })
    ElMessage.success('已分配给当前健康管家')
    refresh()
}

async function remove(id) {
    await api.delete(`/management/customers/${id}/manager`)
    ElMessage.success('已取消服务关系')
    refresh()
}

onMounted(loadManagers)
</script>

<template>
    <div class="page">
        <h2>服务对象设置</h2>
        <el-alert
            title="当前服务对象是选中健康管家负责的在住客户；待分配客户是尚未配置健康管家的在住客户。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
        />
        <div class="toolbar">
            <el-input
                v-model="managerName"
                clearable
                placeholder="健康管家姓名"
                style="width: 220px"
                @keyup.enter="loadManagers"
            />
            <el-button @click="loadManagers">
                查询管家
            </el-button>
            <el-input
                v-model="customerName"
                clearable
                placeholder="待分配客户姓名"
                style="width: 220px"
                @keyup.enter="refresh"
            />
            <el-button
                :disabled="!managerId"
                @click="refresh"
            >
                查询客户
            </el-button>
        </div>
        <div
            v-if="!managers.length"
            class="card"
        >
            暂无符合条件的健康管家。
        </div>
        <div
            v-else
            class="columns"
        >
            <div class="card">
                <h3>选择健康管家</h3>
                <div
                    v-for="manager in managers"
                    :key="manager.id"
                    class="person"
                    :class="{ active: managerId === manager.id }"
                    @click="choose(manager.id)"
                >
                    <b>{{ manager.real_name }}</b>
                    <span>{{ manager.customer_count }} 位服务对象</span>
                </div>
            </div>
            <div class="card table-card">
                <h3>当前服务对象</h3>
                <el-table :data="assigned">
                    <el-table-column
                        prop="name"
                        label="客户"
                        min-width="90"
                    />
                    <el-table-column
                        prop="status_label"
                        label="状态"
                        min-width="90"
                    />
                    <el-table-column
                        prop="family_phone"
                        label="家属电话"
                        min-width="125"
                    />
                    <el-table-column
                        label="操作"
                        width="125"
                        align="center"
                    >
                        <template #default="scope">
                            <el-button
                                link
                                type="danger"
                                @click="remove(scope.row.id)"
                            >
                                取消分配
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
            <div class="card table-card">
                <h3>待分配客户</h3>
                <el-table :data="unassigned">
                    <el-table-column
                        prop="name"
                        label="客户"
                        min-width="100"
                    />
                    <el-table-column
                        prop="status_label"
                        label="状态"
                        min-width="100"
                    />
                    <el-table-column
                        label="操作"
                        width="145"
                        align="center"
                    >
                        <template #default="scope">
                            <el-button
                                link
                                :disabled="!managerId"
                                @click="add(scope.row)"
                            >
                                分配给当前管家
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </div>
    </div>
</template>

<style scoped>
.columns {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;
}

.columns > .card:first-child {
    grid-column: 1 / -1;
}

.table-card {
    min-width: 0;
    overflow: hidden;
}

.person {
    display: inline-flex;
    min-width: 190px;
    flex-direction: column;
    padding: 14px;
    margin: 0 10px 8px 0;
    border-radius: 8px;
    cursor: pointer;
    border: 1px solid #e0ebe6;
}

.person span {
    color: #84918c;
    margin-top: 5px;
}

.person.active {
    background: #e9f6f0;
    color: #177557;
    border-color: #8dd3b7;
}

@media (max-width: 950px) {
    .columns {
        grid-template-columns: 1fr;
    }

    .columns > .card:first-child {
        grid-column: auto;
    }

    .table-card {
        overflow: visible;
    }
}
</style>
