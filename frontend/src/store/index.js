import { createStore } from 'vuex'

function readUser() {
  try {
    return JSON.parse(sessionStorage.getItem('user') || 'null')
  } catch {
    return null
  }
}

export default createStore({
  state: {
    token: sessionStorage.getItem('token') || '',
    user: readUser()
  },
  getters: {
    loggedIn: state => Boolean(state.token),
    role: state => state.user?.role || '',
    realName: state => state.user?.realName || ''
  },
  mutations: {
    setSession(state, { token, user }) {
      state.token = token
      state.user = user
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('user', JSON.stringify(user))
    },
    clearSession(state) {
      state.token = ''
      state.user = null
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
    }
  },
  actions: {
    saveSession({ commit }, session) {
      commit('setSession', session)
    },
    logout({ commit }) {
      commit('clearSession')
    }
  }
})
