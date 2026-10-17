import Vue from 'vue'
import App from './App.vue'
import axios from 'axios'

Vue.prototype.$axios = axios
Vue.config.productionTip = false

new Vue({
  render: h => h(App),

  // beforeCreate 中模板未解析，且 this 是 vm
  beforeCreate() {
    Vue.prototype.$bus = this // 安装全局事件总线
  }
}).$mount('#app')
