var webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const path = require('path');

module.exports = {
  mode: 'development',

  devtool: 'inline-source-map',

  entry: ['./src/Bucket4j.ts'],

  output: {
    path: path.resolve(__dirname, 'dist'),
    publicPath: '/',
    filename: 'dist/js/bucket4j.min.js',
  },

  resolve: {
    extensions: ['.ts', '.js'],
  },

  plugins: [
    new webpack.NoEmitOnErrorsPlugin(),
    new ExtractTextPlugin('dist/css/main.css'),
  ],

  module: {
    rules: [
      {
        test: /\.ts$/,
        loader: 'ts-loader',
        exclude: '/node_modules/',
      },
      {
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          fallback: 'style-loader',
          use: ['css-loader', 'sass-loader'],
        }),
      },
    ],
  },
};