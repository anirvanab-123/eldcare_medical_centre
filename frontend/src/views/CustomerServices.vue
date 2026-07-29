<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const customers = ref([])
const items = ref([])
const levels = ref([])
const rows = ref([])
const customerId = ref()
const levelId = ref()
const show = ref(false)
const itemKeyword = ref('')
const form = reactive({})

const selectedCustomer = computed(() => {
    return customers.value.find(x => x.id === customerId.value)
})

const customerCareLevelId = computed(() => {
    const c = selectedCustomer.value
    return c?.careLevelId || c?.care_level_id || null
})

const availableItems = computed(() => {
    return items.value.filter(item => {
        return item.name.includes(itemKeyword.value) &&
            !rows.value.some(service => Number(service.care_item_id) === Number(item.id))
    })
})

const serviceText = status => {
    return {
        NORMAL: '数量正常且未到期',
        EXPIRED: '服务已到期',
        DEPLETED: '次数已用完',
        UNPAID: '尚未缴费'
    }[status] || '状态未知'
}

async function init() {
    customers.value = (await api.get('/customers', {
        params: {
            size: 100,
            elderType: ''
        }
    })).records.filter(c => c.status !== 'CHECKED_OUT')
    ;[items.value, levels.value] = await Promise.all([
        api.get('/care/items', {
            params: { status: 1 }
        }),
        api.get('/care/levels', {
            params: { status: 1 }
        })
    ])
}

async function load() {
    if (!customerId.value) {
        rows.value = []
        return
    }
    rows.value = await api.get('/care/services', {
        params: { customerId: customerId.value }
    })
    levelId.value = customerCareLevelId.value || null
}

function open() {
    const today = new Date().toISOString().slice(0, 10)
    itemKeyword.value = ''
    Object.assign(form, {
        customerId: customerId.value,
        careItemId: null,
        quantity: 1,
        expiryDate: today
    })
    show.value = true
}

async function save() {
    try {
        await api.post('/care/services', form)
        show.value = false
        ElMessage.success('护理项目购买成功')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function renew(row) {
    const expiryDate = prompt('新的到期日期（YYYY-MM-DD）', row.expiry_date)
    const quantity = Number(prompt('新增次数', '1'))
    if (expiryDate && quantity > 0) {
        await api.put(`/care/services/${row.id}/renew`, {
            expiryDate,
            quantity
        })
        load()
    }
}

async function togglePayment(row) {
    await api.put(`/care/services/${row.id}/payment`, {
        paidStatus: Number(row.paid_status) === 1 ? 0 : 1
    })
    ElMessage.success(Number(row.paid_status) === 1 ? '已标记为欠费' : '已确认缴费')
    load()
}

async function removeService(id) {
    await ElMessageBox.confirm('移除后客户将不再享有该护理服务，确认吗？')
    await api.delete(`/care/services/${id}`)
    load()
}

async function setLevel() {
    try {
        await api.put(`/care/customers/${customerId.value}/level`, {
            levelId: levelId.value
        })
        selectedCustomer.value.careLevelId = levelId.value
        ElMessage.success('护理级别设置成功')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function removeLevel() {
    await ElMessageBox.confirm('移除级别会同时移除该级别生成的基础护理项目，确认吗？')
    await api.delete(`/care/customers/${customerId.value}/level`)
    selectedCustomer.value.careLevelId = null
    levelId.value = null
    load()
}

onMounted(init)
</script>

<template>
    <div class="page">
        <h2>客户护理设置 / 服务关注</h2>
        <el-alert
            title="护理级别决定基础护理套餐；套餐之外可以额外购买。已有项目不能重复购买，需要增加次数时请使用续费。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
        />
        <div class="toolbar">
            <el-select
                v-model="customerId"
                filterable
                clearable
                placeholder="按姓名选择在住客户"
                style="width: 230px"
                @change="load"
            >
                <el-option
                    v-for="customer in customers"
                    :key="customer.id"
                    :label="customer.name + '（' + customer.statusLabel + '）'"
                    :value="customer.id"
                />
            </el-select>
            <el-select
                v-model="levelId"
                :disabled="!customerId || !!customerCareLevelId"
                placeholder="选择启用的护理级别"
                style="width: 210px"
            >
                <el-option
                    v-for="level in levels"
                    :key="level.id"
                    :label="level.name"
                    :value="level.id"
                />
            </el-select>
            <el-button
                :disabled="!customerId || !levelId || !!customerCareLevelId"
                @click="setLevel"
            >
                设置护理级别
            </el-button>
            <el-button
                :disabled="!customerId || !customerCareLevelId"
                @click="removeLevel"
            >
                移除护理级别
            </el-button>
            <el-button
                type="primary"
                :disabled="!customerId"
                @click="open"
            >
                购买护理项目
            </el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column prop="item_code" label="编号" />
                <el-table-column prop="item_name" label="护理项目" />
                <el-table-column prop="source_label" label="服务来源" />
                <el-table-column prop="total_quantity" label="总次数" />
                <el-table-column prop="remaining_quantity" label="剩余次数" />
                <el-table-column prop="expiry_date" label="到期日期" />
                <el-table-column label="服务状态">
                    <template #default="scope">
                        {{ serviceText(scope.row.service_status) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="230">
                    <template #default="scope">
                        <el-button link @click="renew(scope.row)">续费</el-button>
                        <el-button link @click="togglePayment(scope.row)">
                            {{ Number(scope.row.paid_status) === 1 ? '标记欠费' : '确认缴费' }}
                        </el-button>
                        <el-button link type="danger" @click="removeService(scope.row.id)">移除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <el-dialog v-model="show" title="购买护理项目">
            <el-form label-width="100px">
                <el-form-item label="项目查询">
                    <el-input
                        v-model="itemKeyword"
                        clearable
                        placeholder="输入护理项目名称"
                    />
                </el-form-item>
                <el-form-item label="护理项目">
                    <el-select v-model="form.careItemId" style="width: 100%">
                        <el-option
                            v-for="item in availableItems"
                            :key="item.id"
                            :label="item.name"
                            :value="item.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="购买次数">
                    <el-input-number v-model="form.quantity" :min="1" />
                </el-form-item>
                <el-form-item label="到期日期">
                    <el-date-picker
                        v-model="form.expiryDate"
                        value-format="YYYY-MM-DD"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="show = false">取消</el-button>
                <el-button type="primary" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>
