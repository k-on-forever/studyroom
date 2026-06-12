'use strict'
const path = require('path')
const utils = require('./utils')
const webpack = require('webpack')
const config = require('../config')
const { merge } = require('webpack-merge')
const baseWebpackConfig = require('./webpack.base.conf')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const portfinder = require('portfinder')

const HOST = process.env.HOST
const PORT = process.env.PORT && Number(process.env.PORT)

function toDevServerProxy (proxyTable) {
  if (!proxyTable || typeof proxyTable !== 'object') {
    return []
  }
  return Object.keys(proxyTable).map(function (context) {
    return Object.assign({ context: context }, proxyTable[context])
  })
}

const devWebpackConfig = merge(baseWebpackConfig, {
  mode: 'development',
  module: {
    rules: utils.styleLoaders({ sourceMap: config.dev.cssSourceMap, usePostCSS: true })
  },
  devtool: config.dev.devtool,
  devServer: {
    static: {
      directory: path.join(__dirname, '..')
    },
    historyApiFallback: true,
    hot: true,
    compress: true,
    host: HOST || config.dev.host,
    port: PORT || config.dev.port,
    open: config.dev.autoOpenBrowser,
    client: {
      overlay: config.dev.errorOverlay
        ? { warnings: false, errors: true }
        : false
    },
    proxy: toDevServerProxy(config.dev.proxyTable),
    watchFiles: {
      options: {
        poll: config.dev.poll,
        ignored: [
          /node_modules/,
          /DumpStack\.log/i,
          /pagefile\.sys/i,
          /^D:\\DumpStack/i,
          /^[a-zA-Z]:\\DumpStack/i
        ]
      }
    }
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env': require('../config/dev.env')
    }),
    new HtmlWebpackPlugin({
      filename: 'index.html',
      template: 'index.html',
      inject: true
    })
  ]
})

module.exports = new Promise((resolve, reject) => {
  portfinder.basePort = process.env.PORT || config.dev.port
  portfinder.getPort((err, port) => {
    if (err) {
      reject(err)
    } else {
      process.env.PORT = port
      devWebpackConfig.devServer.port = port
      console.log(`\n  App running at: http://${devWebpackConfig.devServer.host}:${port}\n`)
      resolve(devWebpackConfig)
    }
  })
})
