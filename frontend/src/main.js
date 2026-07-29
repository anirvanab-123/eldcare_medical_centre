import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import store from './store'
import './style.css'

// 清理旧版本遗留的长期登录信息，之后仅在当前浏览器会话内保持登录。
localStorage.removeItem('token')
localStorage.removeItem('user')

createApp(App)
    .use(store)
    .use(router)
    .use(ElementPlus, { locale: zhCn })
    .mount('#app')
