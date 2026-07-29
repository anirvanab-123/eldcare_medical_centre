<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api'

const rows = ref([])
const query = reactive({
    name: '',
    elderType: ''
})

async function load() {
    rows.value = await api.get('/management/my-customers', {
        params: query
    })
}

onMounted(load)
</script>

<template>
    <div class="page">
        <h2>我的服务对象</h2>
        <div class="toolbar">
            <el-input
                v-model="query.name"
                clearable
                placeholder="客户姓名（支持模糊查询）"
                style="width: 260px"
                @keyup.enter="load"
            />
            <el-select
                v-model="query.elderType"
                clearable
                placeholder="老人类型"
                style="width: 180px"
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
            <el-button @click="load">
                组合查询
            </el-button>
        </div>
        <el-alert
            title="自理老人：未配置护理级别及护理项目；护理老人：已配置护理级别或护理项目。"
            type="info"
            :closable="false"
            style="margin-bottom: 16px"
        />
        <div class="card">
            <el-table :data="rows">
                <el-table-column
                    prop="name"
                    label="客户"
                />
                <el-table-column
                    prop="gender"
                    label="性别"
                />
                <el-table-column
                    prop="elder_type_label"
                    label="老人类型"
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
                    prop="care_level_name"
                    label="护理级别"
                >
                    <template #default="scope">
                        {{ scope.row.care_level_name || '暂未设置' }}
                    </template>
                </el-table-column>
                <el-table-column
                    prop="family_name"
                    label="家属"
                />
                <el-table-column
                    prop="family_phone"
                    label="联系电话"
                />
                <el-table-column
                    prop="status_label"
                    label="状态"
                />
            </el-table>
        </div>
    </div>
</template>
