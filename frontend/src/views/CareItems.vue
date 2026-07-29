<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const rows = ref([])
const show = ref(false)
const query = reactive({
    name: '',
    status: 1
})
const form = reactive({})

async function load() {
    rows.value = await api.get('/care/items', {
        params: {
            name: query.name,
            status: query.status === '' ? undefined : query.status
        }
    })
}

function open(row = {}) {
    Object.assign(form, {
        id: row.id,
        itemCode: row.item_code || '',
        name: row.name || '',
        price: row.price || 0,
        status: row.status ?? 1,
        executionCycle: row.execution_cycle || '每日',
        executionTimes: row.execution_times || 1,
        description: row.description || ''
    })
    show.value = true
}

async function save() {
    try {
        if (form.id) {
            await api.put(`/care/items/${form.id}`, form)
        } else {
            await api.post('/care/items', form)
        }
        show.value = false
        ElMessage.success('保存成功')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function remove(id) {
    await ElMessageBox.confirm('删除不会影响客户已经购买的历史服务，确认删除？')
    await api.delete(`/care/items/${id}`)
    load()
}

onMounted(load)
</script>

<template>
    <div class="page">
        <h2>护理项目</h2>
        <div class="toolbar">
            <el-input
                v-model="query.name"
                clearable
                placeholder="项目名称"
                style="width: 220px"
            />
            <el-select
                v-model="query.status"
                style="width: 140px"
            >
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
                <el-option label="全部" value="" />
            </el-select>
            <el-button @click="load">组合查询</el-button>
            <el-button type="primary" @click="open()">新增项目</el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column prop="item_code" label="编号" />
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="price" label="价格" />
                <el-table-column prop="execution_cycle" label="周期" />
                <el-table-column prop="execution_times" label="次数" />
                <el-table-column label="状态">
                    <template #default="scope">
                        {{ scope.row.status ? '启用' : '停用' }}
                    </template>
                </el-table-column>
                <el-table-column label="操作">
                    <template #default="scope">
                        <el-button link @click="open(scope.row)">编辑</el-button>
                        <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <el-dialog
            v-model="show"
            title="护理项目"
            width="560px"
        >
            <el-form label-width="90px">
                <el-form-item label="项目编号">
                    <el-input v-model="form.itemCode" />
                </el-form-item>
                <el-form-item label="项目名称">
                    <el-input v-model="form.name" />
                </el-form-item>
                <el-form-item label="价格">
                    <el-input-number v-model="form.price" :min="0" />
                </el-form-item>
                <el-form-item label="执行周期">
                    <el-input v-model="form.executionCycle" />
                </el-form-item>
                <el-form-item label="执行次数">
                    <el-input-number v-model="form.executionTimes" :min="1" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch
                        v-model="form.status"
                        :active-value="1"
                        :inactive-value="0"
                    />
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="form.description" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="show = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>
