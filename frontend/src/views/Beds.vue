<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../api'

const props = defineProps({ mode: String })
const rows = ref([])
const building = ref('606')
const floor = ref(1)
const floors = ref([])
const name = ref('')
const usageState = ref('ACTIVE')
const startDate = ref('')
const total = ref(0)
const pager = reactive({
    page: 1,
    size: 10
})

const statusClass = status => ({
    FREE: 'free',
    OCCUPIED: 'occupied',
    OUTING: 'outing'
}[status] || 'unknown')

const statusText = (status, customerStatus) => {
    if (status === 'FREE') return '空闲'
    if (status === 'OUTING') return '已入住'
    if (status === 'OCCUPIED') return '已入住'
    return '状态未知'
}

const nameTag = (bed) => {
    if (!bed.customer_name) return '暂未入住'
    if (bed.customer_status === 'OUTING') return bed.customer_name + '（暂时离院）'
    return bed.customer_name
}

const roomTypeText = type => ({
    SINGLE: '单人间',
    DOUBLE: '双人间',
    MULTI: '多人间',
    APARTMENT: '套房'
}[type] || '标准间')

const roomTypeClass = type => ({
    SINGLE: 'type-single',
    DOUBLE: 'type-double',
    MULTI: 'type-multi',
    APARTMENT: 'type-apartment'
}[type] || 'type-double')

const floorFacilities = computed(() => {
    const facilities = {
        1: [
            { name: '棋牌室', icon: '🀄', desc: '麻将、棋牌' },
            { name: '茶艺室', icon: '🍵', desc: '品茶交流' },
            { name: '影音室', icon: '🎬', desc: '电影放映' }
        ],
        2: [
            { name: '舞蹈室', icon: '💃', desc: '广场舞、交谊舞' },
            { name: '音乐室', icon: '🎹', desc: '钢琴、合唱' },
            { name: '书画室', icon: '🎨', desc: '书法、绘画' }
        ],
        3: [
            { name: '健身房', icon: '🏋️', desc: '康复器械' },
            { name: '乒乓球室', icon: '🏓', desc: '乒乓球运动' },
            { name: '阅览室', icon: '📚', desc: '图书阅览' }
        ],
        4: [
            { name: '阳光露台', icon: '🌞', desc: '休闲观景' },
            { name: '棋牌室', icon: '🀄', desc: '麻将、棋牌' },
            { name: '多功能厅', icon: '🎭', desc: '演出活动' }
        ]
    }
    return facilities[floor.value] || []
})

const roomGroups = computed(() => {
    const groups = new Map()
    rows.value.forEach(bed => {
        if (!groups.has(bed.room_id)) {
            groups.set(bed.room_id, {
                id: bed.room_id,
                roomNo: bed.room_no,
                roomType: bed.room_type,
                area: bed.area,
                capacity: bed.capacity,
                beds: []
            })
        }
        groups.get(bed.room_id).beds.push(bed)
    })
    return [...groups.values()]
})

const floorStats = computed(() => ({
    total: rows.value.length,
    free: rows.value.filter(b => b.status === 'FREE').length,
    occupied: rows.value.filter(b => b.status === 'OCCUPIED').length,
    outing: rows.value.filter(b => b.status === 'OUTING').length
}))

async function load() {
    if (props.mode === 'overview') {
        rows.value = await api.get('/beds/overview', {
            params: {
                building: building.value,
                floor: floor.value
            }
        })
    } else {
        const result = await api.get('/beds/usage', {
            params: {
                name: name.value,
                state: usageState.value,
                startDate: startDate.value || undefined,
                page: pager.page,
                size: pager.size
            }
        })
        rows.value = result.records || []
        total.value = result.total || 0
    }
}

async function loadFloors() {
    floors.value = await api.get('/beds/floors', {
        params: {
            building: building.value
        }
    })
}

function changeBuilding() {
    floor.value = 1
    loadFloors()
    load()
}

function changeFloor() {
    load()
}

function search() {
    pager.page = 1
    load()
}

async function editEnd(row) {
    const value = prompt('请输入床位使用结束日期（YYYY-MM-DD）', row.end_date || '')
    if (value) {
        await api.put(`/beds/usage/${row.id}/end-date`, {
            endDate: value
        })
        load()
    }
}

watch(() => props.mode, () => {
    pager.page = 1
    load()
})

onMounted(async () => {
    await loadFloors()
    await load()
})
</script>

<template>
    <div class="page">
        <h2>{{ mode === 'overview' ? '床位示意图' : '床位使用记录' }}</h2>
        <div class="toolbar">
            <template v-if="mode === 'overview'">
                <el-select
                    v-model="building"
                    style="width:150px"
                    @change="changeBuilding"
                >
                    <el-option
                        label="606号楼"
                        value="606"
                    />
                </el-select>
                <el-select
                    v-model="floor"
                    style="width:130px"
                    @change="changeFloor"
                >
                    <el-option
                        v-for="f in floors"
                        :key="f.value"
                        :label="f.label"
                        :value="f.value"
                    />
                </el-select>
                <div class="stat">
                    <i class="bed-icon total-icon">▰</i>
                    总床位：{{ floorStats.total }}
                </div>
                <div class="stat">
                    <i class="bed-icon free-icon">▰</i>
                    空闲：{{ floorStats.free }}
                </div>
                <div class="stat">
                    <i class="bed-icon occupied-icon">▰</i>
                    有人：{{ floorStats.occupied }}
                </div>
                <div class="stat">
                    <i class="bed-icon outing-icon">▰</i>
                    外出：{{ floorStats.outing }}
                </div>
            </template>
            <template v-else>
                <el-input
                    v-model="name"
                    placeholder="客户姓名"
                    style="width:240px"
                    @keyup.enter="search"
                />
                <el-date-picker
                    v-model="startDate"
                    value-format="YYYY-MM-DD"
                    placeholder="入住日期"
                    style="width:160px"
                />
                <el-select
                    v-model="usageState"
                    style="width:150px"
                >
                    <el-option
                        label="正在使用"
                        value="ACTIVE"
                    />
                    <el-option
                        label="使用历史"
                        value="HISTORY"
                    />
                    <el-option
                        label="全部记录"
                        value="ALL"
                    />
                </el-select>
                <el-button @click="search">查询</el-button>
            </template>
        </div>

        <div
            v-if="mode === 'overview'"
            class="floor-card"
        >
            <div class="floor-title">{{ building }}号楼 · {{ floor }}楼床位平面示意图</div>
            <div class="floor-plan">
                <div class="public-area elevator">
                    <div class="area-icon">⇅</div>
                    <div class="area-name">电梯厅</div>
                </div>
                <div class="public-area laundry">
                    <div class="area-icon">🧺</div>
                    <div class="area-name">洗衣房</div>
                    <div class="area-desc">洗护服务</div>
                </div>
                <div class="public-area activity">
                    <div class="area-icon">🎯</div>
                    <div class="area-name">活动中心</div>
                    <div class="facilities">
                        <div
                            v-for="facility in floorFacilities"
                            :key="facility.name"
                            class="facility-item"
                        >
                            <span class="facility-icon">{{ facility.icon }}</span>
                            <span class="facility-name">{{ facility.name }}</span>
                            <span class="facility-desc">{{ facility.desc }}</span>
                        </div>
                    </div>
                </div>
                <div
                    v-for="room in roomGroups"
                    :key="room.id"
                    class="room-cell"
                    :class="roomTypeClass(room.roomType)"
                >
                    <div class="room-header">
                        <span class="room-number">{{ room.roomNo }}房间</span>
                        <span class="room-type-tag">{{ roomTypeText(room.roomType) }}</span>
                    </div>
                    <div class="room-info">
                        <span class="room-area">{{ room.area }}㎡</span>
                        <span class="room-capacity">共{{ room.capacity }}床</span>
                    </div>
                    <div class="room-beds">
                        <div
                            v-for="bed in room.beds"
                            :key="bed.bed_id"
                            class="mini-bed"
                            :class="statusClass(bed.status)"
                        >
                            <span class="bed-shape">▰</span>
                            <div>
                                <b>{{ bed.bed_no }}床 · {{ statusText(bed.status, bed.customer_status) }}</b>
                                <small>{{ nameTag(bed) }}</small>
                            </div>
                        </div>
                    </div>
                </div>
                <div
                    v-if="!roomGroups.length"
                    class="empty-floor"
                >
                    该楼层暂无房间
                </div>
            </div>
            <div class="legend">
                <div class="legend-item">
                    <span class="legend-color free-legend"></span>
                    空闲
                </div>
                <div class="legend-item">
                    <span class="legend-color occupied-legend"></span>
                    已入住
                </div>
                <div class="legend-item">
                    <span class="legend-color outing-legend"></span>
                    暂时离院
                </div>
                <div class="legend-item">
                    <span class="legend-color type-single"></span>
                    单人间
                </div>
                <div class="legend-item">
                    <span class="legend-color type-double"></span>
                    双人间
                </div>
                <div class="legend-item">
                    <span class="legend-color type-multi"></span>
                    多人间
                </div>
                <div class="legend-item">
                    <span class="legend-color type-apartment"></span>
                    套房
                </div>
            </div>
        </div>

        <div
            v-else
            class="card"
        >
            <el-table :data="rows">
                <el-table-column
                    prop="customer_name"
                    label="客户"
                />
                <el-table-column
                    prop="building_no"
                    label="楼栋"
                />
                <el-table-column
                    prop="room_no"
                    label="房间"
                />
                <el-table-column
                    prop="bed_no"
                    label="床位"
                />
                <el-table-column
                    prop="start_date"
                    label="开始日期"
                />
                <el-table-column
                    prop="end_date"
                    label="预计/实际结束日期"
                >
                    <template #default="scope">
                        {{ scope.row.end_date || '暂未设置' }}
                    </template>
                </el-table-column>
                <el-table-column label="使用状态">
                    <template #default="scope">
                        {{ scope.row.active ? '正在使用' : '使用历史' }}
                    </template>
                </el-table-column>
                <el-table-column
                    label="操作"
                    width="175"
                    align="center"
                >
                    <template #default="scope">
                        <el-button
                            link
                            @click="editEnd(scope.row)"
                        >
                            修改结束日期
                        </el-button>
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
    </div>
</template>

<style scoped>
.toolbar {
    align-items: center;
}

.stat {
    font-size: 15px;
    color: #4b5652;
    white-space: nowrap;
}

.bed-icon {
    font-style: normal;
    font-size: 27px;
    vertical-align: -3px;
    margin-right: 6px;
}

.total-icon {
    color: #33bf7a;
}

.free-icon {
    color: #ef5959;
}

.occupied-icon {
    color: #455d70;
}

.outing-icon {
    color: #1ea9df;
}

.floor-card {
    background: #fff;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 8px 25px rgba(33, 67, 55, .08);
}

.floor-title {
    text-align: center;
    font-size: 24px;
    font-weight: 700;
    margin-bottom: 20px;
    color: #263d35;
}

.floor-plan {
    display: grid;
    grid-template-columns: repeat(4, minmax(200px, 1fr));
    grid-auto-rows: minmax(200px, auto);
    border-left: 2px solid #7a8a85;
    border-top: 2px solid #7a8a85;
    background: #f7faf9;
    gap: 0;
}

.public-area,
.room-cell,
.empty-floor {
    border-right: 2px solid #7a8a85;
    border-bottom: 2px solid #7a8a85;
}

.public-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    padding: 15px;
}

.public-area .area-icon {
    font-size: 36px;
    margin-bottom: 8px;
}

.public-area .area-name {
    font-size: 18px;
    font-weight: 700;
}

.public-area .area-desc {
    font-size: 12px;
    opacity: 0.9;
    margin-top: 4px;
}

.elevator {
    background: #18b9d8;
}

.laundry {
    background: #399b96;
}

.activity {
    background: linear-gradient(135deg, #25dd8b 0%, #1db954 100%);
    grid-column: span 2;
}

.activity .area-icon {
    font-size: 40px;
}

.activity .area-name {
    font-size: 22px;
}

.activity .facilities {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 12px;
    justify-content: center;
    width: 100%;
}

.facility-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    background: rgba(255, 255, 255, 0.25);
    padding: 10px 15px;
    border-radius: 10px;
    min-width: 100px;
}

.facility-icon {
    font-size: 26px;
}

.facility-name {
    font-size: 14px;
    font-weight: 600;
    margin-top: 6px;
}

.facility-desc {
    font-size: 11px;
    opacity: 0.9;
    margin-top: 3px;
}

.room-cell {
    background: #fff;
    min-height: 220px;
    display: flex;
    flex-direction: column;
}

.room-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 15px;
    border-bottom: 2px solid #d0d9d5;
    background: #f4f7f6;
}

.room-number {
    font-size: 18px;
    font-weight: 700;
    color: #263d35;
}

.room-type-tag {
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 15px;
    font-weight: 600;
}

.type-single .room-type-tag {
    background: #ffe4e4;
    color: #d63031;
}

.type-double .room-type-tag {
    background: #e8f5e9;
    color: #2e7d32;
}

.type-multi .room-type-tag {
    background: #fff3e0;
    color: #ef6c00;
}

.type-apartment .room-type-tag {
    background: #e3f2fd;
    color: #1565c0;
}

.room-info {
    display: flex;
    gap: 15px;
    padding: 8px 15px;
    background: #fafbfb;
    border-bottom: 1px solid #e8ecea;
}

.room-area,
.room-capacity {
    font-size: 13px;
    color: #68736f;
}

.room-area::before {
    content: '🏠 ';
}

.room-capacity::before {
    content: '🛏️ ';
}

.room-beds {
    padding: 15px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex: 1;
}

.mini-bed {
    display: flex;
    align-items: center;
    gap: 12px;
    border-left: 5px solid #aaa;
    padding: 10px 12px;
    border-radius: 6px;
    background: #f6f7f7;
}

.mini-bed.free {
    border-color: #ef5959;
    background: #fff3f3;
}

.mini-bed.occupied {
    border-color: #455d70;
    background: #edf1f3;
}

.mini-bed.outing {
    border-color: #1ea9df;
    background: #edf9fd;
}

.bed-shape {
    font-size: 30px;
}

.mini-bed.free .bed-shape {
    color: #ef5959;
}

.mini-bed.occupied .bed-shape {
    color: #455d70;
}

.mini-bed.outing .bed-shape {
    color: #1ea9df;
}

.mini-bed div {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.mini-bed b {
    font-size: 15px;
}

.mini-bed small {
    margin-top: 4px;
    color: #68736f;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
}

.mini-bed.outing small {
    color: #1ea9df;
    font-weight: 600;
}

.empty-floor {
    grid-column: 1 / -1;
    padding: 80px;
    text-align: center;
    color: #89938f;
    font-size: 18px;
}

.legend {
    display: flex;
    justify-content: center;
    gap: 30px;
    margin-top: 20px;
    padding-top: 15px;
    border-top: 1px dashed #d0d9d5;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #5a6662;
}

.legend-color {
    width: 20px;
    height: 12px;
    border-radius: 3px;
}

.legend-color.free-legend {
    background: #fff3f3;
    border: 1px solid #ef5959;
}

.legend-color.occupied-legend {
    background: #edf1f3;
    border: 1px solid #455d70;
}

.legend-color.outing-legend {
    background: #edf9fd;
    border: 1px solid #1ea9df;
}

.legend-color.type-single {
    background: #ffe4e4;
    border: 1px solid #d63031;
}

.legend-color.type-double {
    background: #e8f5e9;
    border: 1px solid #2e7d32;
}

.legend-color.type-multi {
    background: #fff3e0;
    border: 1px solid #ef6c00;
}

.legend-color.type-apartment {
    background: #e3f2fd;
    border: 1px solid #1565c0;
}

@media (max-width: 1100px) {
    .floor-plan {
        grid-template-columns: repeat(2, minmax(180px, 1fr));
    }

    .activity {
        grid-column: span 2;
    }
}

@media (max-width: 700px) {
    .floor-plan {
        grid-template-columns: 1fr;
    }

    .activity {
        grid-column: span 1;
    }

    .facility-item {
        min-width: 80px;
    }
}
</style>
