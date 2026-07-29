<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({
    type: String
})

const rows = ref([])
const customers = ref([])
const show = ref(false)
const keyword = ref('')
const total = ref(0)
const pager = reactive({
    page: 1,
    size: 10
})
const form = reactive({})
const user = JSON.parse(sessionStorage.getItem('user') || '{}')

const isManager = computed(() => user.role === 'HEALTH_MANAGER')
const outing = computed(() => props.type === 'outing')
const title = computed(() => outing.value ? '外出登记' : '退住登记')

const approvalText = status => ({
    SUBMITTED: '待管理员审批',
    APPROVED: '审核通过',
    REJECTED: '审核未通过'
}[status] || '状态未知')

const checkoutText = type => ({
    NORMAL: '正常退住',
    DEATH: '死亡退住',
    KEEP_BED: '暂时离院（保留床位）'
}[type] || '类型未知')

async function load() {
    const result = await api.get(`/requests/${props.type}`, {
        params: {
            name: keyword.value,
            page: pager.page,
            size: pager.size
        }
    })
    rows.value = result.records || []
    total.value = result.total || 0
}

function search() {
    pager.page = 1
    load()
}

async function open() {
    customers.value = await api.get('/management/my-customers')
    Object.assign(form, {
        customerId: null,
        reason: '',
        outingTime: '',
        expectedReturnTime: '',
        checkoutType: 'NORMAL',
        checkoutDate: ''
    })
    show.value = true
}

async function save() {
    try {
        await api.post(`/requests/${props.type}`, form)
        show.value = false
        ElMessage.success('申请已提交，等待管理员审批')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function approve(id, result) {
    try {
        await api.put(`/requests/${props.type}/${id}/approve`, {
            result
        })
        ElMessage.success(result === 'APPROVED' ? '申请已通过' : '申请已驳回')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function registerReturn(id) {
    try {
        await api.put(`/requests/outing/${id}/return`)
        ElMessage.success('客户回院登记完成')
        load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

watch(() => props.type, () => {
    pager.page = 1
    load()
})

onMounted(load)
</script>

<template>
    <div class="page">
        <h2>{{ title }}</h2>
        <el-alert
            v-if="user.role === 'ADMIN'"
            title="管理员负责查询和审批；外出、退住申请由客户所属的健康管家发起。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
        />
        <el-alert
            v-else
            title="健康管家只能为自己负责的在住客户提交申请，提交后由管理员审核。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
        />
        <div class="toolbar">
            <el-input
                v-model="keyword"
                placeholder="客户姓名"
                style="width: 240px"
                @keyup.enter="search"
            />
            <el-button @click="search">
                查询
            </el-button>
            <el-button
                v-if="isManager"
                type="primary"
                @click="open"
            >
                {{ outing ? '新增外出申请' : '新增退住申请' }}
            </el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column
                    prop="customer_name"
                    label="客户"
                />
                <el-table-column
                    prop="applicant_name"
                    label="申请人（健康管家）"
                />
                <el-table-column
                    prop="reason"
                    label="事由"
                />
                <el-table-column
                    v-if="outing"
                    prop="outing_time"
                    label="外出时间"
                />
                <el-table-column
                    v-if="outing"
                    prop="expected_return_time"
                    label="预计回院"
                />
                <el-table-column
                    v-if="!outing"
                    label="退住类型"
                >
                    <template #default="scope">
                        {{ checkoutText(scope.row.checkout_type) }}
                    </template>
                </el-table-column>
                <el-table-column label="审批状态">
                    <template #default="scope">
                        {{ approvalText(scope.row.approval_status) }}
                    </template>
                </el-table-column>
                <el-table-column
                    label="操作"
                    width="230"
                >
                    <template #default="scope">
                        <template v-if="user.role === 'ADMIN' && scope.row.approval_status === 'SUBMITTED'">
                            <el-button
                                link
                                type="success"
                                @click="approve(scope.row.id, 'APPROVED')"
                            >
                                通过
                            </el-button>
                            <el-button
                                link
                                type="danger"
                                @click="approve(scope.row.id, 'REJECTED')"
                            >
                                驳回
                            </el-button>
                        </template>
                        <el-button
                            v-if="isManager && outing && scope.row.approval_status === 'APPROVED' && !scope.row.actual_return_time"
                            link
                            @click="registerReturn(scope.row.id)"
                        >
                            登记回院
                        </el-button>
                        <span v-if="user.role === 'ADMIN' && scope.row.approval_status !== 'SUBMITTED'">
                            已完成审批
                        </span>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination
                v-model:current-page="pager.page"
                v-model:page-size="pager.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next"
                @current-change="load"
                @size-change="search"
            />
        </div>
        <el-dialog
            v-model="show"
            :title="outing ? '新增外出申请' : '新增退住申请'"
            width="560px"
        >
            <el-form label-width="100px">
                <el-form-item label="客户">
                    <el-select
                        v-model="form.customerId"
                        style="width: 100%"
                    >
                        <el-option
                            v-for="customer in customers"
                            :key="customer.id"
                            :label="customer.name"
                            :value="customer.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="申请事由">
                    <el-input
                        v-model="form.reason"
                        type="textarea"
                    />
                </el-form-item>
                <template v-if="outing">
                    <el-form-item label="外出时间">
                        <el-date-picker
                            v-model="form.outingTime"
                            type="datetime"
                            value-format="YYYY-MM-DDTHH:mm:ss"
                        />
                    </el-form-item>
                    <el-form-item label="预计回院">
                        <el-date-picker
                            v-model="form.expectedReturnTime"
                            type="datetime"
                            value-format="YYYY-MM-DDTHH:mm:ss"
                        />
                    </el-form-item>
                </template>
                <template v-else>
                    <el-form-item label="退住类型">
                        <el-select v-model="form.checkoutType">
                            <el-option
                                label="正常退住"
                                value="NORMAL"
                            />
                            <el-option
                                label="死亡退住"
                                value="DEATH"
                            />
                            <el-option
                                label="暂时离院（保留床位）"
                                value="KEEP_BED"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="退住日期">
                        <el-date-picker
                            v-model="form.checkoutDate"
                            value-format="YYYY-MM-DD"
                        />
                    </el-form-item>
                </template>
            </el-form>
            <template #footer>
                <el-button @click="show = false">
                    取消
                </el-button>
                <el-button
                    type="primary"
                    @click="save"
                >
                    提交申请
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>
