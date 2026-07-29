const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  parallel: false,
  productionSourceMap: false,
  chainWebpack: config => {
    config.plugin('copy').tap(args => {
      if (args && args[0] && args[0].patterns) {
        args[0].patterns = args[0].patterns.map(pattern => {
          if (typeof pattern === 'string') {
            return { from: pattern, globOptions: { ignore: ['**/index.html'] } }
          }
          if (pattern.from && pattern.globOptions) {
            pattern.globOptions.ignore = [...(pattern.globOptions.ignore || []), '**/index.html']
          } else if (pattern.from) {
            pattern.globOptions = { ignore: ['**/index.html'] }
          }
          return pattern
        })
      }
      return args
    })
  },
  devServer: {
    port: 5173,
    client: {
      overlay: { errors: true, warnings: false, runtimeErrors: false }
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
