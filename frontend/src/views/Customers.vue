<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const rows = ref([])
const total = ref(0)
const rooms = ref([])
const beds = ref([])
const show = ref(false)
const bedShow = ref(false)
const query = reactive({
    page: 1,
    size: 10,
    name: '',
    elderType: 'SELF_CARE'
})
const form = reactive({})
const change = reactive({
    customerId: null,
    roomId: null,
    bedId: null
})

const roomTypeLabel = type => ({
    SINGLE: '单人间',
    DOUBLE: '双人间',
    MULTI: '多人间',
    APARTMENT: '套房'
}[type] || '标准间')

const roomGroups = computed(() => {
    const floors = [...new Set(rooms.value.map(r => r.floor_no))].sort()
    return floors.map(floor => ({
        floor,
        rooms: rooms.value.filter(room => room.floor_no === floor)
    })).filter(group => group.rooms.length)
})

async function load() {
    const result = await api.get('/customers', {
        params: query
    })
    rows.value = result.records
    total.value = result.total
}

async function search() {
    query.page = 1
    await load()
}

async function init() {
    rooms.value = await api.get('/beds/rooms')
    await load()
}

async function loadBeds(roomId, includeBedId) {
    beds.value = await api.get('/beds', {
        params: {
            roomId,
            status: 'FREE'
        }
    })
    if (includeBedId) {
        const all = await api.get('/beds', {
            params: {
                roomId
            }
        })
        const current = all.find(item => item.id === includeBedId)
        if (current && !beds.value.some(item => item.id === current.id)) {
            beds.value.push(current)
        }
    }
}

async function open(customer) {
    Object.assign(
        form,
        customer
            ? { ...customer }
            : {
                name: '',
                gender: '男',
                birthDate: '',
                idCard: '',
                bloodType: 'A',
                familyName: '',
                familyPhone: '',
                buildingNo: '606',
                roomId: null,
                bedId: null,
                checkInDate: new Date().toISOString().slice(0, 10),
                contractEndDate: ''
            }
    )
    beds.value = []
    if (form.roomId) {
        await loadBeds(form.roomId, form.bedId)
    }
    show.value = true
}

async function roomChanged() {
    form.bedId = null
    await loadBeds(form.roomId)
}

async function save() {
    try {
        form.id
            ? await api.put(`/customers/${form.id}`, form)
            : await api.post('/customers', form)
        show.value = false
        ElMessage.success(form.id ? '客户资料修改成功' : '入住登记成功')
        await load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

async function remove(customer) {
    await ElMessageBox.confirm(
        `确认删除客户"${customer.name}"并释放床位吗？`,
        '谨慎操作',
        { type: 'warning' }
    )
    await api.delete(`/customers/${customer.id}`)
    ElMessage.success('已删除')
    await load()
}

function openChange(customer) {
    Object.assign(change, {
        customerId: customer.id,
        roomId: null,
        bedId: null
    })
    beds.value = []
    bedShow.value = true
}

async function changeRoom() {
    change.bedId = null
    await loadBeds(change.roomId)
}

async function changeBed() {
    try {
        await api.post(`/customers/${change.customerId}/change-bed`, {
            bedId: change.bedId
        })
        bedShow.value = false
        ElMessage.success('床位调换成功')
        await load()
    } catch (error) {
        ElMessage.error(error.message)
    }
}

onMounted(init)
</script>

<template>
    <div class="page">
        <h2>入住登记</h2>
        <div class="toolbar">
            <el-input
                v-model="query.name"
                clearable
                placeholder="客户姓名（支持模糊查询）"
                style="width:260px"
                @keyup.enter="search"
            />
            <el-select
                v-model="query.elderType"
                clearable
                placeholder="老人类型"
                style="width:170px"
            >
                <el-option
                    label="自理老人"
                    value="SELF_CARE"
                />
                <el-option
                    label="护理老人"
                    value="NURSING"
                />
            </el-select>
            <el-button @click="search">组合查询</el-button>
            <el-button
                type="primary"
                @click="open()"
            >
                新增入住登记
            </el-button>
        </div>
        <div class="card">
            <el-table :data="rows">
                <el-table-column
                    prop="name"
                    label="客户"
                    min-width="90"
                />
                <el-table-column
                    prop="gender"
                    label="性别"
                    width="60"
                    align="center"
                />
                <el-table-column
                    prop="elderTypeLabel"
                    label="老人类型"
                    width="100"
                />
                <el-table-column
                    prop="birthDate"
                    label="出生日期"
                    width="112"
                />
                <el-table-column
                    prop="age"
                    label="年龄"
                    width="62"
                    align="center"
                />
                <el-table-column
                    prop="familyName"
                    label="家属"
                    min-width="85"
                />
                <el-table-column
                    prop="familyPhone"
                    label="联系电话"
                    width="125"
                />
                <el-table-column
                    prop="checkInDate"
                    label="入住日期"
                    width="112"
                />
                <el-table-column
                    prop="contractEndDate"
                    label="合同到期"
                    width="112"
                />
                <el-table-column
                    prop="statusLabel"
                    label="客户状态"
                    width="120"
                />
                <el-table-column
                    label="操作"
                    width="205"
                    align="center"
                    fixed="right"
                >
                    <template #default="scope">
                        <el-button
                            link
                            @click="open(scope.row)"
                        >
                            编辑
                        </el-button>
                        <el-button
                            v-if="scope.row.status !== 'CHECKED_OUT'"
                            link
                            @click="openChange(scope.row)"
                        >
                            调床
                        </el-button>
                        <el-button
                            link
                            type="danger"
                            @click="remove(scope.row)"
                        >
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination
                v-model:current-page="query.page"
                :page-size="query.size"
                :total="total"
                layout="total, prev, pager, next"
                @current-change="load"
            />
        </div>

        <el-dialog
            v-model="show"
            :title="form.id ? '修改客户资料' : '客户入住登记'"
            width="680px"
        >
            <el-form
                :model="form"
                label-width="100px"
            >
                <el-row :gutter="15">
                    <el-col :span="12">
                        <el-form-item label="客户姓名">
                            <el-input v-model="form.name" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="性别">
                            <el-select v-model="form.gender">
                                <el-option
                                    label="男"
                                    value="男"
                                />
                                <el-option
                                    label="女"
                                    value="女"
                                />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="出生日期">
                            <el-date-picker
                                v-model="form.birthDate"
                                value-format="YYYY-MM-DD"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col
                        :span="12"
                        v-if="form.birthDate"
                    >
                        <el-form-item label="自动计算年龄">
                            <el-input
                                :model-value="Math.max(0, new Date().getFullYear() - Number(form.birthDate.slice(0, 4))) + '岁'"
                                disabled
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="身份证号">
                            <el-input v-model="form.idCard" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="血型">
                            <el-select v-model="form.bloodType">
                                <el-option
                                    v-for="item in ['A', 'B', 'AB', 'O', '未知']"
                                    :key="item"
                                    :label="item"
                                    :value="item"
                                />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="家属姓名">
                            <el-input v-model="form.familyName" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="联系电话">
                            <el-input v-model="form.familyPhone" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="入住日期">
                            <el-date-picker
                                v-model="form.checkInDate"
                                value-format="YYYY-MM-DD"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同到期">
                            <el-date-picker
                                v-model="form.contractEndDate"
                                value-format="YYYY-MM-DD"
                            />
                        </el-form-item>
                    </el-col>
                    <template v-if="!form.id">
                        <el-col :span="12">
                            <el-form-item label="房间">
                                <el-select
                                    v-model="form.roomId"
                                    style="width:100%"
                                    @change="roomChanged"
                                >
                                    <el-option-group
                                        v-for="group in roomGroups"
                                        :key="group.floor"
                                        :label="group.floor + '楼'"
                                    >
                                        <el-option
                                            v-for="room in group.rooms"
                                            :key="room.id"
                                            :label="room.room_no + '房间 · ' + roomTypeLabel(room.room_type) + ' · ' + room.area + '㎡'"
                                            :value="room.id"
                                        />
                                    </el-option-group>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="床位">
                                <el-select
                                    v-model="form.bedId"
                                    style="width:100%"
                                >
                                    <el-option
                                        v-for="bed in beds"
                                        :key="bed.id"
                                        :label="bed.bedNo + '床'"
                                        :value="bed.id"
                                    />
                                </el-select>
                            </el-form-item>
                        </el-col>
                    </template>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="show = false">取消</el-button>
                <el-button
                    type="primary"
                    @click="save"
                >
                    保存
                </el-button>
            </template>
        </el-dialog>

        <el-dialog
            v-model="bedShow"
            title="床位调换"
            width="480px"
        >
            <el-form label-width="90px">
                <el-form-item label="目标房间">
                    <el-select
                        v-model="change.roomId"
                        style="width:100%"
                        @change="changeRoom"
                    >
                        <el-option-group
                            v-for="group in roomGroups"
                            :key="group.floor"
                            :label="group.floor + '楼'"
                        >
                            <el-option
                                v-for="room in group.rooms"
                                :key="room.id"
                                :label="room.room_no + '房间 · ' + roomTypeLabel(room.room_type) + ' · ' + room.area + '㎡'"
                                :value="room.id"
                            />
                        </el-option-group>
                    </el-select>
                </el-form-item>
                <el-form-item label="目标床位">
                    <el-select
                        v-model="change.bedId"
                        style="width:100%"
                    >
                        <el-option
                            v-for="bed in beds"
                            :key="bed.id"
                            :label="bed.bedNo + '床'"
                            :value="bed.id"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="bedShow = false">取消</el-button>
                <el-button
                    type="primary"
                    @click="changeBed"
                >
                    确认调床
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>
