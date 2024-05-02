var webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const path = require('path');

module.exports = {
  mode: 'development',

  devtool: '#source-map',

  entry: ['./src/Bucket4j.ts'],

  output: {
    path: path.resolve(__dirname, 'dist'),
    publicPath: '/',
    filename: 'dist/js/bundle.min.js',
  },

  resolve: {
    extensions: ['.ts', '.js', '.scss'],
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