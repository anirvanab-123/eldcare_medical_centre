<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const user = JSON.parse(sessionStorage.getItem('user') || '{}')
const customers = ref([])
const services = ref([])
const rows = ref([])
const customerId = ref()
const show = ref(false)
const form = reactive({})

const serviceText = status => {
    return {
        NORMAL: '可执行',
        EXPIRED: '已到期',
        DEPLETED: '次数用完',
        UNPAID: '未缴费'
    }[status] || '不可执行'
}

async function init() {
    if (user.role === 'ADMIN') {
        customers.value = (await api.get('/customers', {
            params: {
                size: 100,
                elderType: ''
            }
        })).records.filter(c => c.status !== 'CHECKED_OUT')
    } else {
        customers.value = await api.get('/management/my-customers')
    }
    await load()
}

async function load() {
    rows.value = await api.get('/care/records', {
        params: customerId.value ? { customerId: customerId.value } : {}
    })
}

async function select() {
    services.value = customerId.value
        ? await api.get('/care/services', {
            params: { customerId: customerId.value }
        })
        : []
    load()
}

function open() {
    Object.assign(form, {
        customerServiceId: null,
        quantity: 1,
        nursingTime: new Date().toISOString().slice(0, 19),
        remark: ''
    })
    show.value = true
}

async function save() {
    try {
        await api.post('/care/records', form)
        show.value = false
        ElMessage.success('护理记录已生成')
        select()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function remove(id) {
    await ElMessageBox.confirm('确认隐藏这条护理记录吗？')
    await api.delete(`/care/records/${id}`)
    load()
}

onMounted(init)
</script>

<template>
    <div class="page">
        <h2>{{ user.role === 'ADMIN' ? '护理记录' : '日常护理' }}</h2>
        <div class="toolbar">
            <el-select
                v-model="customerId"
                filterable
                clearable
                placeholder="按姓名选择客户"
                style="width: 250px"
                @change="select"
            >
                <el-option
                    v-for="customer in customers"
                    :key="customer.id"
                    :label="customer.name"
                    :value="customer.id"
                />
            </el-select>
            <el-button
                v-if="user.role === 'HEALTH_MANAGER'"
                type="primary"
                :disabled="!customerId"
                @click="open"
            >
                执行护理
            </el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column prop="customer_name" label="客户" />
                <el-table-column prop="item_name" label="护理项目" />
                <el-table-column prop="manager_name" label="健康管家" />
                <el-table-column prop="nursing_time" label="护理时间" />
                <el-table-column prop="quantity" label="护理次数" />
                <el-table-column prop="remark" label="备注" />
                <el-table-column label="操作">
                    <template #default="scope">
                        <el-button link type="danger" @click="remove(scope.row.id)">移除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <el-dialog v-model="show" title="执行日常护理">
            <el-form label-width="100px">
                <el-form-item label="护理服务">
                    <el-select v-model="form.customerServiceId" style="width: 100%">
                        <el-option
                            v-for="service in services"
                            :key="service.id"
                            :label="service.item_name + '（' + serviceText(service.service_status) + '，剩余' + service.remaining_quantity + '次）'"
                            :value="service.id"
                            :disabled="service.service_status !== 'NORMAL'"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="护理时间">
                    <el-date-picker
                        v-model="form.nursingTime"
                        type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                    />
                </el-form-item>
                <el-form-item label="护理次数">
                    <el-input-number v-model="form.quantity" :min="1" />
                </el-form-item>
                <el-form-item label="护理备注">
                    <el-input v-model="form.remark" type="textarea" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="show = false">取消</el-button>
                <el-button type="primary" @click="save">确认完成</el-button>
            </template>
        </el-dialog>
    </div>
</template>
