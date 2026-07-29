<script setup>
import { onMounted, ref } from 'vue'
import api from '../api'

const stats = ref({
    total: 0,
    free: 0,
    occupied: 0,
    outing: 0
})

onMounted(async () => {
    try {
        stats.value = await api.get('/beds/stats')
    } catch {}
})
</script>

<template>
    <div class="page dashboard">
        <div class="welcome-title">
            <div>
                <h2>工作台</h2>
                <p>欢迎回来，今天也要用心守护每一位老人。</p>
            </div>
            <span>☀ 今日服务概览</span>
        </div>
        <div class="stats">
            <div class="stat-card total">
                <i>▰</i>
                <div>
                    <b>{{ stats.total }}</b>
                    <span>床位总数</span>
                </div>
            </div>
            <div class="stat-card free">
                <i>✓</i>
                <div>
                    <b>{{ stats.free }}</b>
                    <span>空闲床位</span>
                </div>
            </div>
            <div class="stat-card occupied">
                <i>♙</i>
                <div>
                    <b>{{ stats.occupied }}</b>
                    <span>入住床位</span>
                </div>
            </div>
            <div class="stat-card outing">
                <i>➜</i>
                <div>
                    <b>{{ stats.outing }}</b>
                    <span>外出床位</span>
                </div>
            </div>
        </div>
        <div class="content-grid">
            <div class="card reminder">
                <span class="round">♢</span>
                <div>
                    <h3>照护提醒</h3>
                    <p>及时处理待审批的外出、退住申请，并关注即将到期、欠费或剩余次数不足的护理服务。</p>
                </div>
            </div>
            <div class="card slogan">
                <b>用专业与温度</b>
                <span>让照护服务更清晰、更及时、更安心</span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.welcome-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.welcome-title h2 {
    margin-bottom: 5px;
}

.welcome-title p {
    color: #7b8b98;
    margin: 0;
}

.welcome-title > span {
    padding: 9px 15px;
    border-radius: 20px;
    background: #fff4df;
    color: #d37a25;
    font-weight: 600;
}

.stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 18px;
    margin: 26px 0;
}

.stat-card {
    min-height: 128px;
    border-radius: 17px;
    color: #fff;
    padding: 22px;
    display: flex;
    align-items: center;
    gap: 18px;
    box-shadow: 0 10px 25px rgba(46, 78, 105, .18);
    transition: .2s;
}

.stat-card:hover {
    transform: translateY(-4px);
}

.stat-card i {
    width: 52px;
    height: 52px;
    border-radius: 15px;
    background: rgba(255, 255, 255, .2);
    display: grid;
    place-items: center;
    font-size: 25px;
    font-style: normal;
}

.stat-card div {
    display: flex;
    flex-direction: column;
}

.stat-card b {
    font-size: 34px;
}

.stat-card span {
    opacity: .92;
}

.total {
    background: linear-gradient(135deg, #675fd4, #8a78e8);
}

.free {
    background: linear-gradient(135deg, #17a970, #40c993);
}

.occupied {
    background: linear-gradient(135deg, #3476dc, #57a1ef);
}

.outing {
    background: linear-gradient(135deg, #e47b36, #f2a653);
}

.content-grid {
    display: grid;
    grid-template-columns: 1.4fr 1fr;
    gap: 18px;
}

.reminder {
    display: flex;
    align-items: center;
    gap: 18px;
    background: linear-gradient(110deg, #fff, #eef8f5);
}

.round {
    width: 58px;
    height: 58px;
    flex: none;
    border-radius: 50%;
    display: grid;
    place-items: center;
    background: #daf3e9;
    color: #17845f;
    font-size: 28px;
}

.slogan {
    display: flex;
    flex-direction: column;
    justify-content: center;
    background: linear-gradient(135deg, #243e55, #347969);
    color: #fff;
}

.slogan b {
    font-size: 23px;
}

.slogan span {
    margin-top: 10px;
    color: #d8ebe5;
}

@media (max-width: 950px) {
    .stats {
        grid-template-columns: 1fr 1fr;
    }

    .content-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 560px) {
    .stats {
        grid-template-columns: 1fr;
    }
}
</style>
