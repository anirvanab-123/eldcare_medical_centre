<template>
    <router-view />
</template>

<style>
.el-table th:last-child .cell,
.el-table td:last-child .cell {
    text-align: center;
}

.el-table td:last-child .cell {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 4px;
}

.el-table td:last-child .el-button {
    margin: 2px !important;
}
</style>
