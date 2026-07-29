<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const rows = ref([])
const items = ref([])
const show = ref(false)
const status = ref(1)
const form = reactive({})

async function load() {
    [rows.value, items.value] = await Promise.all([
        api.get('/care/levels', {
            params: {
                status: status.value === '' ? undefined : status.value
            }
        }),
        api.get('/care/items', {
            params: { status: 1 }
        })
    ])
}

function open(row = {}) {
    Object.assign(form, {
        id: row.id,
        name: row.name || '',
        levelCode: row.level_code || '',
        dailyPrice: row.daily_price || '',
        status: row.status ?? 1,
        itemIds: row.item_ids ? String(row.item_ids).split(',').map(Number) : []
    })
    show.value = true
}

async function save() {
    try {
        if (form.id) {
            await api.put(`/care/levels/${form.id}`, form)
        } else {
            await api.post('/care/levels', form)
        }
        show.value = false
        ElMessage.success('保存成功')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

onMounted(load)
</script>

<template>
    <div class="page">
        <h2>护理级别</h2>
        <div class="toolbar">
            <el-select
                v-model="status"
                style="width: 150px"
                @change="load"
            >
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
                <el-option label="全部" value="" />
            </el-select>
            <el-button type="primary" @click="open()">新增级别</el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column prop="name" label="级别名称" />
                <el-table-column prop="level_code" label="级别编码" />
                <el-table-column prop="daily_price" label="日费用" />
                <el-table-column prop="item_names" label="包含护理项目" />
                <el-table-column label="状态">
                    <template #default="scope">
                        {{ scope.row.status ? '启用' : '停用' }}
                    </template>
                </el-table-column>
                <el-table-column label="操作">
                    <template #default="scope">
                        <el-button link @click="open(scope.row)">状态与项目配置</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <el-dialog v-model="show" title="护理级别配置">
            <el-form label-width="100px">
                <el-form-item label="级别名称">
                    <el-input v-model="form.name" :disabled="!!form.id" />
                    <span v-if="form.id" class="muted">已有级别名称不可修改</span>
                </el-form-item>
                <el-form-item label="级别编码">
                    <el-input v-model="form.levelCode" :disabled="!!form.id" />
                    <span v-if="form.id" class="muted">已有级别编码不可修改</span>
                </el-form-item>
                <el-form-item label="日费用">
                    <el-input
                        v-model="form.dailyPrice"
                        type="number"
                        placeholder="例如：150.00"
                    />
                    <span>元</span>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch
                        v-model="form.status"
                        :active-value="1"
                        :inactive-value="0"
                    />
                </el-form-item>
                <el-form-item label="护理项目">
                    <el-select
                        v-model="form.itemIds"
                        multiple
                        style="width: 100%"
                    >
                        <el-option
                            v-for="item in items"
                            :key="item.id"
                            :label="item.name"
                            :value="item.id"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="show = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>
